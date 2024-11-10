package com.onandor.nesemu.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            .padding(start = 20.dp, end = 20.dp)
            .clickable {
                checked = !checked
                onCheckedChange(checked)
            },
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