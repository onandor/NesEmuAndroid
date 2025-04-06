package com.onandor.nesemu.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.OffsetDateTime

@Entity
data class LibraryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val romHash: String,
    val name: String,
    val uri: String,
    val isDirectory: Boolean,
    val parentDirectoryUri: String? = null
)

data class LibraryEntryWithDate(
    @Embedded val entry: LibraryEntry,
    val lastPlayedDate: OffsetDateTime?
)
