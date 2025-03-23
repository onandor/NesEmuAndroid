package com.onandor.nesemu.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.onandor.nesemu.data.entity.SaveState
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveStateDao {

    @Query("select * from SaveState")
    fun observeAll(): Flow<List<SaveState>>

    @Query("select * from SaveState where (romHash = :romHash and type = 'Automatic')")
    suspend fun findAutosaveByRomHash(romHash: Long): SaveState?

    @Upsert
    suspend fun upsert(vararg saveStates: SaveState)

    @Delete
    suspend fun delete(vararg saveStates: SaveState)
}