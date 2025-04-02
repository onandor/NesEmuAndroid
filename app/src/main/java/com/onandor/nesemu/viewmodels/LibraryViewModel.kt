package com.onandor.nesemu.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.entity.SaveState
import com.onandor.nesemu.data.repository.SaveStateRepository
import com.onandor.nesemu.di.IODispatcher
import com.onandor.nesemu.service.EmulationService
import com.onandor.nesemu.service.LibraryService
import com.onandor.nesemu.navigation.NavActions
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.emulation.nes.RomParseException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    @IODispatcher private val coroutineScope: CoroutineScope,
    private val navManager: NavigationManager,
    private val emulationService: EmulationService,
    private val libraryService: LibraryService,
    private val saveStateRepository: SaveStateRepository
) : ViewModel() {

    data class UiState(
        val errorMessage: String? = null,
        val showLibraryChooserDialog: Boolean = false,
        val libraryLoading: Boolean = false,
        val displayedEntries: List<LibraryEntry> = emptyList(),
        val inSubdirectory: Boolean = false,
        val path: String = "/",

        // Save state dialog
        val selectedGame: LibraryEntry? = null,
        val saveStates: List<SaveState> = emptyList(),
        val saveStateToDelete: SaveState? = null
    )

    sealed class Event {
        data class OnNewLibrarySelected(val libraryUri: String) : Event()
        object OnRescanLibrary : Event()
        data class OnOpenLibraryEntry(val entry: LibraryEntry) : Event()
        data class OnOpenSaveState(val saveState: SaveState?) : Event()
        data class OnShowSaveStateDeleteDialog(val saveState: SaveState) : Event()
        data class OnDeleteSaveState(val confirmed: Boolean) : Event()
        object OnNavigateUp : Event()
        object OnErrorMessageToastShown : Event()
        object OnNavigateToPreferences : Event()
        object OnHideSaveStateSheet : Event()
    }

    private var libraryDirectory: LibraryEntry? = null
    private var currentDirectory: LibraryEntry? = null

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        collectLibraryServiceState()
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.OnNewLibrarySelected -> {
                coroutineScope.launch { libraryService.changeLibraryUri(event.libraryUri) }
                _uiState.update { it.copy(showLibraryChooserDialog = false) }
            }
            Event.OnRescanLibrary -> {
                libraryDirectory?.let {
                    _uiState.update { it.copy(path = "/") }
                    coroutineScope.launch {
                        libraryService.rescanLibrary()
                        navigateToDirectory(libraryDirectory!!)
                    }
                }
            }
            is Event.OnOpenLibraryEntry -> {
                if (event.entry.isDirectory) {
                    navigateToDirectory(event.entry)
                } else {
                    openGame(event.entry)
                }
            }
            is Event.OnOpenSaveState -> {
                val game = _uiState.value.selectedGame!!
                _uiState.update { it.copy(selectedGame = null) }
                launchGame(game, event.saveState)
            }
            is Event.OnShowSaveStateDeleteDialog -> {
                _uiState.update {
                    it.copy(
                        saveStateToDelete = event.saveState,
                        selectedGame = null
                    )
                }
            }
            is Event.OnDeleteSaveState -> {
                if (event.confirmed) {
                    val saveState = _uiState.value.saveStateToDelete!!
                    coroutineScope.launch { saveStateRepository.delete(saveState) }
                }
                _uiState.update { it.copy(saveStateToDelete = null) }
            }
            Event.OnNavigateUp -> {
                navigateUpOneDirectory()
            }
            Event.OnErrorMessageToastShown -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
            Event.OnNavigateToPreferences -> {
                navManager.navigateTo(NavActions.preferencesScreen())
            }
            Event.OnHideSaveStateSheet -> {
                _uiState.update { it.copy(selectedGame = null) }
            }
        }
    }

    private fun navigateToDirectory(directory: LibraryEntry) = coroutineScope.launch {
        val displayedEntries = libraryService.getEntriesInDirectory(directory)
        _uiState.update {
            val path = if (directory.uri == libraryDirectory?.uri) {
                "/"
            } else if (it.path == "/") {
                "/${directory.name}"
            } else {
                "${it.path}/${directory.name}"
            }
            it.copy(
                inSubdirectory = directory.parentDirectoryUri != null,
                displayedEntries = displayedEntries,
                path = path
            )
        }
        currentDirectory = directory
    }

    private fun navigateUpOneDirectory() = coroutineScope.launch {
        if (currentDirectory == null) {
            return@launch
        }

        val listing = libraryService.getEntriesInParentDirectory(currentDirectory!!)
        currentDirectory = if (listing.directory != null) listing.directory else libraryDirectory
        _uiState.update {
            val pathEndIndex = if (it.path.count { char -> char == '/' } > 1) {
                it.path.lastIndexOf('/')
            } else {
                1
            }
            it.copy(
                inSubdirectory = currentDirectory?.parentDirectoryUri != null,
                displayedEntries = listing.entries,
                path = it.path.substring(0, pathEndIndex)
            )
        }
    }

    private fun openGame(game: LibraryEntry) = coroutineScope.launch {
        val saveStates = saveStateRepository.findByRomHash(game.romHash)
        if (saveStates.isEmpty()) {
            launchGame(game, null)
        } else {
            _uiState.update {
                it.copy(
                    selectedGame = game,
                    saveStates = saveStates.sortedBy { it.slot }
                )
            }
        }
    }

    private fun launchGame(game: LibraryEntry, saveState: SaveState?) {
        try {
            emulationService.loadGame(game, saveState)
            navManager.navigateTo(NavActions.gameScreen())
        } catch (e: RomParseException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        } catch (e: Exception) {
            Log.e("MainViewModel", e.localizedMessage, e)
            _uiState.update {
                it.copy(errorMessage = "An unexpected error occurred while reading the ROM file")
            }
        }
    }

    private fun collectLibraryServiceState() = coroutineScope.launch {
        libraryService.state.collect { state ->
            if (state.libraryDirectory != null && libraryDirectory != state.libraryDirectory) {
                libraryDirectory = state.libraryDirectory
                currentDirectory = state.libraryDirectory
                val displayedEntries = libraryService.getEntriesInDirectory(state.libraryDirectory)
                _uiState.update {
                    it.copy(
                        inSubdirectory = false,
                        displayedEntries = displayedEntries
                    )
                }
            }

            _uiState.update {
                it.copy(
                    libraryLoading = state.isLoading,
                    showLibraryChooserDialog = libraryDirectory == null
                )
            }
        }
    }
}