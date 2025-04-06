package com.onandor.nesemu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private val EmptyComposable: @Composable() () -> Unit = {}

@Composable
fun CheckboxListItem(
    modifier: Modifier = Modifier,
    initialValue: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
    text: @Composable() () -> Unit
) {
    var checked by remember { mutableStateOf(initialValue) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                checked = !checked
                onCheckedChange(checked)
            }
            .padding(start = 25.dp, end = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        text()
        Checkbox(
            checked = checked,
            onCheckedChange = {
                checked = it
                onCheckedChange(checked)
            }
        )
    }
}

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    mainText: @Composable() () -> Unit,
    subText: @Composable() () -> Unit = EmptyComposable,
    leftDisplayItem: @Composable() () -> Unit = EmptyComposable,
    rightDisplayItem: @Composable() () -> Unit = EmptyComposable
) {
    val modifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 70.dp)
            .padding(top = 10.dp, bottom = 10.dp, start = 25.dp, end = 25.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leftDisplayItem !== EmptyComposable) {
            leftDisplayItem()
            Spacer(modifier = Modifier.width(15.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                mainText()
                subText()
            }
            if (rightDisplayItem !== EmptyComposable) {
                Spacer(modifier = Modifier.width(15.dp))
                rightDisplayItem()
            }
        }
    }
}