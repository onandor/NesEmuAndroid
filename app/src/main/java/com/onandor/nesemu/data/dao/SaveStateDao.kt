package com.onandor.nesemu.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.onandor.nesemu.data.entity.SaveState

@Dao
interface SaveStateDao {

    @Query("select * from SaveState where (libraryEntryId = :libraryEntryId and type = 'Automatic')")
    suspend fun findAutosaveByLibraryEntryId(libraryEntryId: Long): SaveState?

    @Query("select * from SaveState where romHash = :romHash")
    suspend fun findByRomHash(romHash: String): List<SaveState>

    @Upsert
    suspend fun upsert(vararg saveStates: SaveState)

    @Delete
    suspend fun delete(vararg saveStates: SaveState)
}