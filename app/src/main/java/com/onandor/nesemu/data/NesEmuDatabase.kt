package com.onandor.nesemu.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.onandor.nesemu.data.dao.NesGameDao
import com.onandor.nesemu.data.dao.SaveStateDao
import com.onandor.nesemu.data.entity.NesEmuTypeConverters
import com.onandor.nesemu.data.entity.NesGame
import com.onandor.nesemu.data.entity.SaveState

@Database(entities = [NesGame::class, SaveState::class], version = 1, exportSchema = false)
@TypeConverters(NesEmuTypeConverters::class)
abstract class NesEmuDatabase : RoomDatabase() {
    abstract fun nesGameDao(): NesGameDao
    abstract fun saveStateDao(): SaveStateDao
}