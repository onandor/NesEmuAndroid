package com.onandor.nesemu.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.core.DragIndication
import com.composables.core.ModalBottomSheet
import com.composables.core.ModalBottomSheetState
import com.composables.core.Scrim
import com.composables.core.Sheet
import com.onandor.nesemu.data.entity.SaveState
import java.time.format.DateTimeFormatter
import kotlin.collections.forEach

enum class SaveStateSheetType {
    Save,
    Load,
    LoadAndNew
}

private val EmptySaveStateCallback: (SaveState) -> Unit = {}

@Composable
fun SaveStateSelectionSheet(
    sheetState: ModalBottomSheetState,
    saveStates: List<SaveState>,
    type: SaveStateSheetType,
    onDismiss: () -> Unit,
    onSelectSaveState: (Int, SaveState?) -> Unit,
    onDeleteSaveState: (SaveState) -> Unit = EmptySaveStateCallback
) {
    ModalBottomSheet(
        state = sheetState,
        onDismiss = onDismiss,
    ) {
        Scrim()
        Sheet(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DragIndication(
                    modifier = Modifier
                        .padding(top = 15.dp, bottom = 15.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(100)
                        )
                        .width(32.dp)
                        .height(4.dp)
                )
                if (type == SaveStateSheetType.LoadAndNew) {
                    ListItem(
                        mainText = {
                            Text(
                                text = "New game",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        onClick = { onSelectSaveState(0, null) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp))
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (saveStates.isEmpty() &&
                        (type == SaveStateSheetType.Load || type == SaveStateSheetType.LoadAndNew)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp, bottom = 20.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No save states",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }
                    } else {
                        val startIdx = if (type == SaveStateSheetType.Save) 1 else 0
                        for (slot in startIdx .. 5) {
                            val saveState = saveStates.find { it.slot == slot }
                            if (saveState == null && type == SaveStateSheetType.Save) {
                                ListItem(
                                    mainText = {
                                        Text(
                                            text = "Slot $slot",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    onClick = { onSelectSaveState(slot, null) }
                                )
                            } else if (saveState != null) {
                                SaveStateListItem(
                                    saveState = saveState,
                                    onClick = onSelectSaveState,
                                    onDelete = onDeleteSaveState
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveStateListItem(
    saveState: SaveState,
    onClick: (Int, SaveState) -> Unit,
    onDelete: (SaveState) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val name = if (saveState.slot == 0) "Exit save" else "Slot ${saveState.slot}"
    ListItem(
        mainText = {
            Row(
                modifier = Modifier.padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SaveStatePreview(
                    modifier = Modifier.clip(RoundedCornerShape(5.dp)),
                    previewBytes = saveState.preview
                )
                Text(
                    modifier = Modifier.padding(start = 20.dp),
                    text = name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (onDelete !== EmptySaveStateCallback) {
                    Spacer(modifier = Modifier.weight(1f))
                    RectangularIconButton(onClick = { onDelete(saveState) }) {
                        Icon(Icons.Default.Delete, null)
                    }
                }
            }
        },
        subText = {
            Column {
                Text(
                    text = "Last played: ${formatter.format(saveState.modificationDate)}",
                    fontStyle = FontStyle.Italic
                )
                Text(
                    text = "Playtime: ${saveState.playtime.toTime()}",
                    fontStyle = FontStyle.Italic
                )
            }
        },
        onClick = { onClick(saveState.slot, saveState) }
    )
}

@Composable
private fun SaveStatePreview(
    modifier: Modifier = Modifier,
    previewBytes: ByteArray
) {
    val bitmap = remember(previewBytes) {
        BitmapFactory.decodeByteArray(previewBytes, 0, previewBytes.size)
    }

    if (bitmap != null) {
        Image(
            modifier = modifier,
            painter = BitmapPainter(bitmap.asImageBitmap()),
            contentDescription = "Save State Preview"
        )
    }
}

private fun Long.toTime(): String {
    val hours = (this / 3600).toString().padStart(2, '0')
    val minutes = ((this % 3600) / 60).toString().padStart(2, '0')
    val seconds = (this % 60).toString().padStart(2, '0')

    return "$hours:$minutes:$seconds"
}