package com.onandor.nesemu.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.onandor.nesemu.navigation.CartridgeNavArgs
import com.onandor.nesemu.navigation.NavActions
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.nes.DebugFeature
import com.onandor.nesemu.nes.Nes
import com.onandor.nesemu.nes.NesException
import com.onandor.nesemu.nes.NesListener
import com.onandor.nesemu.ui.components.NesRenderer
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
    val settingsOverlayVisible: Boolean = false
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val navManager: NavigationManager,
    private val nes: Nes
) : ViewModel() {

    val renderer: NesRenderer = NesRenderer(256, 240)
    private var requestRender: () -> Unit = {}
    private val nesRunnerJob: Job

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()

    private val nesListener = object : NesListener {
        override fun onFrameReady() {
            renderer.setTextureData(nes.ppu.frame)
            requestRender()
        }
    }

    init {
        nes.registerListener(nesListener)

        val cartridge = (navManager.getCurrentNavAction()!!.navArgs as CartridgeNavArgs).cartridge
        nes.insertCartridge(cartridge)

        nesRunnerJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                nes.reset()
            } catch (e: Exception) {
                if (e is NesException) {
                    Log.e(e.tag, e.message.toString())
                    // TODO: display some kind of error message
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

    fun enableDebugFeature(feature: DebugFeature) {
        nes.enableDebugFeature(feature)
    }

    fun disableDebugFeature(feature: DebugFeature) {
        nes.disableDebugFeature(feature)
    }

    override fun onCleared() {
        nes.unregisterListener(nesListener)
        nes.running = false // TODO: might not be enough, seems to keep running, need to investigate
        nesRunnerJob.cancel()
    }
}