package com.onandor.nesemu.data.repository

import com.onandor.nesemu.data.entity.CoverArt
import kotlinx.coroutines.flow.Flow

interface CoverArtRepository {

    fun observeAllUrls(): Flow<Map<String, String?>>
    suspend fun findByRomHash(romHash: String): CoverArt?
    suspend fun existsByRomHash(romHash: String): Boolean
    suspend fun upsert(coverArt: CoverArt)
}