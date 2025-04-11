package com.onandor.nesemu.domain.service

import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.entity.LibraryEntryWithDate
import kotlinx.coroutines.flow.StateFlow

interface LibraryService {

    data class State(
        val isLoading: Boolean = false,
        val libraryDirectory: LibraryEntry? = null
    )

    val state: StateFlow<State>
    val displayedEntries: StateFlow<List<LibraryEntryWithDate>>

    suspend fun rescanLibrary()
    fun navigateToDirectory(directory: LibraryEntry)
    suspend fun navigateUpOneDirectory(): LibraryEntry?
    suspend fun changeLibraryUri(libraryUri: String)
}