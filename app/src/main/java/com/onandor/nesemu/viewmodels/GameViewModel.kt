package com.onandor.nesemu.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.onandor.nesemu.emulation.EmulationListener
import com.onandor.nesemu.emulation.Emulator
import com.onandor.nesemu.navigation.NavActions
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.emulation.nes.NesException
import com.onandor.nesemu.ui.components.NesRenderer
import com.onandor.nesemu.input.NesButton
import com.onandor.nesemu.input.NesButtonState
import com.onandor.nesemu.input.NesInputManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class GameUiState(
    val settingsOverlayVisible: Boolean = false,
    val errorMessage: String? = null,
    val emulationPaused: Boolean = false
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val navManager: NavigationManager,
    private val emulator: Emulator,
    private val inputManager: NesInputManager
) : ViewModel() {

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

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()

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
        emulator.registerListener(emulationListener)
        emulator.startAudioStream()

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
    }

    fun setRenderCallback(requestRender: () -> Unit) {
        this.requestRender = requestRender
    }

    fun navigateToPreferencesScreen() {
        _uiState.update { it.copy(emulationPaused = true) }
        emulator.stop()
        navManager.navigateTo(NavActions.preferencesScreen())
    }

    fun quit() {
        navManager.navigateBack()
    }

    fun navigateToDebugScreen() {
        navManager.navigateTo(NavActions.debugScreen())
    }

    fun buttonStateChanged(button: NesButton, state: NesButtonState) {
        buttonStateMap[button] = state
        inputManager.onInputEvent(NesInputManager.VIRTUAL_CONTROLLER_DEVICE_ID, button, state)
    }

    fun dpadStateChanged(buttonStates: Map<NesButton, NesButtonState>) {
        buttonStateMap.putAll(buttonStates)
        inputManager.onInputEvents(NesInputManager.VIRTUAL_CONTROLLER_DEVICE_ID, buttonStates)
    }

    fun errorMessageToastShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun setEmulationState(paused: Boolean) {
        this._uiState.update { it.copy(emulationPaused = paused) }
        if (paused) {
            emulator.stop()
        } else {
            emulator.start()
        }
    }

    fun navigateBack() {
        navManager.navigateBack()
    }

    override fun onCleared() {
        emulator.pauseAudioStream()
        emulator.stop()
        emulator.unregisterListener(emulationListener)
    }
}