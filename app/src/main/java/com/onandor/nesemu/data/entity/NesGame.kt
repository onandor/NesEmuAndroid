package com.onandor.nesemu.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity
data class NesGame(
    @PrimaryKey
    val romHash: Long,                  // SHA-1 hash of the ROM file without the header
    val fileName: String,
    val fileUri: String                 // Android persistable document URI pointing to the file
)

data class NesGameWithSaveStates(
    @Embedded
    val game: NesGame,
    @Relation(
        parentColumn = "romHash",
        entityColumn = "romHash"
    )
    val saveStates: List<SaveState>
)