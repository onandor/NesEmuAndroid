package com.onandor.nesemu.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.nesemu.data.entity.SaveState
import com.onandor.nesemu.data.repository.SaveStateRepository
import com.onandor.nesemu.di.IODispatcher
import com.onandor.nesemu.emulation.EmulationListener
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.emulation.nes.NesException
import com.onandor.nesemu.ui.components.game.NesRenderer
import com.onandor.nesemu.input.NesButton
import com.onandor.nesemu.input.NesButtonState
import com.onandor.nesemu.input.NesInputManager
import com.onandor.nesemu.navigation.NavAction
import com.onandor.nesemu.navigation.NavDestinations
import com.onandor.nesemu.service.EmulationService
import com.onandor.nesemu.service.EmulationState
import com.onandor.nesemu.ui.components.SaveStateSheetType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    @IODispatcher private val coroutineScope: CoroutineScope,
    private val navManager: NavigationManager,
    private val emulationService: EmulationService,
    private val inputManager: NesInputManager,
    private val saveStateRepository: SaveStateRepository
) : ViewModel() {

    data class UiState(
        val errorMessage: String? = null,
        val emulationPaused: Boolean = false,
        val showPauseMenu: Boolean = false,

        val saveStateSheetType: SaveStateSheetType? = null,
        val saveStates: List<SaveState> = emptyList(),
        val saveStateToOverwrite: SaveState? = null
    )

    sealed class Event {
        data class OnRenderCallbackCreated(val requestRender: () -> Unit) : Event()
        data class OnButtonStateChanged(val button: NesButton, val state: NesButtonState) : Event()
        data class OnDpadStateChanged(val buttonStates: Map<NesButton, NesButtonState>) : Event()
        object OnErrorMessageToastShown : Event()

        // Pause menu
        data class OnNavigateTo(val action: NavAction) : Event()
        object OnNavigateBack: Event()
        object OnShowPauseMenuDialog : Event()
        object OnHidePauseMenuDialog : Event()
        object OnResetConsole : Event()

        data class OnShowSaveStateSheet(val type: SaveStateSheetType) : Event()
        object OnHideSaveStateSheet : Event()
        data class OnSelectSaveState(val slot: Int, val saveState: SaveState?) : Event()
        data class OnOverwriteSaveState(val confirmed: Boolean) : Event()
    }

    val buttonStateMap = mutableMapOf<NesButton, NesButtonState>(
        NesButton.DPadRight to NesButtonState.Up,
        NesButton.DPadLeft to NesButtonState.Up,
        NesButton.DPadDown to NesButtonState.Up,
        NesButton.DPadUp to NesButtonState.Up,
        NesButton.Start to NesButtonState.Up,
        NesButton.Select to NesButtonState.Up,
        NesButton.B to NesButtonState.Up,
        NesButton.A to NesButtonState.Up
    )

    val renderer: NesRenderer = NesRenderer(256, 240)
    private var requestRender: () -> Unit = {}

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private var inputManagerEventJob: Job? = null
    private var navManagerJob: Job? = null

    private val emulationListener = object : EmulationListener {

        override fun onFrameReady(
            frame: IntArray,
            patternTable: IntArray,
            nametable: IntArray,
            colorPalettes: Array<IntArray>
        ) {
            renderer.setTextureData(frame)
            requestRender()
        }
    }

    init {
        inputManagerEventJob = collectInputManagerEvents()
        navManagerJob = collectNavigationEvents()
        emulationService.registerListener(emulationListener)
        emulationService.renderer = renderer

        try {
            emulationService.start()
        } catch (e: NesException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        } catch (e: Exception) {
            Log.e("GameViewModel", e.localizedMessage, e)
            _uiState.update { it.copy(errorMessage = "An unexpected error occurred") }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.OnButtonStateChanged -> {
                buttonStateMap[event.button] = event.state
                inputManager.onInputEvent(
                    NesInputManager.VIRTUAL_CONTROLLER_DEVICE_ID, event.button, event.state)
            }
            is Event.OnDpadStateChanged -> {
                buttonStateMap.putAll(event.buttonStates)
                inputManager.onInputEvents(
                    NesInputManager.VIRTUAL_CONTROLLER_DEVICE_ID, event.buttonStates)
            }
            is Event.OnErrorMessageToastShown -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
            is Event.OnRenderCallbackCreated -> {
                this.requestRender = event.requestRender
            }

            // Pause menu events
            is Event.OnNavigateTo -> {
                hidePauseMenu()
                setEmulationPaused(true)
                navManager.navigateTo(event.action)
            }
            is Event.OnNavigateBack -> {
                hidePauseMenu()
                cleanUp()
                navManager.navigateBack()
            }
            is Event.OnShowPauseMenuDialog -> {
                _uiState.update { it.copy(showPauseMenu = true) }
                setEmulationPaused(true)
            }
            is Event.OnHidePauseMenuDialog -> {
                hidePauseMenu()
                setEmulationPaused(false)
            }
            is Event.OnResetConsole -> {
                hidePauseMenu()
                emulationService.reset()
                _uiState.update { it.copy(emulationPaused = false) }
            }

            // Save state management
            is Event.OnShowSaveStateSheet -> {
                showSaveStateSheet(event.type)
            }
            Event.OnHideSaveStateSheet -> {
                hideSaveStateSheet()
            }
            is Event.OnSelectSaveState -> {
                val sheetType = _uiState.value.saveStateSheetType
                hideSaveStateSheet()
                if (sheetType == SaveStateSheetType.Save) {
                    if (event.saveState != null) {
                        _uiState.update { it.copy(saveStateToOverwrite = event.saveState) }
                    } else {
                        saveGame(event.slot)
                        hidePauseMenu()
                        setEmulationPaused(false)
                    }
                } else if (event.saveState != null) {
                    setEmulationPaused(true)
                    emulationService.loadSave(event.saveState)
                    hidePauseMenu()
                    setEmulationPaused(false)
                }
            }
            is Event.OnOverwriteSaveState -> {
                if (event.confirmed) {
                    val saveState = _uiState.value.saveStateToOverwrite!!
                    saveGame(saveState.slot)
                    hidePauseMenu()
                    setEmulationPaused(false)
                }
                _uiState.update { it.copy(saveStateToOverwrite = null) }
            }
        }
    }

    private fun hidePauseMenu() {
        _uiState.update { it.copy(showPauseMenu = false) }
    }

    private fun hideSaveStateSheet() {
        _uiState.update { it.copy(saveStateSheetType = null, saveStates = emptyList()) }
    }

    private fun setEmulationPaused(paused: Boolean) {
        if (_uiState.value.emulationPaused == paused) {
            return
        }

        this._uiState.update { it.copy(emulationPaused = paused) }
        if (paused) {
            emulationService.pause()
        } else {
            emulationService.start()
        }
    }

    private fun showSaveStateSheet(type: SaveStateSheetType) = coroutineScope.launch {
        var saveStates =
            saveStateRepository.findByRomHash(emulationService.loadedGame?.romHash ?: "")

        if (type == SaveStateSheetType.Save) {
            saveStates = saveStates.filterNot { it.slot == 0 }
        }

        _uiState.update {
            it.copy(
                saveStateSheetType = type,
                saveStates = saveStates
            )
        }
    }

    private fun saveGame(slot: Int) {
        emulationService.pause()
        emulationService.saveGame(slot)
        emulationService.start()
    }

    private fun collectInputManagerEvents(): Job = viewModelScope.launch {
        inputManager.events.collect { event ->
            when (event) {
                is NesInputManager.Event.OnPauseButtonPressed -> {
                    if (navManager.getCurrentRoute() != NavDestinations.GAME_SCREEN) {
                        return@collect
                    }
                    if (!_uiState.value.showPauseMenu) {
                        onEvent(Event.OnShowPauseMenuDialog)
                    } else {
                        onEvent(Event.OnHidePauseMenuDialog)
                    }
                }
            }
        }
    }

    private fun collectNavigationEvents(): Job = viewModelScope.launch {
        navManager.navActions.collect { navAction ->
            if (navAction?.destination == NavDestinations.BACK
                && navManager.getCurrentRoute() == NavDestinations.GAME_SCREEN
                && emulationService.state == EmulationState.Running
            ) {
                setEmulationPaused(false)
            }
        }
    }

    private fun cleanUp() {
        inputManagerEventJob?.cancel()
        navManagerJob?.cancel()
        emulationService.stop()
        emulationService.unregisterListener(emulationListener)
        emulationService.renderer = null
    }
}