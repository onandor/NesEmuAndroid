package com.onandor.nesemu.ui.screens

import android.view.SurfaceHolder
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.nesemu.ui.components.NesSurfaceView
import com.onandor.nesemu.viewmodels.MainViewModel

@Composable
fun GameScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    Scaffold { padding ->
        NesSurfaceView(modifier = Modifier.padding(padding))
    }
}

@Composable
private fun NesSurfaceView(modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = {
            NesSurfaceView(it).apply {
                this.holder.addCallback(object : SurfaceHolder.Callback{
                    override fun surfaceCreated(holder: SurfaceHolder) {}

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {}

                    override fun surfaceDestroyed(holder: SurfaceHolder) {}
                })
            }
        }
    )
}