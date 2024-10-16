package com.onandor.nesemu.ui.components

import android.content.Context
import android.opengl.GLSurfaceView

class NesSurfaceView(context: Context) : GLSurfaceView(context) {

    private val renderer: NesRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = NesRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }
}