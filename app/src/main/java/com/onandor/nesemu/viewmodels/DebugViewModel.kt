package com.onandor.nesemu.viewmodels

import androidx.lifecycle.ViewModel
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.nes.DebugFeature
import com.onandor.nesemu.nes.Nes
import com.onandor.nesemu.nes.NesListener
import com.onandor.nesemu.ui.components.NesRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val navManager: NavigationManager,
    private val nes: Nes
) : ViewModel() {

    val patternTableRenderer: NesRenderer = NesRenderer(256, 128)
    private var requestPatternTableRender: () -> Unit = {}

    val nametableRenderer: NesRenderer = NesRenderer(512, 480)
    private var requestNametableRender: () -> Unit = {}

    val colorPaletteRenderers = Array(8) { NesRenderer(60, 15) }
    private var requestColorPaletteRender = Array(8) { {} }

    private val nesListener = object : NesListener {
        override fun onFrameReady() {
            patternTableRenderer.setTextureData(nes.ppu.dbgPatternTableFrame)
            requestPatternTableRender()

            nametableRenderer.setTextureData(nes.ppu.dbgNametableFrame)
            requestNametableRender()

            for (i in 0 ..< 8) {
                colorPaletteRenderers[i].setTextureData(nes.ppu.dbgColorPalettes[i])
                requestColorPaletteRender[i]()
            }
        }
    }

    init {
        nes.registerListener(nesListener)
    }

    fun setPatternTableRenderCallback(requestRender: () -> Unit) {
        this.requestPatternTableRender = requestRender
    }

    fun setNametableRenderCallback(requestRender: () -> Unit) {
        this.requestNametableRender = requestRender
    }

    fun setColorPaletteRenderCallback(idx: Int, requestRender: () -> Unit) {
        this.requestColorPaletteRender[idx] = requestRender
    }

    fun enableDebugFeature(feature: DebugFeature) {
        nes.enableDebugFeature(feature)
    }

    fun disableDebugFeature(feature: DebugFeature) {
        nes.disableDebugFeature(feature)
    }

    fun navigateBack() {
        navManager.navigateBack()
    }

    override fun onCleared() {
        nes.unregisterListener(nesListener)
    }
}