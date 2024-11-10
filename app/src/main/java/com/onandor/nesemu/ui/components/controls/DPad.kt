package com.onandor.nesemu.ui.components.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun DPad(
    modifier: Modifier = Modifier,
    onStateChanged: (Map<Button, ButtonState>) -> Unit
) {
    var buttonStates = remember {
        mapOf(
            Button.DPAD_RIGHT to ButtonState.UP,
            Button.DPAD_LEFT to ButtonState.UP,
            Button.DPAD_UP to ButtonState.UP,
            Button.DPAD_DOWN to ButtonState.UP
        )
    }
    val sizeDp = 130.dp
    val sizePx = with(LocalDensity.current) { sizeDp.toPx() }

    val canvasModifier = Modifier
        .size(130.dp)
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.first()
                    buttonStates = evaluateButtonStates(sizePx / 2, change)
                    onStateChanged(buttonStates)
                }
            }
        }

    Canvas(modifier = canvasModifier) {
        val innerXStart = size.width / 2 - 50f
        val innerXEnd = size.width / 2 + 50f
        val innerYTop = size.height / 2 - 50f
        val innerYBottom = size.height / 2 + 50f

        val buttonOutline = Path().apply {
            moveTo(innerXStart, 0f)
            lineTo(innerXEnd, 0f)
            lineTo(innerXEnd, innerYTop)
            lineTo(size.width, innerYTop)
            lineTo(size.width, innerYBottom)
            lineTo(innerXEnd, innerYBottom)
            lineTo(innerXEnd, size.height)
            lineTo(innerXStart, size.height)
            lineTo(innerXStart, innerYBottom)
            lineTo(0f, innerYBottom)
            lineTo(0f, innerYTop)
            lineTo(innerXStart, innerYTop)
            lineTo(innerXStart, 0f)
            close()
        }

        drawPath(
            path = buttonOutline,
            color = Color.DarkGray,
            style = Fill
        )

        drawPath(
            buttonOutline,
            color = Color.DarkGray,
            style = Stroke(
                width = 8f,
                pathEffect = PathEffect.cornerPathEffect(5.dp.toPx())
            )
        )
    }
}

private fun evaluateButtonStates(center: Float, change: PointerInputChange): Map<Button, ButtonState> {
    val x = change.position.x
    val y = change.position.y
    val touchXStart = center - 65f
    val touchXEnd = center + 65f
    val touchYTop = center - 65f
    val touchYBottom = center + 65f

    val buttonStates = mutableMapOf(
        Button.DPAD_RIGHT to ButtonState.UP,
        Button.DPAD_LEFT to ButtonState.UP,
        Button.DPAD_UP to ButtonState.UP,
        Button.DPAD_DOWN to ButtonState.UP
    )

    if (change.changedToUp()) {
        return buttonStates
    }

    if (x < touchXStart) {
        buttonStates[Button.DPAD_LEFT] = ButtonState.DOWN
    } else if (x > touchXEnd) {
        buttonStates[Button.DPAD_RIGHT] = ButtonState.DOWN
    }
    if (y < touchYTop) {
        buttonStates[Button.DPAD_UP] = ButtonState.DOWN
    } else if (y > touchYBottom) {
        buttonStates[Button.DPAD_DOWN] = ButtonState.DOWN
    }

    return buttonStates
}