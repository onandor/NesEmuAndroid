package com.onandor.nesemu.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.core.DragIndication
import com.composables.core.ModalBottomSheet
import com.composables.core.ModalBottomSheetState
import com.composables.core.Scrim
import com.composables.core.Sheet
import com.composables.core.SheetDetent
import com.composables.core.rememberModalBottomSheetState
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.entity.SaveState
import com.onandor.nesemu.ui.components.HorizontalDivider
import com.onandor.nesemu.ui.components.ListItem
import com.onandor.nesemu.ui.components.RectangularButton
import com.onandor.nesemu.ui.components.RectangularIconButton
import com.onandor.nesemu.ui.components.StatusBarScaffold
import com.onandor.nesemu.ui.components.TitleDialog
import com.onandor.nesemu.ui.components.TopBar
import com.onandor.nesemu.viewmodels.LibraryViewModel
import com.onandor.nesemu.viewmodels.LibraryViewModel.Event
import java.time.format.DateTimeFormatter

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

    LaunchedEffect(uiState.selectedGame) {
        if (uiState.selectedGame != null) {
            bottomSheetState.currentDetent = SheetDetent.FullyExpanded
        } else {
            bottomSheetState.currentDetent = SheetDetent.Hidden
        }
    }

    SaveStateSelectionSheet(
        sheetState = bottomSheetState,
        game = uiState.selectedGame,
        saveStates = uiState.saveStates,
        onEvent = viewModel::onEvent
    )
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
private fun SaveStateSelectionSheet(
    sheetState: ModalBottomSheetState,
    game: LibraryEntry?,
    saveStates: List<SaveState>,
    onEvent: (Event) -> Unit
) {
    ModalBottomSheet(
        state = sheetState,
        onDismiss = { onEvent(Event.OnHideSaveStateDialog) },
    ) {
        Scrim()
        Sheet(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DragIndication(
                    modifier = Modifier
                        .padding(top = 15.dp, bottom = 15.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(100))
                        .width(32.dp)
                        .height(4.dp)
                )
                ListItem(
                    mainText = {
                        Text(
                            text = "New game",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    onClick = { onEvent(Event.OnOpenSaveState(null)) }
                )
                HorizontalDivider(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp))
                saveStates.forEach { saveState ->
                    SaveStateListItem(
                        saveState = saveState,
                        onClick = { onEvent(Event.OnOpenSaveState(saveState)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveStateListItem(
    saveState: SaveState,
    onClick: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val name = if (saveState.slot == 0) "Autosave" else "Slot ${saveState.slot}"
    ListItem(
        mainText = {
            Text(
                text = name,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        subText = {
            Column {
                Text(
                    text = "Last played: ${formatter.format(saveState.modificationDate)}",
                    fontStyle = FontStyle.Italic
                )
                Text(
                    text = "Playtime: ${saveState.playtime.toTime()}",
                    fontStyle = FontStyle.Italic
                )
            }
        },
        onClick = onClick
    )
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

private fun Long.toTime(): String {
    val hours = (this / 3600).toString().padStart(2, '0')
    val minutes = ((this % 3600) / 60).toString().padStart(2, '0')
    val seconds = (this % 60).toString().padStart(2, '0')

    return "$hours:$minutes:$seconds"
}