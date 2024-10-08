package com.onandor.nesemu.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text("Rom select")
            Button(onClick = { pickRomFileLauncher.launch(arrayOf("*/*")) }) {
                Text("Select rom")
            }
        }
    }
}