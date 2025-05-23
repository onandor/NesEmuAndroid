package com.onandor.nesemu.viewmodels

import android.view.MotionEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.domain.emulation.nes.DebugFeature
import com.onandor.nesemu.domain.service.EmulationService
import com.onandor.nesemu.ui.components.game.NesRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val navManager: NavigationManager,
    private val emulationService: EmulationService
) : ViewModel() {

    data class UiState(
        val renderPatternTable: Boolean = true,
        val renderNametable: Boolean = true,
        val renderColorPalettes: Boolean = true,
        val showBottomSheet: Boolean = false,
        val emulationPaused: Boolean = true
    )

    sealed class Event {
        data class OnPatternTableRenderCallbackCreated(val requestRender: () -> Unit) : Event()
        data class OnNametableRenderCallbackCreated(val requestRender: () -> Unit) : Event()
        data class OnColorPaletteRenderCallbackCreated(
            val index: Int,
            val requestRender: () -> Unit
        ) : Event()
        data class OnColorPaletteTouch(val index: Int, val motionEvent: MotionEvent) : Event()
        data class OnSetDebugFeatureBool(val feature: DebugFeature, val value: Boolean) : Event()
        data object OnNavigateBack : Event()
        data object OnShowBottomSheet : Event()
        data object OnHideBottomSheet : Event()
        data object OnToggleEmulationPaused : Event()
    }

    val patternTableRenderer: NesRenderer = NesRenderer(256, 128)
    private var requestPatternTableRender: () -> Unit = {}

    val nametableRenderer: NesRenderer = NesRenderer(512, 480)
    private var requestNametableRender: () -> Unit = {}

    val colorPaletteRenderers = Array(8) { NesRenderer(60, 15) }
    private var requestColorPaletteRender = Array(8) { {} }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        collectRenderedFrames()
        emulationService.setDebugFeatureBool(DebugFeature.PpuRenderPatternTable, true)
        emulationService.setDebugFeatureBool(DebugFeature.PpuRenderNametable, true)
        emulationService.setDebugFeatureBool(DebugFeature.PpuRenderColorPalettes, true)
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.OnPatternTableRenderCallbackCreated -> {
                this.requestPatternTableRender = event.requestRender
            }
            is Event.OnNametableRenderCallbackCreated -> {
                this.requestNametableRender = event.requestRender
            }
            is Event.OnColorPaletteRenderCallbackCreated -> {
                this.requestColorPaletteRender[event.index] = event.requestRender
            }
            is Event.OnColorPaletteTouch -> {
                if (event.motionEvent.action == MotionEvent.ACTION_DOWN) {
                    emulationService.setDebugFeatureInt(DebugFeature.PpuSetColorPalette, event.index)
                }
            }
            is Event.OnSetDebugFeatureBool -> {
                when (event.feature) {
                    DebugFeature.PpuRenderPatternTable -> {
                        _uiState.update { it.copy(renderPatternTable = event.value) }
                    }
                    DebugFeature.PpuRenderNametable -> {
                        _uiState.update { it.copy(renderNametable = event.value) }
                    }
                    DebugFeature.PpuRenderColorPalettes -> {
                        _uiState.update { it.copy(renderColorPalettes = event.value) }
                    }
                    else -> {}
                }
                emulationService.setDebugFeatureBool(event.feature, event.value)
            }
            is Event.OnNavigateBack -> {
                navManager.navigateBack()
            }
            Event.OnShowBottomSheet -> {
                _uiState.update { it.copy(showBottomSheet = true) }
            }
            Event.OnHideBottomSheet -> {
                _uiState.update { it.copy(showBottomSheet = false) }
            }
            Event.OnToggleEmulationPaused -> {
                val newPaused = !_uiState.value.emulationPaused
                if (newPaused) {
                    emulationService.pause()
                } else {
                    emulationService.start()
                }
                _uiState.update { it.copy(emulationPaused = newPaused) }
            }
        }
    }

    private fun collectRenderedFrames(): Job = viewModelScope.launch {
        emulationService.frames.collect { frame ->
            patternTableRenderer.setTextureData(frame.patternTable)
            requestPatternTableRender()

            nametableRenderer.setTextureData(frame.nametable)
            requestNametableRender()

            for (i in 0 ..< 8) {
                colorPaletteRenderers[i].setTextureData(frame.colorPalettes[i])
                requestColorPaletteRender[i]()
            }
        }
    }

    override fun onCleared() {
        emulationService.setDebugFeatureInt(DebugFeature.PpuSetColorPalette, 0)
        emulationService.setDebugFeatureBool(DebugFeature.PpuRenderPatternTable, false)
        emulationService.setDebugFeatureBool(DebugFeature.PpuRenderNametable, false)
        emulationService.setDebugFeatureBool(DebugFeature.PpuRenderColorPalettes, false)
    }
}