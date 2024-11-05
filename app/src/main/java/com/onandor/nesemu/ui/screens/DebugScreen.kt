package com.onandor.nesemu.ui.screens

import android.view.MotionEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.nesemu.nes.DebugFeature
import com.onandor.nesemu.ui.components.NesRenderer
import com.onandor.nesemu.viewmodels.DebugViewModel
import com.onandor.nesemu.ui.components.NesSurfaceView

@Composable
fun DebugScreen(
    viewModel: DebugViewModel = hiltViewModel()
) {
    Scaffold { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                ColorPalettes(
                    renderers = viewModel.colorPaletteRenderers,
                    setRenderCallback = viewModel::setColorPaletteRenderCallback,
                    onColorPaletteTouchEvent = viewModel::onColorPaletteTouchEvent
                )
                Button(
                    onClick = {
                        viewModel.setDebugFeatureBool(DebugFeature.PPU_RENDER_COLOR_PALETTES, true)
                    }
                ) {
                    Text("Enable color palette rendering")
                }
                Button(
                    onClick = {
                        viewModel.setDebugFeatureBool(DebugFeature.PPU_RENDER_COLOR_PALETTES, false)
                    }
                ) {
                    Text("Disable color palette rendering")
                }
            }

            item {
                NesSurfaceView(
                    modifier = Modifier.fillMaxWidth().aspectRatio(256f / 128f),
                    renderer = viewModel.patternTableRenderer,
                    setRenderCallback = viewModel::setPatternTableRenderCallback
                )
                Button(
                    onClick = {
                        viewModel.setDebugFeatureBool(DebugFeature.PPU_RENDER_PATTERN_TABLE, true)
                    }
                ) {
                    Text("Enable pattern table rendering")
                }
                Button(
                    onClick = {
                        viewModel.setDebugFeatureBool(DebugFeature.PPU_RENDER_PATTERN_TABLE, false)
                    }
                ) {
                    Text("Disable pattern table rendering")
                }
            }

            item {
                NesSurfaceView(
                    modifier = Modifier.fillMaxWidth().aspectRatio(512f / 480f),
                    renderer = viewModel.nametableRenderer,
                    setRenderCallback = viewModel::setNametableRenderCallback
                )
                Button(
                    onClick = {
                        viewModel.setDebugFeatureBool(DebugFeature.PPU_RENDER_NAMETABLE, true)
                    }
                ) {
                    Text("Enable nametable rendering")
                }
                Button(
                    onClick = {
                        viewModel.setDebugFeatureBool(DebugFeature.PPU_RENDER_NAMETABLE, false)
                    }
                ) {
                    Text("Disable nametable rendering")
                }
            }
        }
    }
}

@Composable
private fun ColorPalettes(
    renderers: Array<NesRenderer>,
    setRenderCallback: (Int, () -> Unit) -> Unit,
    onColorPaletteTouchEvent: (Int, MotionEvent) -> Unit
 ) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 0 ..< 4) {
                NesSurfaceView(
                    modifier = Modifier.width(screenWidth / 4.5f).aspectRatio(60f / 15f),
                    renderer = renderers[i],
                    setRenderCallback = { setRenderCallback(i, it) },
                    onTouchEvent = { onColorPaletteTouchEvent(i, it) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 4 ..< 8) {
                NesSurfaceView(
                    modifier = Modifier.width(screenWidth / 4.5f).aspectRatio(60f / 15f),
                    renderer = renderers[i],
                    setRenderCallback = { setRenderCallback(i, it) },
                    onTouchEvent = { onColorPaletteTouchEvent(i, it) }
                )
            }
        }
    }
}