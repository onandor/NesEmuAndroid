package com.onandor.nesemu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun NesSurfaceView(
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