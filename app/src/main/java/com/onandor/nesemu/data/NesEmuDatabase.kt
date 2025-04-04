package com.onandor.nesemu.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.onandor.nesemu.data.dao.CoverArtDao
import com.onandor.nesemu.data.dao.LibraryEntryDao
import com.onandor.nesemu.data.dao.SaveStateDao
import com.onandor.nesemu.data.entity.CoverArt
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.entity.NesEmuTypeConverters
import com.onandor.nesemu.data.entity.SaveState

@Database(
    entities = [LibraryEntry::class, SaveState::class, CoverArt::class],
    version = 10,
    exportSchema = false
)
@TypeConverters(NesEmuTypeConverters::class)
abstract class NesEmuDatabase : RoomDatabase() {
    abstract fun libraryEntryDao(): LibraryEntryDao
    abstract fun saveStateDao(): SaveStateDao
    abstract fun coverArtDao(): CoverArtDao
}