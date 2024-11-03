package com.onandor.nesemu.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.nesemu.ui.components.NesRenderer
import com.onandor.nesemu.ui.components.NesSurfaceView
import com.onandor.nesemu.viewmodels.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.settingsOverlayVisible) {
                SettingsOverlay()
            }
            Game(
                renderer = viewModel.renderer,
                setRenderCallback = viewModel::setRenderCallback,
                onShowSettingsOverlay = viewModel::onShowSettingsOverlay,
                onQuit = viewModel::onQuit
            )
        }
    }
}

@Composable
private fun NesSurfaceView(
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

@Composable
private fun SettingsOverlay(
    modifier: Modifier = Modifier
) {

}

@Composable
private fun Game(
    modifier: Modifier = Modifier,
    renderer: NesRenderer,
    setRenderCallback: (() -> Unit) -> Unit,
    onShowSettingsOverlay: () -> Unit,
    onQuit: () -> Unit
) {
    val configuration = LocalConfiguration.current

    if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        Column {
            NesSurfaceView(
                modifier = modifier.fillMaxWidth().aspectRatio(256f / 240f),
                renderer = renderer,
                setRenderCallback = setRenderCallback
            )
            Button(onClick = onShowSettingsOverlay) {
                Text("Show debug overlay")
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onQuit) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null
                )
            }
            NesSurfaceView(
                modifier = modifier.fillMaxHeight().aspectRatio(256f / 240f),
                renderer = renderer,
                setRenderCallback = setRenderCallback
            )
            IconButton(onClick = onShowSettingsOverlay) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null
                )
            }
        }
    }
}