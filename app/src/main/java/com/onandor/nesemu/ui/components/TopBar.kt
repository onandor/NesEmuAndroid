package com.onandor.nesemu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    title: String,
    actions: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 50.dp)
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp)
    ) {
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            navigationIcon()
        }
        Box(modifier = Modifier.align(Alignment.Center)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            Row {
                actions()
            }
        }
    }
}