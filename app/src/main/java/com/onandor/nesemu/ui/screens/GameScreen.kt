package com.onandor.nesemu.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.nesemu.ui.components.NesRenderer
import com.onandor.nesemu.ui.components.NesSurfaceView
import com.onandor.nesemu.input.NesButton
import com.onandor.nesemu.input.NesButtonState
import com.onandor.nesemu.ui.components.controls.DPad
import com.onandor.nesemu.ui.components.controls.FaceButton
import com.onandor.nesemu.ui.components.controls.OptionButton
import com.onandor.nesemu.ui.util.HideSystemBars
import com.onandor.nesemu.viewmodels.GameViewModel
import com.onandor.nesemu.R

@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.settingsOverlayVisible) {
                SettingsOverlay()
            }
            Game(
                renderer = viewModel.renderer,
                padding = padding,
                setRenderCallback = viewModel::setRenderCallback,
                onShowSettingsOverlay = viewModel::showSettingsOverlay,
                onQuit = viewModel::quit,
                onNavigateToDebugScreen = viewModel::navigateToDebugScreen,
                onButtonStateChanged = viewModel::buttonStateChanged,
                onDPadStateChanged = viewModel::dpadStateChanged,
                emulationPaused = uiState.emulationPaused,
                setEmulationPaused = viewModel::setEmulationState
            )
        }
    }

    if (uiState.errorMessage != null) {
        LaunchedEffect(uiState.errorMessage) {
            Toast.makeText(context, uiState.errorMessage, Toast.LENGTH_LONG).show()
            viewModel.errorMessageToastShown()
        }
    }

    BackHandler {
        viewModel.navigateBack()
    }
}

@Composable
private fun SettingsOverlay(
    modifier: Modifier = Modifier
) {

}

@Composable
private fun Game(
    modifier: Modifier = Modifier,
    padding: PaddingValues,
    renderer: NesRenderer,
    setRenderCallback: (() -> Unit) -> Unit,
    onShowSettingsOverlay: () -> Unit,
    onQuit: () -> Unit,
    onNavigateToDebugScreen: () -> Unit,
    onButtonStateChanged: (NesButton, NesButtonState) -> Unit,
    onDPadStateChanged: (Map<NesButton, NesButtonState>) -> Unit,
    emulationPaused: Boolean,
    setEmulationPaused: (Boolean) -> Unit
) {
    val configuration = LocalConfiguration.current

    if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        Column(modifier = Modifier.padding(padding)) {
            NesSurfaceView(
                modifier = modifier.fillMaxWidth().aspectRatio(256f / 240f),
                renderer = renderer,
                setRenderCallback = setRenderCallback
            )
            VerticalControls(
                onButtonStateChanged = onButtonStateChanged,
                onDPadStateChanged = onDPadStateChanged
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.padding(bottom = 10.dp, start = 10.dp, end = 10.dp)) {
                IconButton(onClick = onShowSettingsOverlay) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null
                    )
                }
                IconButton(onClick = onNavigateToDebugScreen) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { setEmulationPaused(!emulationPaused) }) {
                    if (emulationPaused) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_pause),
                            contentDescription = null
                        )
                    }
                }
                IconButton(onClick = onQuit) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null
                    )
                }
            }
        }
    } else {
        HideSystemBars()
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HorizontalControlsLeft(
                modifier = Modifier.weight(1f),
                onShowSettingsOverlay = onShowSettingsOverlay,
                onNavigateToDebugScreen = onNavigateToDebugScreen,
                onButtonStateChanged = onButtonStateChanged,
                onDPadStateChanged = onDPadStateChanged,
                emulationPaused = emulationPaused,
                setEmulationPaused = setEmulationPaused
            )
            NesSurfaceView(
                modifier = modifier.fillMaxHeight().aspectRatio(256f / 240f),
                renderer = renderer,
                setRenderCallback = setRenderCallback
            )
            HorizontalControlsRight(
                modifier = Modifier.weight(1f),
                onQuit = onQuit,
                onButtonStateChanged = onButtonStateChanged
            )
        }
    }
}

@Composable
private fun VerticalControls(
    modifier: Modifier = Modifier,
    onButtonStateChanged: (NesButton, NesButtonState) -> Unit,
    onDPadStateChanged: (Map<NesButton, NesButtonState>) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 60.dp, start = 25.dp, end = 25.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            DPad(onStateChanged = onDPadStateChanged)
            Row {
                FaceButton(
                    text = "B",
                    onStateChanged = { onButtonStateChanged(NesButton.B, it) }
                )
                Spacer(modifier = Modifier.width(15.dp))
                FaceButton(
                    text = "A",
                    onStateChanged = { onButtonStateChanged(NesButton.A, it) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            OptionButton(
                text = "SELECT",
                onStateChanged = { onButtonStateChanged(NesButton.SELECT, it) }
            )
            Spacer(modifier = Modifier.width(20.dp))
            OptionButton(
                text = "START",
                onStateChanged = { onButtonStateChanged(NesButton.START, it) }
            )
        }
    }
}

@Composable
private fun HorizontalControlsLeft(
    modifier: Modifier = Modifier,
    onShowSettingsOverlay: () -> Unit,
    onNavigateToDebugScreen: () -> Unit,
    onButtonStateChanged: (NesButton, NesButtonState) -> Unit,
    onDPadStateChanged: (Map<NesButton, NesButtonState>) -> Unit,
    emulationPaused: Boolean,
    setEmulationPaused: (Boolean) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(bottom = 10.dp, start = 10.dp)
        ) {
            IconButton(onClick = onShowSettingsOverlay) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null
                )
            }
            IconButton(onClick = onNavigateToDebugScreen) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null
                )
            }
        }
        DPad(
            modifier = Modifier.align(Alignment.Center),
            onStateChanged = onDPadStateChanged
        )
        OptionButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp),
            text = "SELECT",
            onStateChanged = { onButtonStateChanged(NesButton.SELECT, it) }
        )
    }
}

@Composable
private fun HorizontalControlsRight(
    modifier: Modifier = Modifier,
    onQuit: () -> Unit,
    onButtonStateChanged: (NesButton, NesButtonState) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(bottom = 10.dp, end = 10.dp)
        ) {
            IconButton(onClick = onQuit) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .height(140.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            FaceButton(
                text = "B",
                onStateChanged = { onButtonStateChanged(NesButton.B, it) }
            )
            Spacer(modifier = Modifier.width(15.dp))
            FaceButton(
                text = "A",
                onStateChanged = { onButtonStateChanged(NesButton.A, it) }
            )
        }
        OptionButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp),
            text = "START",
            onStateChanged = { onButtonStateChanged(NesButton.START, it) }
        )
    }
}