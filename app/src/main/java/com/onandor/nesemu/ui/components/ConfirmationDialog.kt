package com.onandor.nesemu.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConfirmationDialog(
    title: String,
    warningText: String,
    confirmButtonLabel: String,
    onResult: (Boolean) -> Unit
) {
    TitleDialog(
        text = title,
        onDismissRequest = { onResult(false) }
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(warningText)
            Spacer(modifier = Modifier.height(20.dp))
            RectangularButton(onClick = { onResult(true) }) {
                Text(confirmButtonLabel)
            }
        }
    }
}