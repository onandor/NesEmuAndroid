package com.onandor.nesemu.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.nesemu.viewmodels.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val pickRomFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { documentUri ->
        documentUri?.let {
            val stream = context.contentResolver.openInputStream(it)
            if (stream != null) {
                viewModel.onRomSelected(stream)
            }
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text("Rom select")
            Button(onClick = { pickRomFileLauncher.launch(arrayOf("*/*")) }) {
                Text("Select rom")
            }
        }
    }

    if (uiState.errorMessage != null) {
        LaunchedEffect(uiState.errorMessage) {
            Toast.makeText(context, uiState.errorMessage, Toast.LENGTH_LONG).show()
            viewModel.errorMessageToastShown()
        }
    }
}