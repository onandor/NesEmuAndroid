package com.onandor.nesemu.viewmodels

import android.view.MotionEvent
import androidx.lifecycle.ViewModel
import com.onandor.nesemu.emulation.EmulationListener
import com.onandor.nesemu.emulation.Emulator
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.emulation.nes.DebugFeature
import com.onandor.nesemu.ui.components.NesRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class DebugScreenUiState(
    val renderPatternTable: Boolean = false,
    val renderNametable: Boolean = false,
    val renderColorPalettes: Boolean = false
)

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val navManager: NavigationManager,
    private val emulator: Emulator
) : ViewModel() {

    val patternTableRenderer: NesRenderer = NesRenderer(256, 128)
    private var requestPatternTableRender: () -> Unit = {}

    val nametableRenderer: NesRenderer = NesRenderer(512, 480)
    private var requestNametableRender: () -> Unit = {}

    val colorPaletteRenderers = Array(8) { NesRenderer(60, 15) }
    private var requestColorPaletteRender = Array(8) { {} }

    private val emulationListener = object : EmulationListener {

        override fun onFrameReady(
            frame: IntArray,
            patternTable: IntArray,
            nametable: IntArray,
            colorPalettes: Array<IntArray>
        ) {
            patternTableRenderer.setTextureData(patternTable)
            requestPatternTableRender()

            nametableRenderer.setTextureData(nametable)
            requestNametableRender()

            for (i in 0 ..< 8) {
                colorPaletteRenderers[i].setTextureData(colorPalettes[i])
                requestColorPaletteRender[i]()
            }
        }
    }

    private val _uiState = MutableStateFlow(DebugScreenUiState())
    val uiState = _uiState.asStateFlow()

    init {
        emulator.registerListener(emulationListener)
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
            emulator.nes.setDebugFeatureInt(DebugFeature.PPU_SET_COLOR_PALETTE, idx)
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
        emulator.nes.setDebugFeatureBool(feature, value)
    }

    fun navigateBack() {
        navManager.navigateBack()
    }

    override fun onCleared() {
        emulator.nes.setDebugFeatureInt(DebugFeature.PPU_SET_COLOR_PALETTE, 0)
        emulator.nes.setDebugFeatureBool(DebugFeature.PPU_RENDER_PATTERN_TABLE, false)
        emulator.nes.setDebugFeatureBool(DebugFeature.PPU_RENDER_NAMETABLE, false)
        emulator.nes.setDebugFeatureBool(DebugFeature.PPU_RENDER_COLOR_PALETTES, false)
        emulator.unregisterListener(emulationListener)
    }
}