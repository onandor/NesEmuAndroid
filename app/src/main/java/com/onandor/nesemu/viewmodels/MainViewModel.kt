package com.onandor.nesemu.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.service.EmulationService
import com.onandor.nesemu.service.LibraryService
import com.onandor.nesemu.navigation.NavActions
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.emulation.nes.RomParseException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.FileNotFoundException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val navManager: NavigationManager,
    private val emulationService: EmulationService,
    private val libraryService: LibraryService
) : ViewModel() {

    data class UiState(
        val errorMessage: String? = null,
        val showLibraryChooserDialog: Boolean = false,
        val librarySpecified: Boolean = false,
        val libraryLoading: Boolean = false,
        val displayedEntries: List<LibraryEntry> = emptyList(),
        val inSubdirectory: Boolean = false
    )

    sealed class Event {
        data class OnNewLibrarySelected(val libraryUri: String) : Event()
        object OnShowLibraryChooserDialog : Event()
        object OnHideLibraryChooserDialog : Event()
        data class OnOpenLibraryEntry(val entry: LibraryEntry) : Event()
        object OnNavigateUp : Event()
        object OnErrorMessageToastShown : Event()
        object OnNavigateToPreferences : Event()
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
                viewModelScope.launch { libraryService.changeLibraryUri(event.libraryUri) }
                _uiState.update {
                    it.copy(
                        showLibraryChooserDialog = false,
                        librarySpecified = true
                    )
                }
            }
            Event.OnShowLibraryChooserDialog -> {
                _uiState.update { it.copy(showLibraryChooserDialog = true) }
            }
            Event.OnHideLibraryChooserDialog -> {
                _uiState.update { it.copy(showLibraryChooserDialog = false) }
            }
            is Event.OnOpenLibraryEntry -> {
                if (event.entry.isDirectory) {
                    navigateToDirectory(event.entry)
                } else {
                    println("file clicked")
                }
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
        }
    }

    private fun navigateToDirectory(directory: LibraryEntry) = viewModelScope.launch {
        val displayedEntries = libraryService.getEntriesInDirectory(directory)
        currentDirectory = directory
        _uiState.update { it.copy(displayedEntries = emptyList()) }
        _uiState.update {
            it.copy(
                inSubdirectory = currentDirectory?.parentDirectoryUri != null,
                displayedEntries = displayedEntries
            )
        }
    }

    private fun navigateUpOneDirectory() = viewModelScope.launch {
        if (currentDirectory == null) {
            return@launch
        }

        val listing = libraryService.getEntriesInParentDirectory(currentDirectory!!)
        currentDirectory = if (listing.directory != null) listing.directory else libraryDirectory
        _uiState.update {
            it.copy(
                inSubdirectory = currentDirectory?.parentDirectoryUri != null,
                displayedEntries = listing.entries
            )
        }
    }

    private fun onRomSelected(uriString: String) {
        try {
            //emulator.loadRomFile(uriString)
            //navManager.navigateTo(NavActions.gameScreen())
        } catch (e: RomParseException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        } catch (e: FileNotFoundException) {
            _uiState.update { it.copy(errorMessage = "The selected ROM file is missing") }
        } catch (e: Exception) {
            Log.e("MainViewModel", e.localizedMessage, e)
            _uiState.update {
                it.copy(errorMessage = "An unexpected error occurred while reading the ROM file")
            }
        }
    }

    private fun collectLibraryServiceState() = viewModelScope.launch {
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

            val showLibraryChooserDialog = libraryDirectory == null
            val librarySpecified = libraryDirectory != null

            _uiState.update {
                it.copy(
                    libraryLoading = state.isLoading,
                    showLibraryChooserDialog = showLibraryChooserDialog,
                    librarySpecified = librarySpecified
                )
            }
        }
    }
}