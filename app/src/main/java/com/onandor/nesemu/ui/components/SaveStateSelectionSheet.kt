package com.onandor.nesemu.ui.components

import android.graphics.Bitmap
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
import com.onandor.nesemu.ui.model.UiSaveState
import java.time.format.DateTimeFormatter

enum class SaveStateSheetType {
    Save,
    Load,
    LoadAndNew
}

private val EmptySaveStateCallback: (UiSaveState) -> Unit = {}

@Composable
fun SaveStateSelectionSheet(
    sheetState: ModalBottomSheetState,
    saveStates: List<UiSaveState>,
    type: SaveStateSheetType,
    onDismiss: () -> Unit,
    onSelectSaveState: (Int, UiSaveState?) -> Unit,
    onDeleteSaveState: (UiSaveState) -> Unit = EmptySaveStateCallback
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
                            val saveState = saveStates.find { it.entity.slot == slot }
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
    saveState: UiSaveState,
    onClick: (Int, UiSaveState) -> Unit,
    onDelete: (UiSaveState) -> Unit
) {
    ListItem(
        mainText = {
            Row(
                modifier = Modifier.padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier.clip(RoundedCornerShape(5.dp)),
                    painter = BitmapPainter(saveState.preview.asImageBitmap()),
                    contentDescription = "Save State Preview"
                )
                Text(
                    modifier = Modifier.padding(start = 20.dp),
                    text = saveState.name,
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
                    text = "Last played: ${saveState.lastPlayedDate}",
                    fontStyle = FontStyle.Italic
                )
                Text(
                    text = "Playtime: ${saveState.playtime}",
                    fontStyle = FontStyle.Italic
                )
            }
        },
        onClick = { onClick(saveState.entity.slot, saveState) }
    )
}