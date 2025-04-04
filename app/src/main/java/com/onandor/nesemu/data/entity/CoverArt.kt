package com.onandor.nesemu.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CoverArt(
    @PrimaryKey
    val romHash: String,
    val imageUrl: String?
)