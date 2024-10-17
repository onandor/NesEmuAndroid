package com.onandor.nesemu.ui.components

import android.opengl.GLES30.*
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class NesRenderer : GLSurfaceView.Renderer {

    private var mTexture: Int = 0
    private var mShaderProgram: Int = 0
    private var mVao: Int = 0
    private var mVbo: Int = 0

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        createShaderProgram()

        val vao = IntArray(1)
        glGenVertexArrays(1, vao, 0)
        mVao = vao[0]
        glBindVertexArray(mVao)

        val vbo = IntArray(1)
        glGenBuffers(1, vbo, 0)
        mVbo = vbo[0]
        glBindBuffer(GL_ARRAY_BUFFER, mVbo)
        glBufferData(
            GL_ARRAY_BUFFER,
            TEST_TRIANGLE_COORDS.size * SIZE_OF_FLOAT,
            TEST_TRIANGLE_VERTEX_BUFFER,
            GL_STATIC_DRAW
        )

        glVertexAttribPointer(
            0,
            COORDS_PER_VERTEX,
            GL_FLOAT,
            false,
            COORDS_PER_VERTEX * SIZE_OF_FLOAT,
            0
        )
        glEnableVertexAttribArray(0)

        val color = floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f)
        glUniform4fv(0, 1, color, 0)

        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        // Resize viewport
        glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(unused: GL10?) {
        // Redraw background color
        glClear(GL_COLOR_BUFFER_BIT)
        glDrawArrays(GL_TRIANGLES, 0, TEST_TRIANGLE_COORDS.size / COORDS_PER_VERTEX)
    }

    private fun createShaderProgram() {
        val vertexShader = compileShader(GL_VERTEX_SHADER, VERTEX_SHADER_SOURCE)
        val fragmentShader = compileShader(GL_FRAGMENT_SHADER, FRAGMENT_SHADER_SOURCE)

        mShaderProgram = glCreateProgram()
        glAttachShader(mShaderProgram, vertexShader)
        glAttachShader(mShaderProgram, fragmentShader)
        glLinkProgram(mShaderProgram)

        val success = IntArray(1)
        glGetProgramiv(mShaderProgram, GL_LINK_STATUS, success, 0)
        if (success[0] == 0) {
            val infoLog = glGetProgramInfoLog(mShaderProgram)
            glDeleteProgram(mShaderProgram)
            throw RuntimeException("OpenGL program linking failed: $infoLog")
        }

        glUseProgram(mShaderProgram)
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
    }

    private fun compileShader(shaderType: Int, shaderSource: String): Int {
        val shader = glCreateShader(shaderType)
        glShaderSource(shader, shaderSource)
        glCompileShader(shader)
        val success = IntArray(1)
        glGetShaderiv(shader, GL_COMPILE_STATUS, success, 0)
        if (success[0] == 0) {
            val infoLog = glGetShaderInfoLog(shader)
            glDeleteShader(shader)
            throw RuntimeException("OpenGL shader compilation failed: $infoLog")
        }
        return shader
    }

    // TODO: call it from somewhere
    fun onDestroy() {
        glDisableVertexAttribArray(mVao)
        glDeleteVertexArrays(1, IntBuffer.allocate(1).put(mVao))
        glDeleteBuffers(1, IntBuffer.allocate(1).put(mVbo))
        glDeleteProgram(mShaderProgram)
    }

    companion object {
        private const val VERTEX_SHADER_SOURCE =
            """
                #version 300 es
                
                layout (location = 0) in vec2 vPos;
                
                void main() {
                    gl_Position = vec4(vPos.x, vPos.y, 0.0, 1.0);
                }
            """
        private const val FRAGMENT_SHADER_SOURCE =
            """
                #version 300 es
                
                layout (location = 0) uniform vec4 vColor;
                out vec4 FragColor;
                
                void main() {
                    FragColor = vColor;
                }
            """

        private const val WIDTH = 256
        private const val HEIGHT = 240
        private const val SIZE_OF_FLOAT = 4
        private const val COORDS_PER_VERTEX = 2

        private val TEST_TRIANGLE_COORDS = floatArrayOf(
             0.0f,  0.622008459f,
            -0.5f, -0.311004243f,
             0.5f, -0.311004243f
        )
        private val TEST_TRIANGLE_VERTEX_BUFFER: FloatBuffer = ByteBuffer
            .allocateDirect(TEST_TRIANGLE_COORDS.size * SIZE_OF_FLOAT)
            .run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(TEST_TRIANGLE_COORDS)
                    position(0)
                }
            }

        private val QUAD_COORDS = floatArrayOf(
            -1f,  1f,
            -1f, -1f,
             1f, -1f,
             1f,  1f
        )
    }
}