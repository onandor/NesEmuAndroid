package com.onandor.nesemu.ui.components.game

import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun NesSurfaceView(
    modifier: Modifier,
    renderer: NesRenderer,
    onRenderCallbackCreated: (() -> Unit) -> Unit,
    onTouchEvent: (MotionEvent) -> Unit = {}
) {
    AndroidView(
        modifier = modifier,
        factory = {
            NesSurfaceView(it, renderer, onTouchEvent).apply {
                onRenderCallbackCreated(this::requestRender)
            }
        }
    )
}