package com.onandor.nesemu.ui.components.game

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class NesSurfaceView(
    context: Context,
    renderer: NesRenderer,
    private val _onTouchEvent: (MotionEvent) -> Unit = {}
): GLSurfaceView(context) {

    init {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (_onTouchEvent !== {}) {
            _onTouchEvent(event)
            return true
        }
        return false
    }
}