package com.onandor.nesemu.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.entity.SaveState
import com.onandor.nesemu.data.repository.CoverArtRepository
import com.onandor.nesemu.data.repository.SaveStateRepository
import com.onandor.nesemu.di.IODispatcher
import com.onandor.nesemu.domain.service.EmulationService
import com.onandor.nesemu.domain.service.LibraryService
import com.onandor.nesemu.navigation.NavActions
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.domain.emulation.nes.RomParseException
import com.onandor.nesemu.ui.model.UiLibraryEntry
import com.onandor.nesemu.ui.model.UiSaveState
import com.onandor.nesemu.ui.model.toUiLibraryEntry
import com.onandor.nesemu.ui.model.toUiSaveState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    @IODispatcher private val ioScope: CoroutineScope,
    private val navManager: NavigationManager,
    private val emulationService: EmulationService,
    private val libraryService: LibraryService,
    private val saveStateRepository: SaveStateRepository,
    private val coverArtRepository: CoverArtRepository
) : ViewModel() {

    data class UiState(
        val errorMessage: String? = null,
        val showLibraryChooserDialog: Boolean = false,
        val libraryLoading: Boolean = false,
        val displayedEntries: List<UiLibraryEntry> = emptyList(),
        val inSubdirectory: Boolean = false,
        val path: String = "/",
        val coverArtUrls: Map<String, String?> = emptyMap(),

        // Save state dialog
        val selectedGame: UiLibraryEntry? = null,
        val saveStates: List<UiSaveState> = emptyList(),
        val saveStateToDelete: UiSaveState? = null
    )

    sealed class Event {
        data class OnNewLibrarySelected(val libraryUri: String) : Event()
        data object OnRescanLibrary : Event()
        data class OnOpenLibraryEntry(val entry: UiLibraryEntry) : Event()
        data class OnOpenSaveState(val saveState: UiSaveState?) : Event()
        data class OnShowSaveStateDeleteDialog(val saveState: UiSaveState) : Event()
        data class OnDeleteSaveState(val confirmed: Boolean) : Event()
        data object OnNavigateUp : Event()
        data object OnErrorMessageToastShown : Event()
        data object OnNavigateToPreferences : Event()
        data object OnHideSaveStateSheet : Event()
    }

    private var libraryDirectory: LibraryEntry? = null
    private var currentDirectory: LibraryEntry? = null

    private val _uiState = MutableStateFlow(UiState())
    val uiState = combine(
        _uiState, coverArtRepository.observeAllUrls()
    ) { uiState, coverArtUrls ->
        uiState.copy(coverArtUrls = coverArtUrls)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(2000),
            initialValue = UiState()
        )

    init {
        collectLibraryServiceState()
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.OnNewLibrarySelected -> {
                ioScope.launch { libraryService.changeLibraryUri(event.libraryUri) }
                _uiState.update { it.copy(showLibraryChooserDialog = false) }
            }
            Event.OnRescanLibrary -> {
                libraryDirectory?.let {
                    _uiState.update { it.copy(path = "/") }
                    ioScope.launch {
                        libraryService.rescanLibrary()
                        navigateToDirectory(libraryDirectory!!)
                    }
                }
            }
            is Event.OnOpenLibraryEntry -> {
                if (event.entry.entity.isDirectory) {
                    navigateToDirectory(event.entry.entity)
                } else {
                    openGame(event.entry)
                }
            }
            is Event.OnOpenSaveState -> {
                val game = _uiState.value.selectedGame!!
                _uiState.update { it.copy(selectedGame = null) }
                launchGame(game.entity, event.saveState?.entity)
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
                    ioScope.launch { saveStateRepository.delete(saveState.entity) }
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

    private fun navigateToDirectory(directory: LibraryEntry) = ioScope.launch {
        val displayedEntries = libraryService
            .getEntriesInDirectory(directory)
            .map { it.toUiLibraryEntry() }

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

    private fun navigateUpOneDirectory() = ioScope.launch {
        if (currentDirectory == null) {
            return@launch
        }

        val listing = libraryService.getEntriesInParentDirectory(currentDirectory!!)
        currentDirectory = listing.directory ?: libraryDirectory

        _uiState.update {
            val pathEndIndex = if (it.path.count { char -> char == '/' } > 1) {
                it.path.lastIndexOf('/')
            } else {
                1
            }
            it.copy(
                inSubdirectory = currentDirectory?.parentDirectoryUri != null,
                displayedEntries = listing.entries.map { it.toUiLibraryEntry() },
                path = it.path.substring(0, pathEndIndex)
            )
        }
    }

    private fun openGame(game: UiLibraryEntry) = ioScope.launch {
        val saveStates = saveStateRepository
            .findByRomHash(game.entity.romHash)
            .map { it.toUiSaveState() }

        if (saveStates.isEmpty()) {
            launchGame(game.entity, null)
        } else {
            _uiState.update {
                it.copy(
                    selectedGame = game,
                    saveStates = saveStates.sortedBy { it.entity.slot }
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

    private fun collectLibraryServiceState() = ioScope.launch {
        libraryService.state.collect { state ->
            if (state.libraryDirectory != null && libraryDirectory != state.libraryDirectory) {
                libraryDirectory = state.libraryDirectory
                currentDirectory = state.libraryDirectory

                val displayedEntries = libraryService
                    .getEntriesInDirectory(state.libraryDirectory)
                    .map { it.toUiLibraryEntry() }

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