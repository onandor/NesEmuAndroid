package com.onandor.nesemu.ui.model

import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.entity.LibraryEntryWithDate
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

data class UiLibraryEntry(
    val entity: LibraryEntry,
    val displayName: String,
    val lastPlayedDate: String
)

fun LibraryEntryWithDate.toUiLibraryEntry(): UiLibraryEntry {
    return UiLibraryEntry(
        entity = this.entry,
        displayName = if (!entry.isDirectory) entry.name.removeSuffix(".nes") else entry.name,
        lastPlayedDate = lastPlayedDate?.let { formatter.format(it) } ?: "never"
    )
}