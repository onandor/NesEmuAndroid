package com.onandor.nesemu.data.repository

import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.entity.LibraryEntryWithDate
import kotlinx.coroutines.flow.Flow

interface LibraryEntryRepository {

    fun observeRecentlyPlayed(): Flow<List<LibraryEntryWithDate>>
    fun observeAllByParentDirectoryUri(parentDirectoryUri: String): Flow<List<LibraryEntryWithDate>>
    suspend fun findAllNotDirectory(): List<LibraryEntry>
    suspend fun findByUri(uri: String): LibraryEntry?
    suspend fun findByRomHash(romHash: String): LibraryEntry?
    suspend fun upsert(entry: LibraryEntry)
    suspend fun upsert(entries: List<LibraryEntry>)
    suspend fun getLibraryRoot(): LibraryEntry?
    suspend fun upsertLibraryDirectory(directoryName: String, libraryUri: String): LibraryEntry
    suspend fun deleteAll()
}