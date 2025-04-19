package com.onandor.nesemu.ui.components.game

import android.graphics.Bitmap
import android.opengl.GLES11Ext.GL_BGRA
import android.opengl.GLES30.*
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class NesRenderer(private val width: Int, private val height: Int) : GLSurfaceView.Renderer {

    private var mTexture: Int = 0
    private var mShaderProgram: Int = 0
    private var mVao: Int = 0
    private var mVbo: Int = 0
    private var mEbo: Int = 0
    private var mTextureData: IntArray = IntArray(width * height)

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        createShaderProgram()
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Create the vertex attribute array
        val vao = IntArray(1)
        glGenVertexArrays(1, vao, 0)
        mVao = vao[0]
        glBindVertexArray(mVao)

        // Create the vertex buffer object and buffer the vertices of the screen
        val vbo = IntArray(1)
        glGenBuffers(1, vbo, 0)
        mVbo = vbo[0]
        glBindBuffer(GL_ARRAY_BUFFER, mVbo)
        glBufferData(
            GL_ARRAY_BUFFER,
            SCREEN_VERTICES.size * SIZE_OF_FLOAT,
            TEST_QUAD_VERTEX_BUFFER,
            GL_STATIC_DRAW
        )

        // Create the element buffer object and buffer the index order of the screen
        val ebo = IntArray(1)
        glGenBuffers(1, ebo, 0)
        mEbo = ebo[0]
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, mEbo)
        glBufferData(
            GL_ELEMENT_ARRAY_BUFFER,
            SCREEN_INDICES.size * SIZE_OF_SHORT,
            QUAD_INDICES_BUFFER,
            GL_STATIC_DRAW
        )

        // Vertex position attribute
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * SIZE_OF_FLOAT, 0)
        glEnableVertexAttribArray(0)

        // Vertex texture attribute
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * SIZE_OF_FLOAT, 2 * SIZE_OF_FLOAT)
        glEnableVertexAttribArray(1)

        // Create and bind texture
        val texture = IntArray(1)
        glGenTextures(1, texture, 0)
        mTexture = texture[0]
        glBindTexture(GL_TEXTURE_2D, mTexture)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

        glTexImage2D(GL_TEXTURE_2D, 0, GL_BGRA, width, height, 0, GL_BGRA, GL_UNSIGNED_BYTE, null)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        // Resize viewport
        glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(unused: GL10?) {
        // Redraw background color, update texture, draw quad
        glClear(GL_COLOR_BUFFER_BIT)
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height,
            GL_BGRA, GL_UNSIGNED_BYTE, IntBuffer.wrap(mTextureData))
        glDrawElements(GL_TRIANGLES, SCREEN_INDICES.size, GL_UNSIGNED_SHORT, 0)
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

    fun setTextureData(data: IntArray) {
        mTextureData = data
    }

    fun captureFrame(): Bitmap {
        return Bitmap.createBitmap(mTextureData, width, height, Bitmap.Config.RGB_565)
    }

    // Don't need to call manually
    fun onDestroy() {
        glDisableVertexAttribArray(0)
        glDisableVertexAttribArray(1)
        glDeleteVertexArrays(1, IntBuffer.allocate(1).put(mVao))
        glDeleteBuffers(1, IntBuffer.allocate(1).put(mVbo))
        glDeleteBuffers(1, IntBuffer.allocate(1).put(mEbo))
        glDeleteTextures(1, IntBuffer.allocate(1).put(mTexture))
        glDeleteProgram(mShaderProgram)
    }

    companion object {
        private val VERTEX_SHADER_SOURCE =
            """
                #version 300 es
                
                layout (location = 0) in vec2 vPos;
                layout (location = 1) in vec2 vTexCoord;
                
                out vec2 TexCoord;
                
                void main() {
                    gl_Position = vec4(vPos.x, vPos.y, 0.0, 1.0);
                    TexCoord = vTexCoord;
                }
            """.trim()

        private val FRAGMENT_SHADER_SOURCE =
            """
                #version 300 es
                
                precision mediump float;
                
                in vec2 TexCoord;
                out vec4 FragColor;
                uniform sampler2D _texture;
                
                void main() {
                    FragColor = texture(_texture, TexCoord);
                }
            """.trim()

        private const val SIZE_OF_FLOAT = 4
        private const val SIZE_OF_SHORT = 2

        private val SCREEN_VERTICES = floatArrayOf(
            // position   // color
            -1f,  1f, 0f, 0f,   // top left
            -1f, -1f, 0f, 1f,   // bottom left
            1f, -1f, 1f, 1f,   // bottom right
            1f,  1f, 1f, 0f    // top right
        )

        private val TEST_QUAD_VERTEX_BUFFER: FloatBuffer = ByteBuffer
            .allocateDirect(SCREEN_VERTICES.size * SIZE_OF_FLOAT)
            .run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(SCREEN_VERTICES)
                    position(0)
                }
            }

        private val SCREEN_INDICES = shortArrayOf(
            0, 1, 2,
            0, 2, 3
        )
        private val QUAD_INDICES_BUFFER: ShortBuffer = ByteBuffer
            .allocateDirect(SCREEN_INDICES.size * SIZE_OF_SHORT)
            .run {
                order(ByteOrder.nativeOrder())
                asShortBuffer().apply {
                    put(SCREEN_INDICES)
                    position(0)
                }
            }
    }
}