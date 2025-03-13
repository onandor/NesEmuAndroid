package com.onandor.nesemu.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.nesemu.viewmodels.MainViewModel
import com.onandor.nesemu.viewmodels.MainViewModel.Event

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { documentUri ->
        documentUri?.let {
            val stream = context.contentResolver.openInputStream(it)
            if (stream != null) {
                viewModel.onEvent(Event.OnRomSelected(stream))
            }
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier.padding(top = 200.dp),
                text = "NES Emulator",
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                modifier = Modifier
                    .width(250.dp)
                    .height(75.dp),
                onClick = {
                    filePickerLauncher.launch(arrayOf("*/*"))
                }
            ) {
                Text(
                    text = "Select rom",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
            }
            Button(onClick = { viewModel.onEvent(Event.OnNavigateToPreferences) }) {
                Text("Preferences")
            }
            Spacer(modifier = Modifier.height(200.dp))
        }
    }

    if (uiState.errorMessage != null) {
        LaunchedEffect(uiState.errorMessage) {
            Toast.makeText(context, uiState.errorMessage, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(Event.OnErrorMessageToastShown)
        }
    }
}