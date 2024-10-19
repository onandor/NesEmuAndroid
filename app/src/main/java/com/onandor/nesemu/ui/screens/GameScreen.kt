package com.onandor.nesemu.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.nesemu.ui.components.NesRenderer
import com.onandor.nesemu.ui.components.NesSurfaceView
import com.onandor.nesemu.viewmodels.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel()
) {
    Scaffold { padding ->
        NesSurfaceView(
            modifier = Modifier.padding(padding),
            renderer = viewModel.renderer,
            setRenderCallback = viewModel::setRenderCallback
        )
    }
}

@Composable
private fun NesSurfaceView(
    modifier: Modifier,
    renderer: NesRenderer,
    setRenderCallback: (() -> Unit) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = {
            NesSurfaceView(it, renderer).apply { setRenderCallback(this::requestRender) }
        }
    )
}