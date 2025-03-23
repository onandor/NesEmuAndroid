package com.onandor.nesemu.data.repository

import com.onandor.nesemu.data.dao.SaveStateDao
import com.onandor.nesemu.data.entity.SaveState
import com.onandor.nesemu.data.entity.SaveStateType
import com.onandor.nesemu.emulation.savestate.NesState
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveStateRepository @Inject constructor(
    private val saveStateDao: SaveStateDao
) {

    suspend fun upsertAutosave(romHash: Long, sessionPlaytime: Long, nesState: NesState) {
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
                type = SaveStateType.Automatic
            )
        }
        saveStateDao.upsert(autosave)
    }

    suspend fun upsert(saveState: SaveState) {
        saveStateDao.upsert(saveState)
    }
}