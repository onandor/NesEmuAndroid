package com.onandor.nesemu.data.entity

import androidx.room.Entity
import com.onandor.nesemu.domain.emulation.savestate.NesState
import java.time.OffsetDateTime

@Entity(primaryKeys = ["romHash", "slot"])
data class SaveState(
    val romHash: String,
    val playtime: Long,
    val modificationDate: OffsetDateTime,
    val nesState: NesState,
    val slot: Int,
    val preview: ByteArray
)
