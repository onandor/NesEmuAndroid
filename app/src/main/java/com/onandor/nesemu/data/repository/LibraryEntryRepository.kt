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

    suspend fun findByUri(uri: String): LibraryEntry? {
        return libraryEntryDao.findByUri(uri)
    }

    suspend fun upsert(entries: List<LibraryEntry>) {
        libraryEntryDao.upsert(entries)
    }

    suspend fun getLibraryRoot(): LibraryEntry? {
        return libraryEntryDao.findByRomHash(LIBRARY_ROOT)
    }

    suspend fun upsertLibraryDirectory(directoryName: String, libraryUri: String): LibraryEntry {
        var directory = getLibraryRoot()
        directory = if (directory == null) {
            LibraryEntry(
                romHash = LIBRARY_ROOT,
                name = directoryName,
                uri = libraryUri,
                isDirectory = true,
                parentDirectoryUri = null
            )
        } else {
            directory.copy(
                name = directoryName,
                uri = libraryUri
            )
        }
        libraryEntryDao.upsert(directory)
        return directory
    }

    suspend fun findByRomHash(romHash: String): LibraryEntry? {
        return libraryEntryDao.findByRomHash(romHash)
    }

    suspend fun deleteAll() {
        libraryEntryDao.deleteAllExceptRoot()
    }

    companion object {
        private const val LIBRARY_ROOT = "library_root"
    }
}