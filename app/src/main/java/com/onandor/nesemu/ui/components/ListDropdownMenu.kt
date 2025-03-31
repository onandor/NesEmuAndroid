package com.onandor.nesemu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun ListDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: @Composable () -> Unit
) {
    Box {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = null
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            items()
        }
    }
}