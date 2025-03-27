package com.onandor.nesemu.service

import android.net.Uri
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.repository.LibraryEntryRepository
import com.onandor.nesemu.di.DefaultDispatcher
import com.onandor.nesemu.di.IODispatcher
import com.onandor.nesemu.emulation.nes.Cartridge
import com.onandor.nesemu.preferences.PreferenceManager
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
    @IODispatcher coroutineScope: CoroutineScope,
    private val prefManager: PreferenceManager,
    private val documentAccessor: DocumentAccessor,
    private val libraryEntryRepository: LibraryEntryRepository
) {

    data class State(
        val isLoading: Boolean = false,
        val libraryUri: String = ""
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private var libraryUri: String = ""

    init {
        // Retrieve the current URI first, so that the library only gets refreshed when the uri
        // changes
        runBlocking { _state.update { it.copy(libraryUri = prefManager.getLibraryUri()) } }
        coroutineScope.launch {
            prefManager.observeLibraryUri().collect { newLibraryUri ->
                if (newLibraryUri == libraryUri) {
                    return@collect
                }
                libraryUri = newLibraryUri
                refreshLibrary()
                _state.update { it.copy(libraryUri = newLibraryUri) }
            }
        }
    }

    suspend fun refreshLibrary() {
        _state.update { it.copy(isLoading = true) }

        val documents = documentAccessor.traverseDirectory(Uri.parse(libraryUri))
        val entries = documents.map {
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

        _state.update { it.copy(isLoading = false) }
    }

    suspend fun getEntriesInDirectory(directoryUri: String): List<LibraryEntry> {
        return libraryEntryRepository.findAllByParentDirectoryUri(directoryUri)
            .sortedBy { it.isDirectory }
    }

    suspend fun changeLibraryUri(libraryUri: String) {
        prefManager.updateLibraryUri(libraryUri)
    }
}