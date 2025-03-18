package com.onandor.nesemu.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity
data class NesGame(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val fileUri: String,                // Android persistable URI pointing to the file
    val checksum: Long                  // CRC32 checksum of the ROM file without the header
)

data class NesGameWithSaveStates(
    @Embedded
    val game: NesGame,
    @Relation(
        parentColumn = "id",
        entityColumn = "nesGameId"
    )
    val saveStates: List<SaveState>
)