package com.onandor.nesemu.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.onandor.nesemu.emulation.EmulationListener
import com.onandor.nesemu.emulation.Emulator
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.emulation.nes.NesException
import com.onandor.nesemu.ui.components.game.NesRenderer
import com.onandor.nesemu.input.NesButton
import com.onandor.nesemu.input.NesButtonState
import com.onandor.nesemu.input.NesInputManager
import com.onandor.nesemu.navigation.NavAction
import com.onandor.nesemu.navigation.NavDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val navManager: NavigationManager,
    private val emulator: Emulator,
    private val inputManager: NesInputManager
) : ViewModel() {

    data class UiState(
        val errorMessage: String? = null,
        val emulationPaused: Boolean = false,
        val showPauseMenu: Boolean = false
    )

    sealed class Event {
        data class OnRenderCallbackCreated(val requestRender: () -> Unit) : Event()
        data class OnNavigateTo(val action: NavAction) : Event()
        object OnNavigateBack: Event()
        data class OnButtonStateChanged(val button: NesButton, val state: NesButtonState) : Event()
        data class OnDpadStateChanged(val buttonStates: Map<NesButton, NesButtonState>) : Event()
        object OnErrorMessageToastShown : Event()

        object OnShowPauseMenuDialog : Event()
        object OnHidePauseMenuDialog : Event()
        object OnResetConsole : Event()
    }

    val buttonStateMap = mutableMapOf<NesButton, NesButtonState>(
        NesButton.DPAD_RIGHT to NesButtonState.UP,
        NesButton.DPAD_LEFT to NesButtonState.UP,
        NesButton.DPAD_DOWN to NesButtonState.UP,
        NesButton.DPAD_UP to NesButtonState.UP,
        NesButton.START to NesButtonState.UP,
        NesButton.SELECT to NesButtonState.UP,
        NesButton.B to NesButtonState.UP,
        NesButton.A to NesButtonState.UP
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
        emulator.registerListener(emulationListener)

        try {
            emulator.reset()
        } catch (e: Exception) {
            if (e is NesException) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } else {
                Log.e("GameViewModel", e.localizedMessage, e)
                _uiState.update { it.copy(errorMessage = "An unexpected error occurred") }
            }
        }

        emulator.startAudioStream()
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
                emulator.reset()
                _uiState.update { it.copy(emulationPaused = false) }
            }
        }
    }

    private fun hidePauseMenu() {
        _uiState.update { it.copy(showPauseMenu = false) }
    }

    private fun setEmulationPaused(paused: Boolean) {
        this._uiState.update { it.copy(emulationPaused = paused) }
        if (paused) {
            emulator.stop()
        } else {
            emulator.start()
        }
    }

    private fun collectInputManagerEvents(): Job = CoroutineScope(Dispatchers.IO).launch {
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

    private fun collectNavigationEvents(): Job = CoroutineScope(Dispatchers.IO).launch {
        navManager.navActions.collect { navAction ->
            if (navAction?.destination == NavDestinations.BACK
                && navManager.getCurrentRoute() == NavDestinations.GAME_SCREEN
                && emulator.nes.running == false) {
                setEmulationPaused(false)
            }
        }
    }

    override fun onCleared() {
        inputManagerEventJob?.cancel()
        navManagerJob?.cancel()
        emulator.pauseAudioStream()
        emulator.stop()
        emulator.unregisterListener(emulationListener)
    }
}