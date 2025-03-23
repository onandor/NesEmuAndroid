package com.onandor.nesemu.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.onandor.nesemu.data.entity.NesGame
import com.onandor.nesemu.data.entity.NesGameWithSaveStates
import kotlinx.coroutines.flow.Flow

@Dao
interface NesGameDao {

    @Transaction
    @Query("select * from NesGame")
    fun observeAll(): Flow<List<NesGameWithSaveStates>>

    @Query("select * from NesGame where romHash = :romHash")
    suspend fun findByRomHash(romHash: Long): NesGame?

    @Upsert
    suspend fun upsert(vararg nesGames: NesGame)

    @Delete
    suspend fun delete(vararg nesGames: NesGame)
}