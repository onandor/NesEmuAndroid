package com.onandor.nesemu.data.repository

import com.onandor.nesemu.data.dao.CoverArtDao
import com.onandor.nesemu.data.entity.CoverArt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoverArtRepository @Inject constructor(
    private val coverArtDao: CoverArtDao
) {

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