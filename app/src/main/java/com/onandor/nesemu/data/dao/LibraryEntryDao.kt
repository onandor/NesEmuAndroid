package com.onandor.nesemu.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.entity.LibraryEntryWithSaveStates

@Dao
interface LibraryEntryDao {

    @Transaction
    @Query("select * from LibraryEntry")
    suspend fun findAllWithSaveStates(): List<LibraryEntryWithSaveStates>

    @Query("select * from LibraryEntry")
    suspend fun findAll(): List<LibraryEntry>

    @Query("select * from LibraryEntry where romHash = :romHash")
    suspend fun findByRomHash(romHash: String): LibraryEntry?

    @Query("select * from LibraryEntry where uri = :uri")
    suspend fun findByUri(uri: String): LibraryEntry?

    @Query("select * from LibraryEntry where parentDirectoryUri = :parentDirectoryUri")
    suspend fun findAllByParentDirectoryUri(parentDirectoryUri: String): List<LibraryEntry>

    @Upsert
    suspend fun upsert(vararg libraryEntries: LibraryEntry)

    @Upsert
    suspend fun upsert(libraryEntries: List<LibraryEntry>)

    @Delete
    suspend fun delete(vararg libraryEntries: LibraryEntry)

    @Query("delete from LibraryEntry where romHash != 'library_root'")
    suspend fun deleteAllExceptRoot()
}