package com.onandor.nesemu.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.entity.LibraryEntryWithDate
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryEntryDao {

    @Query("""
        select
            le.*,
            max(ss.modificationDate) as lastPlayedDate
        from LibraryEntry le
        left join SaveState ss on le.romHash = ss.romHash
        where isDirectory = 0 and ss.modificationDate is not null
        group by le.id
        order by lastPlayedDate desc
        limit 20
    """)
    fun observeRecentlyPlayed(): Flow<List<LibraryEntryWithDate>>

    @Query("select * from LibraryEntry where isDirectory = 0")
    suspend fun findAllNotDirectory(): List<LibraryEntry>

    @Query("select * from LibraryEntry where romHash = :romHash limit 1")
    suspend fun findByRomHash(romHash: String): LibraryEntry?

    @Query("select * from LibraryEntry where uri = :uri limit 1")
    suspend fun findByUri(uri: String): LibraryEntry?

    @Query("""
        select
            le.*,
            max(ss.modificationDate) as lastPlayedDate
        from LibraryEntry le
        left join SaveState ss on le.romHash = ss.romHash
        where parentDirectoryUri = :parentDirectoryUri
        group by le.id
    """)
    fun observeAllByParentDirectoryUri(parentDirectoryUri: String): Flow<List<LibraryEntryWithDate>>

    @Upsert
    suspend fun upsert(vararg libraryEntries: LibraryEntry)

    @Upsert
    suspend fun upsert(libraryEntries: List<LibraryEntry>)

    @Delete
    suspend fun delete(vararg libraryEntries: LibraryEntry)

    @Query("delete from LibraryEntry where romHash != 'library_root'")
    suspend fun deleteAllExceptRoot()
}