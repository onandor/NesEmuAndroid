package com.onandor.nesemu.data.repository

import com.onandor.nesemu.data.dao.LibraryEntryDao
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.entity.LibraryEntryWithSaveStates
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryEntryRepository @Inject constructor(
    private val libraryEntryDao: LibraryEntryDao
) {

    suspend fun findAllWithSaveStates(): List<LibraryEntryWithSaveStates> {
        return libraryEntryDao.findAllWithSaveStates()
    }

    suspend fun findAll(): List<LibraryEntry> {
        return libraryEntryDao.findAll()
    }

    suspend fun findAllByParentDirectoryUri(parentDirectoryUri: String): List<LibraryEntry> {
        return libraryEntryDao.findAllByParentDirectoryUri(parentDirectoryUri)
    }

    suspend fun upsert(entries: List<LibraryEntry>) {
        libraryEntryDao.upsert(entries)
    }

    suspend fun findByRomHash(romHash: String): LibraryEntry? {
        return libraryEntryDao.findByRomHash(romHash)
    }

    suspend fun deleteAll() {
        libraryEntryDao.deleteAll()
    }
}