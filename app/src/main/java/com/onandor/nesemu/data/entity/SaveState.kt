package com.onandor.nesemu.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onandor.nesemu.emulation.savestate.NesState
import java.time.OffsetDateTime

@Entity
data class SaveState(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nesGameId: Long,
    val playtime: Int,
    val lastPlayedDate: OffsetDateTime,
    val nesState: NesState
)
