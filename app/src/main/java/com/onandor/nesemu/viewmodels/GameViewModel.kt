package com.onandor.nesemu.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.nesemu.navigation.CartridgeNavArgs
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.nes.Nes
import com.onandor.nesemu.nes.NesException
import com.onandor.nesemu.ui.components.NesRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    navManager: NavigationManager
) : ViewModel() {

    private val nes: Nes = Nes(::onFrameReady)
    val renderer: NesRenderer = NesRenderer()
    private var requestRender: () -> Unit = { println("requestRender: not yet set") }
    private val nesRunnerJob: Job

    init {
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

    private fun onFrameReady(frameData: IntArray) {
        renderer.setTextureData(frameData)
        requestRender()
    }

    fun setRenderCallback(requestRender: () -> Unit) {
        this.requestRender = requestRender
    }

    override fun onCleared() {
        nes.running = false // TODO: might not be enough, seems to keep running, need to investigate
        nesRunnerJob.cancel()
        //renderer.onDestroy() // TODO: OpenGL context is lost by this time, handle it sooner
    }
}