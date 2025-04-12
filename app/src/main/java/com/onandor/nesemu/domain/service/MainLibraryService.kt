package com.onandor.nesemu.domain.service

import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.repository.LibraryEntryRepository
import com.onandor.nesemu.di.IODispatcher
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
import androidx.core.net.toUri
import com.onandor.nesemu.data.entity.LibraryEntryWithDate
import com.onandor.nesemu.domain.service.LibraryService.State
import com.onandor.nesemu.util.sha1Hash
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map

@Singleton
class MainLibraryService @Inject constructor(
    @IODispatcher private val ioScope: CoroutineScope,
    private val prefManager: PreferenceManager,
    private val documentAccessor: DocumentAccessor,
    private val libraryEntryRepository: LibraryEntryRepository,
    private val coverArtService: CoverArtService
) : LibraryService {

    private val _state = MutableStateFlow(State())
    override val state = _state.asStateFlow()

    private var entryJob: Job? = null
    private val _displayedEntries = MutableStateFlow<List<LibraryEntryWithDate>>(emptyList())
    override val displayedEntries = _displayedEntries.asStateFlow()

    private var libraryDirectory: LibraryEntry? = null
    private var currentDirectory: LibraryEntry? = null

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
            libraryDirectory?.let {
                navigateToDirectory(it)
            }
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

                navigateToDirectory(libraryDirectory!!)

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

    override suspend fun rescanLibrary() {
        _state.update { it.copy(isLoading = true) }
        val games = persistLibrary()
        libraryDirectory?.let {
            navigateToDirectory(it)
        }
        _state.update { it.copy(isLoading = false) }
        ioScope.launch { coverArtService.sourceUrls(games) }
    }

    private suspend fun persistLibrary(): List<LibraryEntry> {
        if (libraryDirectory == null) {
            return emptyList()
        }

        val documents = documentAccessor.traverseDirectory(libraryDirectory!!.uri.toUri())
        val entries = documents
            .filter { it.isDirectory || it.name.endsWith(".nes") }
            .map {
                val romHash = if (!it.isDirectory) {
                    documentAccessor.readBytes(it.uri).sha1Hash(fromIndex = 16)
                } else {
                    ""
                }
                LibraryEntry(
                    romHash = romHash,
                    name = it.name,
                    uri = it.uri,
                    isDirectory = it.isDirectory,
                    parentDirectoryUri = it.parentDirectoryUri
                )
            }

        libraryEntryRepository.deleteAll()
        libraryEntryRepository.upsert(entries)

        return entries.filterNot { it.isDirectory }
    }

    override fun navigateToDirectory(directory: LibraryEntry) {
        currentDirectory = directory
        collectDirectoryEntries(directory)
    }

    override suspend fun navigateUpOneDirectory(): LibraryEntry? {
        currentDirectory?.let {
            if (it.parentDirectoryUri == null) {
                return@let
            }

            val parentDirectory = libraryEntryRepository.findByUri(it.parentDirectoryUri)
            if (parentDirectory == null) {
                return@let
            }

            navigateToDirectory(parentDirectory)
        }

        return currentDirectory
    }

    override suspend fun changeLibraryUri(libraryUri: String) {
        prefManager.updateLibraryUri(libraryUri)
    }

    private fun collectDirectoryEntries(directory: LibraryEntry) {
        entryJob?.cancel()
        entryJob = ioScope.launch {
            libraryEntryRepository
                .observeAllByParentDirectoryUri(directory.uri)
                .map { entries ->
                    entries
                        .sortedBy { it.entry.name }
                        .sortedBy { !it.entry.isDirectory }
                }
                .collect { entries ->
                    _displayedEntries.update { entries }
                }
        }
    }
}