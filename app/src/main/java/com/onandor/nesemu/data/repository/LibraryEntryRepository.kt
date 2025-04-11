package com.onandor.nesemu.data.repository

import com.onandor.nesemu.data.dao.LibraryEntryDao
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.entity.LibraryEntryWithDate
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryEntryRepository @Inject constructor(
    private val libraryEntryDao: LibraryEntryDao
) {

    fun observeRecentlyPlayed(): Flow<List<LibraryEntryWithDate>> {
        return libraryEntryDao.observeRecentlyPlayed()
    }

    fun observeAllByParentDirectoryUri(parentDirectoryUri: String): Flow<List<LibraryEntryWithDate>> {
        return libraryEntryDao.observeAllByParentDirectoryUri(parentDirectoryUri)
    }

    suspend fun findAllNotDirectory(): List<LibraryEntry> {
        return libraryEntryDao.findAllNotDirectory()
    }

    suspend fun findByUri(uri: String): LibraryEntry? {
        return libraryEntryDao.findByUri(uri)
    }

    suspend fun findByRomHash(romHash: String): LibraryEntry? {
        return libraryEntryDao.findByRomHash(romHash)
    }

    suspend fun upsert(entry: LibraryEntry) {
        libraryEntryDao.upsert(entry)
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

    suspend fun deleteAll() {
        libraryEntryDao.deleteAllExceptRoot()
    }

    companion object {
        private const val LIBRARY_ROOT = "library_root"
    }
}