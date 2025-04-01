package com.onandor.nesemu.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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
