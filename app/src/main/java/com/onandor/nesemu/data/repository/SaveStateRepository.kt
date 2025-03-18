package com.onandor.nesemu.data.repository

import com.onandor.nesemu.data.dao.SaveStateDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveStateRepository @Inject constructor(
    private val saveStateDao: SaveStateDao
) {
}