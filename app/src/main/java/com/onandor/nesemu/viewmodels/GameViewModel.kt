package com.onandor.nesemu.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.onandor.nesemu.navigation.CartridgeNavArgs
import com.onandor.nesemu.navigation.NavActions
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.nes.Nes
import com.onandor.nesemu.nes.NesException
import com.onandor.nesemu.nes.NesListener
import com.onandor.nesemu.nes.RomParseException
import com.onandor.nesemu.ui.components.NesRenderer
import com.onandor.nesemu.ui.components.controls.Button
import com.onandor.nesemu.ui.components.controls.ButtonState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameUiState(
    val settingsOverlayVisible: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val navManager: NavigationManager,
    private val nes: Nes
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
    private var nesRunnerJob: Job? = null

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()

    private val nesListener = object : NesListener {
        override fun onFrameReady() {
            renderer.setTextureData(nes.ppu.frame)
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
        nes.registerListener(nesListener)

        val cartridge = (navManager.getCurrentNavAction()!!.navArgs as CartridgeNavArgs).cartridge
        try {
            nes.insertCartridge(cartridge)
            reset()
        } catch (e: RomParseException) {
            Log.e(e.tag, e.message.toString())
            _uiState.update { it.copy(errorMessage = e.message) }
            navManager.navigateBack()
        }
    }

    private fun reset() {
        nesRunnerJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                nes.reset()
            } catch (e: Exception) {
                if (e is NesException) {
                    Log.e(e.tag, e.message.toString())
                    _uiState.update { it.copy(errorMessage = e.message) }
                } else {
                    e.printStackTrace()
                }
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
        nes.unregisterListener(nesListener)
        nes.running = false // TODO: might not be enough, seems to keep running, need to investigate
        nesRunnerJob?.cancel()
    }
}