package com.onandor.nesemu.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.onandor.nesemu.data.entity.SaveState
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveStateDao {

    @Query("select * from SaveState")
    fun observeAll(): Flow<List<SaveState>>

    @Query("select * from SaveState where nesGameId = :gameId")
    fun findAllByGameId(gameId: Long): List<SaveState>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg saveStates: SaveState)

    @Update
    suspend fun update(vararg saveStates: SaveState)

    @Delete
    suspend fun delete(vararg saveStates: SaveState)
}