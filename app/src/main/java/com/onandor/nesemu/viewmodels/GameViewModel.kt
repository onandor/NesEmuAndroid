package com.onandor.nesemu.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.onandor.nesemu.emulation.Emulator
import com.onandor.nesemu.navigation.NavActions
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.emulation.nes.NesException
import com.onandor.nesemu.emulation.nes.NesListener
import com.onandor.nesemu.ui.components.NesRenderer
import com.onandor.nesemu.ui.components.controls.Button
import com.onandor.nesemu.ui.components.controls.ButtonState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class GameUiState(
    val settingsOverlayVisible: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val navManager: NavigationManager,
    private val emulator: Emulator
) : ViewModel() {

    val buttonStateMap = mutableMapOf<Button, ButtonState>(
        Button.DPAD_RIGHT to ButtonState.UP,
        Button.DPAD_LEFT to ButtonState.UP,
        Button.DPAD_DOWN to ButtonState.UP,
        Button.DPAD_UP to ButtonState.UP,
        Button.START to ButtonState.UP,
        Button.SELECT to ButtonState.UP,
        Button.B to ButtonState.UP,
        Button.A to ButtonState.UP
    )

    val renderer: NesRenderer = NesRenderer(256, 240)
    private var requestRender: () -> Unit = {}

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()

    private val nesListener = object : NesListener {
        override fun onFrameReady() {
            renderer.setTextureData(emulator.nes.ppu.frame)
            requestRender()
        }

        override fun onPollController1Buttons(): Int? {
            return mapButtonStatesToInt()
        }

        override fun onPollController2Buttons(): Int? {
            return 0 // TODO: second controller
        }
    }

    init {
        emulator.nes.registerListener(nesListener)
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

    fun showSettingsOverlay() {
        _uiState.update { it.copy(settingsOverlayVisible = true) }
    }

    fun quit() {
        navManager.navigateBack()
    }

    fun navigateToDebugScreen() {
        navManager.navigateTo(NavActions.debugScreen())
    }

    fun buttonStateChanged(button: Button, state: ButtonState) {
        buttonStateMap[button] = state
    }

    fun dpadStateChanged(state: Map<Button, ButtonState>) {
        buttonStateMap.putAll(state)
    }

    fun errorMessageToastShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun mapButtonStatesToInt(): Int {
        var buttonStates = 0
        buttonStateMap.forEach { _, state ->
            buttonStates = (buttonStates shl 1) or state.ordinal
        }
        return buttonStates
    }

    fun navigateBack() {
        navManager.navigateBack()
    }

    override fun onCleared() {
        emulator.pauseAudioStream()
        emulator.stop()
        emulator.nes.unregisterListener(nesListener)
    }
}