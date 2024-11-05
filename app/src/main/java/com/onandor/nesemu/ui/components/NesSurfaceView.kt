package com.onandor.nesemu.ui.components

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class NesSurfaceView(
    context: Context,
    renderer: NesRenderer,
    private val touchEventCallback: (MotionEvent) -> Unit = {}
): GLSurfaceView(context) {

    init {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (touchEventCallback !== {}) {
            this.touchEventCallback(event)
            return true
        }
        return false
    }
}