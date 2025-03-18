package com.onandor.nesemu.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.onandor.nesemu.data.entity.NesGame
import com.onandor.nesemu.data.entity.NesGameWithSaveStates
import kotlinx.coroutines.flow.Flow

@Dao
interface NesGameDao {

    @Transaction
    @Query("select * from NesGame")
    fun observeAll(): Flow<List<NesGameWithSaveStates>>

    @Query("select * from NesGame where id = :id")
    suspend fun findById(id: Long): NesGame?

    @Query("select * from NesGame where fileName = :fileName")
    suspend fun findByFileName(fileName: String): NesGame?

    @Query("select * from NesGame where fileUri = :fileUri")
    suspend fun findByFileUri(fileUri: String): NesGame?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg nesGames: NesGame)

    @Update
    suspend fun update(vararg nesGames: NesGame)

    @Delete
    suspend fun delete(vararg nesGames: NesGame)
}