package com.onandor.nesemu.ui.components

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class NesRenderer : GLSurfaceView.Renderer {

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        // Set background color
        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        // Resize viewport
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(unused: GL10?) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }
}