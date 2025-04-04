package com.onandor.nesemu.data.repository

import com.onandor.nesemu.data.dao.CoverArtDao
import com.onandor.nesemu.data.entity.CoverArt
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoverArtRepository @Inject constructor(
    private val coverArtDao: CoverArtDao
) {

    fun observeAllUrls(): Flow<Map<String, String?>> {
        return coverArtDao.observeAllUrls()
    }

    suspend fun findByRomHash(romHash: String): CoverArt? {
        return coverArtDao.findByRomHash(romHash)
    }

    suspend fun existsByRomHash(romHash: String): Boolean {
        return coverArtDao.existsByRomHash(romHash)
    }

    suspend fun upsert(coverArt: CoverArt) {
        coverArtDao.upsert(coverArt)
    }
}