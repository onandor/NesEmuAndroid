package com.onandor.nesemu.data.repository

import com.onandor.nesemu.data.dao.NesGameDao
import com.onandor.nesemu.data.entity.NesGame
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NesGameRepository @Inject constructor(
    private val nesGameDao: NesGameDao
) {

    suspend fun upsert(nesGame: NesGame) {
        nesGameDao.upsert(nesGame)
    }

    suspend fun findByRomHash(romHash: Long): NesGame? {
        return nesGameDao.findByRomHash(romHash)
    }
}