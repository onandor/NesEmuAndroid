package com.onandor.nesemu.data.repository

import com.onandor.nesemu.data.dao.NesGameDao
import com.onandor.nesemu.data.dao.SaveStateDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NesGameRepository @Inject constructor(
    private val nesGameDao: NesGameDao
) {
}