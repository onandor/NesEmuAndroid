package com.onandor.nesemu.ui.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.onandor.nesemu.data.entity.SaveState
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

data class UiSaveState(
    val entity: SaveState,
    val playtime: String,
    val lastPlayedDate: String,
    val name: String,
    val preview: Bitmap
)

fun SaveState.toUiSaveState(): UiSaveState {
    return UiSaveState(
        entity = this,
        playtime = playtime.toTimeString(),
        lastPlayedDate = formatter.format(modificationDate),
        name = if (slot == 0) "Autosave" else "Slot $slot",
        preview = BitmapFactory.decodeByteArray(preview, 0, preview.size)
    )
}

private fun Long.toTimeString(): String {
    val hours = (this / 3600).toString().padStart(2, '0')
    val minutes = ((this % 3600) / 60).toString().padStart(2, '0')
    val seconds = (this % 60).toString().padStart(2, '0')

    return "$hours:$minutes:$seconds"
}