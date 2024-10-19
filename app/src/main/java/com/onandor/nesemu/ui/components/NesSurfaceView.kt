package com.onandor.nesemu.ui.components

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class NesSurfaceView(context: Context, renderer: NesRenderer) : GLSurfaceView(context) {

    init {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        println("onTouchEvent")
        return true
    }
}