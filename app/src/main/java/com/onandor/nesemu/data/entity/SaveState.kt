package com.onandor.nesemu.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onandor.nesemu.emulation.savestate.NesState
import java.time.OffsetDateTime

enum class SaveStateType {
    Automatic,
    Manual
}

@Entity
data class SaveState(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val romHash: String,
    val libraryEntryId: Long,
    val playtime: Long,
    val modificationDate: OffsetDateTime,
    val nesState: NesState,
    val type: SaveStateType
)
