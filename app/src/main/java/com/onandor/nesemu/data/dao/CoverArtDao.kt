package com.onandor.nesemu.data.dao

import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Upsert
import com.onandor.nesemu.data.entity.CoverArt
import kotlinx.coroutines.flow.Flow

@Dao
interface CoverArtDao {

    @Query("select * from CoverArt")
    fun observeAllUrls(): Flow<Map<@MapColumn(columnName = "romHash") String, @MapColumn(columnName = "imageUrl") String?>>

    @Query("select * from CoverArt where romHash = :romHash limit 1")
    suspend fun findByRomHash(romHash: String): CoverArt?

    @Query("select exists(select * from CoverArt where romHash = :romHash)")
    suspend fun existsByRomHash(romHash: String): Boolean

    @Upsert
    suspend fun upsert(vararg coverArts: CoverArt)
}