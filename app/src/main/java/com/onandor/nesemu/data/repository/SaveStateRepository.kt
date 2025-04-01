package com.onandor.nesemu.data.repository

import com.onandor.nesemu.data.dao.SaveStateDao
import com.onandor.nesemu.data.entity.SaveState
import com.onandor.nesemu.emulation.savestate.NesState
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveStateRepository @Inject constructor(
    private val saveStateDao: SaveStateDao
) {

    suspend fun upsertAutosave(
        sessionPlaytime: Long,
        nesState: NesState,
        romHash: String
    ) {
        var autosave = saveStateDao.findAutosaveByRomHash(romHash)
        autosave = if (autosave != null) {
            autosave.copy(
                playtime = autosave.playtime + sessionPlaytime,
                nesState = nesState,
                modificationDate = OffsetDateTime.now()
            )
        } else {
            SaveState(
                romHash = romHash,
                playtime = sessionPlaytime,
                modificationDate = OffsetDateTime.now(),
                nesState = nesState,
                slot = 0
            )
        }
        saveStateDao.upsert(autosave)
    }

    suspend fun findByRomHash(romHash: String): List<SaveState> {
        return saveStateDao.findByRomHash(romHash)
    }

    suspend fun upsert(saveState: SaveState) {
        saveStateDao.upsert(saveState)
    }
}