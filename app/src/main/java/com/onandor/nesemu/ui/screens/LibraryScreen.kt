package com.onandor.nesemu.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.ui.components.ListItem
import com.onandor.nesemu.ui.components.RectangularButton
import com.onandor.nesemu.ui.components.RectangularIconButton
import com.onandor.nesemu.ui.components.StatusBarScaffold
import com.onandor.nesemu.ui.components.TitleDialog
import com.onandor.nesemu.ui.components.TopBar
import com.onandor.nesemu.viewmodels.LibraryViewModel
import com.onandor.nesemu.viewmodels.LibraryViewModel.Event

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel()
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

    StatusBarScaffold(
        topBar = { TopBar(onEvent = viewModel::onEvent) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uiState.libraryLoading) {
                LibraryScanning()
            } else {
                FileList(
                    entries = uiState.displayedEntries,
                    path = uiState.path,
                    inSubdirectory = uiState.inSubdirectory,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }

    if (uiState.errorMessage != null) {
        LaunchedEffect(uiState.errorMessage) {
            Toast.makeText(context, uiState.errorMessage, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(Event.OnErrorMessageToastShown)
        }
    }

    if (!uiState.libraryLoading && uiState.showLibraryChooserDialog) {
        LibraryChooserDialog { folderPickerLauncher.launch(null) }
    }
}

@Composable
private fun FileList(
    modifier: Modifier = Modifier,
    entries: List<LibraryEntry>,
    path: String,
    inSubdirectory: Boolean,
    onEvent: (Event) -> Unit
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(start = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RectangularIconButton(
                onClick = { onEvent(Event.OnNavigateUp) },
                enabled = inSubdirectory
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, null)
            }
            Text(
                modifier = Modifier.padding(start = 10.dp),
                fontWeight = FontWeight.SemiBold,
                text = path
            )
        }
        LazyColumn {
            items(entries, LibraryEntry::id) { entry ->
                ListItem(
                    mainText = { Text(entry.name) },
                    onClick = { onEvent(Event.OnOpenLibraryEntry(entry)) }
                )
            }
        }
    }
}

@Composable
private fun LibraryScanning() {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .width(50.dp)
                    .padding(bottom = 30.dp)
            )
            Text("Scanning library...")
        }
    }
}

@Composable
private fun LibraryChooserDialog(
    onLaunchFolderPicker: () -> Unit
) {
    TitleDialog(
        text = "Choose library location",
        onDismissRequest = {}
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

@Composable
private fun TopBar(onEvent: (Event) -> Unit) {
    TopBar(
        title = "Library",
        actions = {
            RectangularIconButton(onClick = { onEvent(Event.OnRescanLibrary) }) {
                Icon(imageVector = Icons.Default.Refresh, null)
            }
            RectangularIconButton(onClick = { onEvent(Event.OnNavigateToPreferences) }) {
                Icon(imageVector = Icons.Default.Settings, null)
            }
        }
    )
}