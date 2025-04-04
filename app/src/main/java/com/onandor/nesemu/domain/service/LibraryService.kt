package com.onandor.nesemu.domain.service

import android.net.Uri
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.repository.LibraryEntryRepository
import com.onandor.nesemu.di.IODispatcher
import com.onandor.nesemu.domain.emulation.nes.Cartridge
import com.onandor.nesemu.data.preferences.PreferenceManager
import com.onandor.nesemu.util.DocumentAccessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryService @Inject constructor(
    @IODispatcher private val ioScope: CoroutineScope,
    private val prefManager: PreferenceManager,
    private val documentAccessor: DocumentAccessor,
    private val libraryEntryRepository: LibraryEntryRepository,
    private val coverArtService: CoverArtService
) {

    data class State(
        val isLoading: Boolean = false,
        val libraryDirectory: LibraryEntry? = null
    )

    data class DirectoryListing(
        val directory: LibraryEntry?,
        val entries: List<LibraryEntry>
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private var libraryDirectory: LibraryEntry? = null

    init {
        // Retrieve the current URI first, so that the library only gets refreshed when the uri
        // changes
        runBlocking {
            val libraryUri = prefManager.getLibraryUri()
            libraryDirectory = if (libraryUri.isEmpty()) {
                null
            } else {
                libraryEntryRepository.getLibraryRoot()
            }
            _state.update { it.copy(libraryDirectory = libraryDirectory) }
        }

        ioScope.launch {
            prefManager.observeLibraryUri().collect { newLibraryUri ->
                if (newLibraryUri.isEmpty() || newLibraryUri == libraryDirectory?.uri) {
                    return@collect
                }

                _state.update { it.copy(isLoading = true) }

                val libraryDirectoryName = documentAccessor.getDocumentName(newLibraryUri) ?: ""
                libraryDirectory = libraryEntryRepository
                    .upsertLibraryDirectory(libraryDirectoryName, newLibraryUri)

                val games = persistLibrary()

                _state.update {
                    it.copy(
                        isLoading = false,
                        libraryDirectory = libraryDirectory
                    )
                }

                coverArtService.sourceUrls(games)
            }
        }
    }

    suspend fun rescanLibrary() {
        _state.update { it.copy(isLoading = true) }
        val games = persistLibrary()
        _state.update { it.copy(isLoading = false) }
        ioScope.launch { coverArtService.sourceUrls(games) }
    }

    private suspend fun persistLibrary(): List<LibraryEntry> {
        if (libraryDirectory == null) {
            return emptyList()
        }

        val documents = documentAccessor.traverseDirectory(Uri.parse(libraryDirectory!!.uri))
        val entries = documents
            .filter { it.isDirectory || it.name.endsWith(".nes") }
            .map {
                val romHash = if (!it.isDirectory) {
                    Cartridge.calculateRomHash(documentAccessor.readBytes(it.uri.toString()))
                } else {
                    ""
                }
                LibraryEntry(
                    romHash = romHash,
                    name = it.name,
                    uri = it.uri.toString(),
                    isDirectory = it.isDirectory,
                    parentDirectoryUri = it.parentDirectoryUri.toString()
                )
            }

        libraryEntryRepository.deleteAll()
        libraryEntryRepository.upsert(entries)

        return entries.filterNot { it.isDirectory }
    }

    suspend fun getEntriesInParentDirectory(directory: LibraryEntry): DirectoryListing {
        var parentDirectory: LibraryEntry?
        var entries: List<LibraryEntry>

        if (directory.parentDirectoryUri == null) {
            // We are in the root, cannot go up
            parentDirectory = directory
            entries = libraryEntryRepository.findAllByParentDirectoryUri(directory.uri)
                .sortedBy { it.name }
                .sortedBy { !it.isDirectory }
        } else {
            parentDirectory = libraryEntryRepository.findByUri(directory.parentDirectoryUri)
            entries = libraryEntryRepository.findAllByParentDirectoryUri(directory.parentDirectoryUri)
                .sortedBy { it.name }
                .sortedBy { !it.isDirectory }
        }

        return DirectoryListing(
            directory = parentDirectory,
            entries = entries
        )
    }

    suspend fun getEntriesInDirectory(directory: LibraryEntry): List<LibraryEntry> {
        return libraryEntryRepository.findAllByParentDirectoryUri(directory.uri)
            .sortedBy { it.name }
            .sortedBy { !it.isDirectory }
    }

    suspend fun changeLibraryUri(libraryUri: String) {
        prefManager.updateLibraryUri(libraryUri)
    }
}