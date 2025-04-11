package com.onandor.nesemu.data.repository

import com.onandor.nesemu.data.dao.CoverArtDao
import com.onandor.nesemu.data.entity.CoverArt
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainCoverArtRepository @Inject constructor(
    private val coverArtDao: CoverArtDao
) : CoverArtRepository {

    override fun observeAllUrls(): Flow<Map<String, String?>> {
        return coverArtDao.observeAllUrls()
    }

    override suspend fun findByRomHash(romHash: String): CoverArt? {
        return coverArtDao.findByRomHash(romHash)
    }

    override suspend fun existsByRomHash(romHash: String): Boolean {
        return coverArtDao.existsByRomHash(romHash)
    }

    override suspend fun upsert(coverArt: CoverArt) {
        coverArtDao.upsert(coverArt)
    }
}