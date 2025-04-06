package com.onandor.nesemu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun TitleDialog(
    modifier: Modifier = Modifier,
    text: String,
    onDismissRequest: () -> Unit,
    content: @Composable() () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        val focusRequester = remember { FocusRequester() }
        Card(
            modifier = modifier
                .focusRequester(focusRequester)
                .focusable(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.padding(bottom = 20.dp),
                        text = text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                content()
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Requesting focus so the escape key event can be caught in the pause menu
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}