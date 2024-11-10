package com.onandor.nesemu.ui.components.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OptionButton(
    modifier: Modifier = Modifier,
    text: String,
    onStateChanged: (ButtonState) -> Unit
) {
    var buttonState by remember { mutableStateOf(ButtonState.UP) }

    val canvasModifier = Modifier
        .width(60.dp)
        .height(20.dp)
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    event.changes.forEach { change ->
                        if (change.changedToDown()) {
                            buttonState = ButtonState.DOWN
                            onStateChanged(ButtonState.DOWN)
                        } else if (change.changedToUp()) {
                            buttonState = ButtonState.UP
                            onStateChanged(ButtonState.UP)
                        }
                    }
                }
            }
        }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val density = LocalDensity.current
        val width = with(density) { 60.dp.toPx() }
        val height = with(density) { 20.dp.toPx() }

        Text(text = text)
        Canvas(modifier = canvasModifier) {
            drawRoundRect(
                topLeft = Offset(0f, 0f),
                size = Size(width, height),
                cornerRadius = CornerRadius(x = 25f, y = 25f),
                color = if (buttonState == ButtonState.DOWN) Color.Gray else Color.DarkGray
            )
        }
    }
}