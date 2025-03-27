package com.onandor.nesemu.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity
data class LibraryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val romHash: String,
    val name: String,
    val uri: String,
    val isDirectory: Boolean,
    val parentDirectoryUri: String
)

data class LibraryEntryWithSaveStates(
    @Embedded
    val libraryEntry: LibraryEntry,
    @Relation(
        parentColumn = "romHash",
        entityColumn = "romHash"
    )
    val saveStates: List<SaveState>
)