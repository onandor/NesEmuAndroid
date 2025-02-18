package com.onandor.nesemu.viewmodels

import android.view.MotionEvent
import androidx.lifecycle.ViewModel
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.nes.DebugFeature
import com.onandor.nesemu.nes.Nes
import com.onandor.nesemu.nes.NesListener
import com.onandor.nesemu.ui.components.NesRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class DebugScreenUiState(
    val showBottomSheet: Boolean = false,
    val renderPatternTable: Boolean = false,
    val renderNametable: Boolean = false,
    val renderColorPalettes: Boolean = false
)

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

    private val _uiState = MutableStateFlow(DebugScreenUiState())
    val uiState = _uiState.asStateFlow()

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

    fun onColorPaletteTouchEvent(idx: Int, event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN) {
            nes.setDebugFeatureInt(DebugFeature.PPU_SET_COLOR_PALETTE, idx)
        }
    }

    fun setDebugFeatureBool(feature: DebugFeature, value: Boolean) {
        when (feature) {
            DebugFeature.PPU_RENDER_PATTERN_TABLE -> {
                _uiState.update { it.copy(renderPatternTable = value) }
            }
            DebugFeature.PPU_RENDER_NAMETABLE -> {
                _uiState.update { it.copy(renderNametable = value) }
            }
            DebugFeature.PPU_RENDER_COLOR_PALETTES -> {
                _uiState.update { it.copy(renderColorPalettes = value) }
            }
            else -> {}
        }
        nes.setDebugFeatureBool(feature, value)
    }

    fun navigateBack() {
        navManager.navigateBack()
    }

    override fun onCleared() {
        nes.setDebugFeatureInt(DebugFeature.PPU_SET_COLOR_PALETTE, 0)
        nes.setDebugFeatureBool(DebugFeature.PPU_RENDER_PATTERN_TABLE, false)
        nes.setDebugFeatureBool(DebugFeature.PPU_RENDER_NAMETABLE, false)
        nes.setDebugFeatureBool(DebugFeature.PPU_RENDER_COLOR_PALETTES, false)
        nes.unregisterListener(nesListener)
    }
}