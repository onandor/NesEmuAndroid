package com.onandor.nesemu.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.ui.components.ListItem
import com.onandor.nesemu.ui.components.RectangularButton
import com.onandor.nesemu.ui.components.StatusBarScaffold
import com.onandor.nesemu.ui.components.TitleDialog
import com.onandor.nesemu.viewmodels.MainViewModel
import com.onandor.nesemu.viewmodels.MainViewModel.Event

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { folderUri ->
        folderUri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.onEvent(Event.OnNewLibrarySelected(it.toString()))
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    StatusBarScaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
//            Text(
//                modifier = Modifier.padding(top = 200.dp),
//                text = "NES Emulator",
//                fontWeight = FontWeight.Bold,
//                fontSize = 40.sp
//            )
//            Spacer(modifier = Modifier.weight(1f))
            if (uiState.librarySpecified) {
                RectangularButton(
                    onClick = { viewModel.onEvent(Event.OnShowLibraryChooserDialog) }
                ) {
                    Text("Select Library Folder")
                }
            }
            RectangularButton(onClick = { viewModel.onEvent(Event.OnNavigateToPreferences) }) {
                Text("Preferences")
            }
//            Spacer(modifier = Modifier.height(200.dp))
            FileList(
                inSubdirectory = uiState.inSubdirectory,
                entries = uiState.displayedEntries,
                onEvent = viewModel::onEvent
            )
        }
    }

    if (uiState.errorMessage != null) {
        LaunchedEffect(uiState.errorMessage) {
            Toast.makeText(context, uiState.errorMessage, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(Event.OnErrorMessageToastShown)
        }
    }

    if (uiState.showLibraryChooserDialog) {
        LibraryChooserDialog(
            onLaunchFolderPicker = { folderPickerLauncher.launch(null) },
            onDismissRequest = { viewModel.onEvent(Event.OnHideLibraryChooserDialog) }
        )
    }
}

@Composable
private fun FileList(
    modifier: Modifier = Modifier,
    entries: List<LibraryEntry>,
    inSubdirectory: Boolean,
    onEvent: (Event) -> Unit
) {
    LazyColumn(modifier = modifier) {
        if (inSubdirectory) {
            item {
                ListItem(
                    mainText = { Text("..") },
                    onClick = { onEvent(Event.OnNavigateUp) }
                )
            }
        }
        items(entries, LibraryEntry::id) { entry ->
            ListItem(
                mainText = { Text(entry.name) },
                onClick = { onEvent(Event.OnOpenLibraryEntry(entry)) }
            )
        }
    }
}

@Composable
private fun LibraryChooserDialog(
    onLaunchFolderPicker: () -> Unit,
    onDismissRequest: () -> Unit
) {
    TitleDialog(
        text = "Choose library location",
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, end = 15.dp)
        ) {
            Text("Please choose a folder on your device which contains the game ROMs you want to play! " +
                    "This folder will act as your game library. It can contain subfolders to further " +
                    "organize your collection.")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                RectangularButton(onClick = onLaunchFolderPicker) {
                    Text("Select Folder")
                }
            }
        }
    }
}