package com.onandor.nesemu.viewmodels

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.nesemu.data.repository.SaveStateRepository
import com.onandor.nesemu.di.IODispatcher
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.domain.emulation.nes.NesException
import com.onandor.nesemu.ui.components.game.NesRenderer
import com.onandor.nesemu.domain.input.NesButton
import com.onandor.nesemu.domain.input.NesButtonState
import com.onandor.nesemu.domain.service.InputService
import com.onandor.nesemu.navigation.NavAction
import com.onandor.nesemu.navigation.NavDestinations
import com.onandor.nesemu.domain.service.EmulationService
import com.onandor.nesemu.domain.service.EmulationState
import com.onandor.nesemu.ui.components.SaveStateSheetType
import com.onandor.nesemu.ui.model.UiSaveState
import com.onandor.nesemu.ui.model.toUiSaveState
import com.onandor.nesemu.util.GlobalLifecycleObserver
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
    @IODispatcher private val ioScope: CoroutineScope,
    private val navManager: NavigationManager,
    private val emulationService: EmulationService,
    private val inputService: InputService,
    private val saveStateRepository: SaveStateRepository,
    private val lifecycleObserver: GlobalLifecycleObserver
) : ViewModel() {

    data class UiState(
        val toastMessage: String? = null,
        val emulationPaused: Boolean = false,
        val showPauseMenu: Boolean = false,

        val saveStateSheetType: SaveStateSheetType? = null,
        val saveStates: List<UiSaveState> = emptyList(),
        val saveStateToOverwrite: UiSaveState? = null
    )

    sealed class Event {
        data class OnRenderCallbackCreated(val requestRender: () -> Unit) : Event()
        data class OnButtonStateChanged(val button: NesButton, val state: NesButtonState) : Event()
        data class OnDpadStateChanged(val buttonStates: Map<NesButton, NesButtonState>) : Event()
        data object OnToastShown : Event()

        // Pause menu
        data class OnNavigateTo(val action: NavAction) : Event()
        data object OnNavigateBack: Event()
        data object OnShowPauseMenuDialog : Event()
        data object OnHidePauseMenuDialog : Event()
        data object OnResetConsole : Event()

        data class OnShowSaveStateSheet(val type: SaveStateSheetType) : Event()
        data object OnHideSaveStateSheet : Event()
        data class OnSelectSaveState(val slot: Int, val saveState: UiSaveState?) : Event()
        data class OnOverwriteSaveState(val confirmed: Boolean) : Event()
    }

    private val buttonStateMap = mutableMapOf<NesButton, NesButtonState>(
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

    init {
        collectRenderedFrames()
        collectLifecycleEvents()
        collectInputManagerEvents()
        collectNavigationEvents()
        emulationService.renderer = renderer

        try {
            emulationService.start()
        } catch (e: NesException) {
            _uiState.update { it.copy(toastMessage = e.message) }
        } catch (e: Exception) {
            Log.e("GameViewModel", e.localizedMessage, e)
            _uiState.update { it.copy(toastMessage = "An unexpected error occurred") }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.OnButtonStateChanged -> {
                buttonStateMap[event.button] = event.state
                inputService.onInputEvent(
                    InputService.VIRTUAL_CONTROLLER_DEVICE_ID, event.button, event.state)
            }
            is Event.OnDpadStateChanged -> {
                buttonStateMap.putAll(event.buttonStates)
                inputService.onInputEvents(
                    InputService.VIRTUAL_CONTROLLER_DEVICE_ID, event.buttonStates)
            }
            is Event.OnToastShown -> {
                _uiState.update { it.copy(toastMessage = null) }
            }
            is Event.OnRenderCallbackCreated -> {
                this.requestRender = event.requestRender
            }

            // Pause menu events
            is Event.OnNavigateTo -> {
                _uiState.update { it.copy(showPauseMenu = false) }
                emulationService.pause()
                navManager.navigateTo(event.action)
            }
            is Event.OnNavigateBack -> {
                _uiState.update { it.copy(showPauseMenu = false) }
                cleanUp()
                navManager.navigateBack()
            }
            is Event.OnShowPauseMenuDialog -> {
                _uiState.update { it.copy(showPauseMenu = true) }
                emulationService.pause()
            }
            is Event.OnHidePauseMenuDialog -> {
                unpause()
            }
            is Event.OnResetConsole -> {
                emulationService.reset()
                unpause()
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
                        emulationService.saveGame(event.slot)
                        unpause()
                    }
                } else if (event.saveState != null) {
                    emulationService.loadSave(event.saveState.entity)
                    unpause()
                }
            }
            is Event.OnOverwriteSaveState -> {
                if (event.confirmed) {
                    val saveState = _uiState.value.saveStateToOverwrite!!
                    emulationService.saveGame(saveState.entity.slot)
                    unpause()
                }
                _uiState.update { it.copy(saveStateToOverwrite = null) }
            }
        }
    }

    private fun unpause() {
        _uiState.update { it.copy(showPauseMenu = false, emulationPaused = false) }
        emulationService.start()
    }

    private fun hideSaveStateSheet() {
        _uiState.update { it.copy(saveStateSheetType = null, saveStates = emptyList()) }
    }

    private fun showSaveStateSheet(type: SaveStateSheetType) = ioScope.launch {
        var saveStates = saveStateRepository
            .findByRomHash(emulationService.loadedGame?.romHash ?: "")
            .map { it.toUiSaveState() }

        if (type == SaveStateSheetType.Save) {
            saveStates = saveStates.filterNot { it.entity.slot == 0 }
        }

        _uiState.update {
            it.copy(
                saveStateSheetType = type,
                saveStates = saveStates
            )
        }
    }

    private fun collectRenderedFrames(): Job = viewModelScope.launch {
        emulationService.frames.collect { frame ->
            renderer.setTextureData(frame.frame)
            requestRender()
        }
    }

    private fun collectInputManagerEvents(): Job = viewModelScope.launch {
        inputService.events.collect { event ->
            when (event) {
                is InputService.Event.OnPauseButtonPressed -> {
                    if (navManager.getCurrentRoute() != NavDestinations.GAME_SCREEN) {
                        return@collect
                    }
                    if (!_uiState.value.showPauseMenu) {
                        onEvent(Event.OnShowPauseMenuDialog)
                    } else {
                        onEvent(Event.OnHidePauseMenuDialog)
                    }
                }
                is InputService.Event.OnInputDeviceDisconnected -> {
                    onEvent(Event.OnShowPauseMenuDialog)
                    val toastMessage = "Input device for player ${event.playerId} got disconnected"
                    _uiState.update { it.copy(toastMessage = toastMessage) }
                }
            }
        }
    }

    private fun collectNavigationEvents(): Job = viewModelScope.launch {
        navManager.navActions.collect { navAction ->
            if (navAction?.destination == NavDestinations.BACK
                && navManager.getCurrentRoute() == NavDestinations.GAME_SCREEN
                && emulationService.state == EmulationState.Paused
            ) {
                emulationService.start()
            }
        }
    }

    private fun collectLifecycleEvents() = viewModelScope.launch {
        lifecycleObserver.events.collect { event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (!_uiState.value.showPauseMenu) {
                        emulationService.start()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    emulationService.stop(immediate = true)
                }
                else -> {}
            }
        }
    }

    private fun cleanUp() {
        emulationService.stop(immediate = true)
        emulationService.renderer = null
    }

    override fun onCleared() {
        cleanUp()
    }
}