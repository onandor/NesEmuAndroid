package com.onandor.nesemu.data.repository

import com.onandor.nesemu.data.dao.SaveStateDao
import com.onandor.nesemu.data.entity.SaveState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveStateRepository @Inject constructor(
    private val saveStateDao: SaveStateDao
) {

    suspend fun findByRomHash(romHash: String): List<SaveState> {
        return saveStateDao.findByRomHash(romHash)
    }

    suspend fun upsert(saveState: SaveState) {
        saveStateDao.upsert(saveState)
    }

    suspend fun delete(saveState: SaveState) {
        saveStateDao.delete(saveState)
    }
}