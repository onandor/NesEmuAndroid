package com.onandor.nesemu.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.composables.core.SheetDetent
import com.composables.core.rememberModalBottomSheetState
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.ui.components.ConfirmationDialog
import com.onandor.nesemu.ui.components.RectangularButton
import com.onandor.nesemu.ui.components.RectangularIconButton
import com.onandor.nesemu.ui.components.SaveStateSelectionSheet
import com.onandor.nesemu.ui.components.SaveStateSheetType
import com.onandor.nesemu.ui.components.StatusBarScaffold
import com.onandor.nesemu.ui.components.TitleDialog
import com.onandor.nesemu.ui.components.TopBar
import com.onandor.nesemu.viewmodels.LibraryViewModel
import com.onandor.nesemu.viewmodels.LibraryViewModel.Event
import com.onandor.nesemu.R

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
    val bottomSheetState = rememberModalBottomSheetState(initialDetent = SheetDetent.Hidden)

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
                    coverArtUrls = uiState.coverArtUrls,
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

    if (uiState.saveStateToDelete != null) {
        ConfirmationDialog(
            title = "Confirmation",
            warningText = "Are you sure you want to delete the selected save state?",
            confirmButtonLabel = "Delete",
            onResult = { viewModel.onEvent(Event.OnDeleteSaveState(it)) }
        )
    }

    LaunchedEffect(uiState.selectedGame) {
        if (uiState.selectedGame != null) {
            bottomSheetState.currentDetent = SheetDetent.FullyExpanded
        } else {
            bottomSheetState.currentDetent = SheetDetent.Hidden
        }
    }

    if (uiState.selectedGame != null) {
        SaveStateSelectionSheet(
            sheetState = bottomSheetState,
            saveStates = uiState.saveStates,
            type = SaveStateSheetType.LoadAndNew,
            onDismiss = { viewModel.onEvent(Event.OnHideSaveStateSheet) },
            onSelectSaveState = { _, saveState ->
                viewModel.onEvent(Event.OnOpenSaveState(saveState))
            },
            onDeleteSaveState = { viewModel.onEvent(Event.OnShowSaveStateDeleteDialog(it)) }
        )
    }
}

@Composable
private fun FileList(
    modifier: Modifier = Modifier,
    entries: List<LibraryEntry>,
    coverArtUrls: Map<String, String?>,
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
                LibraryEntryListItem(
                    name = entry.name,
                    isDirectory = entry.isDirectory,
                    coverArtUrl = coverArtUrls[entry.romHash],
                    onClick = { onEvent(Event.OnOpenLibraryEntry(entry)) }
                )
            }
        }
    }
}

@Composable
private fun LibraryEntryListItem(
    modifier: Modifier = Modifier,
    name: String,
    isDirectory: Boolean,
    coverArtUrl: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(top = 10.dp, bottom = 10.dp, start = 25.dp, end = 25.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDirectory) {
            Image(
                modifier = Modifier.size(65.dp),
                painter = painterResource(R.drawable.ic_folder),
                contentDescription = null
            )
        } else {
            AsyncImage(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .requiredWidth(65.dp),
                model = coverArtUrl,
                contentDescription = null
            )
        }
        Text(
            modifier = Modifier.padding(start = 15.dp),
            text = name
        )
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