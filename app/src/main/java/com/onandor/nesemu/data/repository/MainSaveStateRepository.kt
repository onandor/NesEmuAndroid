package com.onandor.nesemu.data.repository

import com.onandor.nesemu.data.dao.SaveStateDao
import com.onandor.nesemu.data.entity.SaveState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainSaveStateRepository @Inject constructor(
    private val saveStateDao: SaveStateDao
) : SaveStateRepository {

    override suspend fun findByRomHash(romHash: String): List<SaveState> {
        return saveStateDao.findByRomHash(romHash)
    }

    override suspend fun upsert(saveState: SaveState) {
        saveStateDao.upsert(saveState)
    }

    override suspend fun delete(saveState: SaveState) {
        saveStateDao.delete(saveState)
    }
}