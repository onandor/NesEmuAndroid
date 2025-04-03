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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.onandor.nesemu.domain.input.NesButtonState

@Composable
fun FaceButton(
    modifier: Modifier = Modifier,
    text: String,
    onStateChanged: (NesButtonState) -> Unit
) {
    var buttonState by remember { mutableStateOf(NesButtonState.Up) }

    val canvasModifier = Modifier
        .width(60.dp)
        .height(60.dp)
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    event.changes.forEach { change ->
                        if (change.changedToDown()) {
                            buttonState = NesButtonState.Down
                            onStateChanged(NesButtonState.Down)
                        } else if (change.changedToUp()) {
                            buttonState = NesButtonState.Up
                            onStateChanged(NesButtonState.Up)
                        }
                    }
                }
            }
        }

    Column {
        val density = LocalDensity.current
        val width = with(density) { 60.dp.toPx() }
        val height = with(density) { 60.dp.toPx() }

        Text(text = text)
        Canvas(modifier = canvasModifier) {
            val upBrush = Brush.radialGradient(
                0.80f to Color.Red,
                0.90f to Color(0xFFFF9A98),
                1.0f to Color.Gray
            )

            val downBrush = Brush.radialGradient(
                0.80f to Color(0xFF950606),
                1.0f to Color.Gray
            )

            drawCircle(
                brush = if (buttonState == NesButtonState.Down) downBrush else upBrush
            )
        }
    }
}