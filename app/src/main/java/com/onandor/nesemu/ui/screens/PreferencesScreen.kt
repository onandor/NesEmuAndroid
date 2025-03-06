package com.onandor.nesemu.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.nesemu.input.NesInputDevice
import com.onandor.nesemu.input.NesInputManager
import com.onandor.nesemu.ui.components.ClickableListItem
import com.onandor.nesemu.viewmodels.PreferencesViewModel
import com.onandor.nesemu.viewmodels.PreferencesViewModel.Event

@Composable
fun PreferencesScreen(
    viewModel: PreferencesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopBar { viewModel.onEvent(Event.OnNavigateBack) } }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            ClickableListItem(
                onClick = {
                    viewModel.onEvent(
                        Event.OnOpenDeviceSelectionDialog(NesInputManager.CONTROLLER_1)
                    )
                },
                mainText = { Text("Controller 1") },
                subText = {
                    val text = uiState.controller1Device?.name ?: "not connected"
                    Text(text)
                }
            )
            ClickableListItem(
                onClick = {
                    viewModel.onEvent(
                        Event.OnOpenDeviceSelectionDialog(NesInputManager.CONTROLLER_2)
                    )
                },
                mainText = { Text("Controller 2") },
                subText = {
                    val text = uiState.controller2Device?.name ?: "not connected"
                    Text(text)
                }
            )
        }
    }

    if (uiState.deviceSelectionControllerId != null) {
        DeviceSelectionDialog(
            controllerId = uiState.deviceSelectionControllerId!!,
            availableDevices = uiState.availableDevices,
            onEvent = { viewModel.onEvent(it) }
        )
    }

    BackHandler {
        viewModel.onEvent(Event.OnNavigateBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = { Text("Preferences") },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, null)
            }
        }
    )
}

@Composable
private fun DeviceSelectionDialog(
    controllerId: Int,
    availableDevices: List<NesInputDevice>,
    onEvent: (Event) -> Unit
) {
    Dialog(onDismissRequest = { onEvent(Event.OnCloseDeviceSelectionDialog) }) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
                    text = "Select input device"
                )
                availableDevices.forEach { device ->
                    ClickableListItem(
                        onClick = { onEvent(Event.OnDeviceSelected(controllerId, device)) },
                        mainText = { Text(device.name) }
                    )
                }
            }
        }
    }
}