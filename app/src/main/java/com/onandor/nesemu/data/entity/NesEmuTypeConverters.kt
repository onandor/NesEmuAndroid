package com.onandor.nesemu.data.entity

import androidx.room.TypeConverter
import com.onandor.nesemu.emulation.savestate.NesState
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object NesEmuTypeConverters {

    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    @TypeConverter
    fun toOffsetDateTime(value: String?): OffsetDateTime? =
        value?.let { formatter.parse(it, OffsetDateTime::from) }

    @TypeConverter
    fun fromOffsetDateTime(date: OffsetDateTime?): String? = date?.format(formatter)

    @TypeConverter
    fun toNesState(value: String?): NesState? = value?.let { Json.decodeFromString(it) }

    @TypeConverter
    fun fromNesState(state: NesState?): String? = state?.let { Json.encodeToString(it) }
}