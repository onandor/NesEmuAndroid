package com.onandor.nesemu.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.onandor.nesemu.data.dao.LibraryEntryDao
import com.onandor.nesemu.data.dao.SaveStateDao
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.entity.NesEmuTypeConverters
import com.onandor.nesemu.data.entity.SaveState

@Database(entities = [LibraryEntry::class, SaveState::class], version = 6, exportSchema = false)
@TypeConverters(NesEmuTypeConverters::class)
abstract class NesEmuDatabase : RoomDatabase() {
    abstract fun libraryEntryDao(): LibraryEntryDao
    abstract fun saveStateDao(): SaveStateDao
}