package com.onandor.nesemu.ui.components.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
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
    var buttonStates by remember {
        mutableStateOf(
            mapOf(
                Button.DPAD_RIGHT to ButtonState.UP,
                Button.DPAD_LEFT to ButtonState.UP,
                Button.DPAD_UP to ButtonState.UP,
                Button.DPAD_DOWN to ButtonState.UP
            )
        )
    }

    val sizeDp = 140.dp
    val sizePx = with(LocalDensity.current) { sizeDp.toPx() }

    val canvasModifier = modifier
        .size(sizeDp)
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
        val center = size.width / 2
        val innerXStart = center - 50f
        val innerXEnd = center + 50f
        val innerYTop = center - 50f
        val innerYBottom = center + 50f
        val edgePadding = 15f

        val dpad = Path().apply {
            moveTo(innerXStart, edgePadding)
            lineTo(innerXEnd, edgePadding)
            lineTo(innerXEnd, innerYTop)
            lineTo(size.width - edgePadding, innerYTop)
            lineTo(size.width - edgePadding, innerYBottom)
            lineTo(innerXEnd, innerYBottom)
            lineTo(innerXEnd, size.height - edgePadding)
            lineTo(innerXStart, size.height - edgePadding)
            lineTo(innerXStart, innerYBottom)
            lineTo(edgePadding, innerYBottom)
            lineTo(edgePadding, innerYTop)
            lineTo(innerXStart, innerYTop)
            lineTo(innerXStart, edgePadding)
            close()
        }

        val top = Path().apply {
            moveTo(innerXStart, edgePadding)
            lineTo(innerXEnd, edgePadding)
            lineTo(innerXEnd, innerYTop)
            lineTo(center, center)
            lineTo(innerXStart, innerYTop)
            lineTo(innerXStart, edgePadding)
            close()
        }

        val right = Path().apply {
            moveTo(innerXEnd, innerYTop)
            lineTo(size.width - edgePadding, innerYTop)
            lineTo(size.width - edgePadding, innerYBottom)
            lineTo(innerXEnd, innerYBottom)
            lineTo(center, center)
            lineTo(innerXEnd, innerYTop)
            close()
        }

        val bottom = Path().apply {
            moveTo(innerXEnd, innerYBottom)
            lineTo(innerXEnd, size.height - edgePadding)
            lineTo(innerXStart, size.height - edgePadding)
            lineTo(innerXStart, innerYBottom)
            lineTo(center, center)
            lineTo(innerXEnd, innerYBottom)
            close()
        }

        val left = Path().apply {
            moveTo(innerXStart, innerYBottom)
            lineTo(edgePadding, innerYBottom)
            lineTo(edgePadding, innerYTop)
            lineTo(innerXStart, innerYTop)
            lineTo(center, center)
            lineTo(innerXStart, innerYBottom)
            close()
        }

        val highlightedParts = Path()
        if (ButtonState.DOWN == buttonStates[Button.DPAD_UP]) {
            highlightedParts.addPath(top)
        }
        if (ButtonState.DOWN == buttonStates[Button.DPAD_RIGHT]) {
            highlightedParts.addPath(right)
        }
        if (ButtonState.DOWN == buttonStates[Button.DPAD_DOWN]!!) {
            highlightedParts.addPath(bottom)
        }
        if (ButtonState.DOWN == buttonStates[Button.DPAD_LEFT]!!) {
            highlightedParts.addPath(left)
        }

        // Light gray outline
        drawPath(
            path = dpad,
            color = Color.LightGray,
            style = Stroke(
                width = 30f,
                pathEffect = PathEffect.cornerPathEffect(5.dp.toPx())
            )
        )

        // Base button
        drawPath(
            path = dpad,
            color = Color.DarkGray,
            style = Fill
        )

        // Pressed-highlighted parts
        drawPath(
            path = highlightedParts,
            color = Color.Gray,
            style = Fill
        )

        // Rounded corners
        drawPath(
            dpad,
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
    val touchXStart = center - 60f
    val touchXEnd = center + 60f
    val touchYTop = center - 60f
    val touchYBottom = center + 60f

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