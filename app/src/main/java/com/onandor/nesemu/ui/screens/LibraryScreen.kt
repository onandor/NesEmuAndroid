package com.onandor.nesemu.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.composables.core.SheetDetent
import com.composables.core.rememberModalBottomSheetState
import com.onandor.nesemu.ui.components.ConfirmationDialog
import com.onandor.nesemu.ui.components.RectangularButton
import com.onandor.nesemu.ui.components.RectangularIconButton
import com.onandor.nesemu.ui.components.SaveStateSelectionSheet
import com.onandor.nesemu.ui.components.SaveStateSheetType
import com.onandor.nesemu.ui.components.TitleDialog
import com.onandor.nesemu.ui.components.TopBar
import com.onandor.nesemu.viewmodels.LibraryViewModel
import com.onandor.nesemu.viewmodels.LibraryViewModel.Event
import com.onandor.nesemu.R
import com.onandor.nesemu.ui.components.ListItem
import com.onandor.nesemu.ui.model.UiLibraryEntry
import com.onandor.nesemu.viewmodels.NavBarPage

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

    Scaffold (
        topBar = { TopBar(onEvent = viewModel::onEvent) },
        bottomBar = { NavBar(currentPage = uiState.currentPage, onEvent = viewModel::onEvent) }
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
                AnimatedContent(
                    targetState = uiState.currentPage,
                    transitionSpec = {
                        if (targetState == NavBarPage.RecentlyPlayed) {
                            slideInHorizontally { fullHeight -> -fullHeight } + fadeIn() togetherWith
                                    slideOutHorizontally { fullHeight -> fullHeight } + fadeOut()
                        } else {
                            slideInHorizontally { fullHeight -> fullHeight } + fadeIn() togetherWith
                                    slideOutHorizontally { fullHeight -> -fullHeight } + fadeOut()
                        }
                    }
                ) { targetState ->
                    if (targetState == NavBarPage.RecentlyPlayed) {
                        LibraryList(
                            entries = uiState.recentGames,
                            coverArtUrls = uiState.coverArtUrls,
                            onEvent = viewModel::onEvent
                        )
                    } else {
                        LibraryBrowser(
                            entries = uiState.displayedEntries,
                            coverArtUrls = uiState.coverArtUrls,
                            path = uiState.path,
                            inSubdirectory = uiState.inSubdirectory,
                            slideBackwards = uiState.slideLibraryListBackwards,
                            onEvent = viewModel::onEvent
                        )
                    }
                }
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

    BackHandler(uiState.currentPage != NavBarPage.RecentlyPlayed) {
        if (uiState.path != "/") {
            viewModel.onEvent(Event.OnNavigateUp)
        } else {
            viewModel.onEvent(Event.OnSwitchPage(NavBarPage.RecentlyPlayed))
        }
    }
}

@Composable
private fun LibraryBrowser(
    modifier: Modifier = Modifier,
    entries: List<UiLibraryEntry>,
    coverArtUrls: Map<String, String?>,
    path: String,
    inSubdirectory: Boolean,
    slideBackwards: Boolean,
    onEvent: (Event) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        var counter by remember { mutableIntStateOf(0) }
        var firstEntries by remember { mutableStateOf(entries) }
        var secondEntries by remember { mutableStateOf(emptyList<UiLibraryEntry>()) }

        val orientation = LocalConfiguration.current.orientation
        var skipAnimation by remember { mutableStateOf(false) }

        LaunchedEffect(orientation) {
            skipAnimation = true
        }

        LaunchedEffect(entries) {
            if (skipAnimation) {
                skipAnimation = false
                return@LaunchedEffect
            }

            if (counter % 2 == 1) {
                firstEntries = entries
            } else {
                secondEntries = entries
            }
            counter += 1
        }

        Row(
            modifier = Modifier
                .padding(top = 10.dp, bottom = 10.dp, start = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RectangularIconButton(
                onClick = { onEvent(Event.OnNavigateUp) },
                enabled = inSubdirectory
            ) {
                Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, null)
            }
            Text(
                modifier = Modifier.padding(start = 10.dp),
                fontWeight = FontWeight.SemiBold,
                text = path
            )
        }
        AnimatedContent(
            targetState = counter,
            transitionSpec = {
                if (slideBackwards) {
                    slideInHorizontally { height -> -height } togetherWith
                            slideOutHorizontally { height -> height }
                } else {
                    slideInHorizontally { height -> height } togetherWith
                            slideOutHorizontally { height -> -height }
                }
            }
        ) { counter ->
            if (counter % 2 == 0) {
                LibraryList(
                    entries = firstEntries,
                    coverArtUrls = coverArtUrls,
                    onEvent = { onEvent(it) }
                )
            } else {
                LibraryList(
                    entries = secondEntries,
                    coverArtUrls = coverArtUrls,
                    onEvent = { onEvent(it) }
                )
            }
        }
    }
}

@Composable
private fun LibraryList(
    entries: List<UiLibraryEntry>,
    coverArtUrls: Map<String, String?>,
    onEvent: (Event) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(entries, { it.entity.id }) { entry ->
            ListItem(
                mainText = {
                    Text(
                        text = entry.displayName,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                subText = {
                    if (!entry.entity.isDirectory) {
                        Text(
                            text = "Last played: ${entry.lastPlayedDate}",
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                },
                leftDisplayItem = {
                    Box(
                        modifier = Modifier
                            .requiredWidth(65.dp)
                            .heightIn(65.dp, 100.dp)
                    ) {
                        if (entry.entity.isDirectory) {
                            Icon(
                                modifier = Modifier.size(65.dp),
                                painter = painterResource(R.drawable.ic_folder),
                                contentDescription = null
                            )
                        } else {
                            AsyncImage(
                                modifier = Modifier.clip(RoundedCornerShape(5.dp)),
                                model = coverArtUrls[entry.entity.romHash],
                                contentDescription = null
                            )
                        }
                    }
                },
                onClick = { onEvent(Event.OnOpenLibraryEntry(entry)) }
            )
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
        modifier = Modifier
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .statusBarsPadding()
            .padding(top = 10.dp, bottom = 10.dp),
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

@Composable
private fun NavBar(
    currentPage: NavBarPage,
    onEvent: (Event) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_history),
                    contentDescription = null
                )
            },
            label = { Text("Recently Played") },
            selected = currentPage == NavBarPage.RecentlyPlayed,
            onClick = { onEvent(Event.OnSwitchPage(NavBarPage.RecentlyPlayed)) }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_folder),
                    contentDescription = null
                )
            },
            label = { Text("Browse") },
            selected = currentPage == NavBarPage.Browse,
            onClick = { onEvent(Event.OnSwitchPage(NavBarPage.Browse)) }
        )
    }
}