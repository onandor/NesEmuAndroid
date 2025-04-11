package com.onandor.nesemu.data.repository

import com.onandor.nesemu.data.entity.SaveState

interface SaveStateRepository {

    suspend fun findByRomHash(romHash: String): List<SaveState>
    suspend fun upsert(saveState: SaveState)
    suspend fun delete(saveState: SaveState)
}