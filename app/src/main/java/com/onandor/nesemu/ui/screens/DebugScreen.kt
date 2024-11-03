package com.onandor.nesemu.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.nesemu.nes.DebugFeature
import com.onandor.nesemu.viewmodels.DebugViewModel
import com.onandor.nesemu.ui.components.NesSurfaceView

@Composable
fun DebugScreen(
    viewModel: DebugViewModel = hiltViewModel()
) {
    Scaffold { padding ->
        Column {
            NesSurfaceView(
                modifier = Modifier.padding(padding).fillMaxWidth().aspectRatio(256F / 128F),
                renderer = viewModel.patternTableRenderer,
                setRenderCallback = viewModel::setPatternTableRenderCallback
            )
            Button(
                onClick = { viewModel.enableDebugFeature(DebugFeature.PPU_RENDER_PATTERN_TABLE) }
            ) {
                Text("Enable pattern table rendering")
            }
            Button(
                onClick = { viewModel.disableDebugFeature(DebugFeature.PPU_RENDER_PATTERN_TABLE) }
            ) {
                Text("Disable pattern table rendering")
            }
        }
    }
}