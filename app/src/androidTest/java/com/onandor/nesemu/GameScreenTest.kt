package com.onandor.nesemu

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.onandor.nesemu.domain.input.NesButton
import com.onandor.nesemu.domain.input.NesButtonState
import com.onandor.nesemu.ui.screens.VerticalControls
import com.onandor.nesemu.viewmodels.GameViewModel
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class GameScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val buttonStateMap = mutableMapOf<NesButton, NesButtonState>(
        NesButton.DPadRight to NesButtonState.Up,
        NesButton.DPadLeft to NesButtonState.Up,
        NesButton.DPadDown to NesButtonState.Up,
        NesButton.DPadUp to NesButtonState.Up,
        NesButton.Start to NesButtonState.Up,
        NesButton.Select to NesButtonState.Up,
        NesButton.B to NesButtonState.Up,
        NesButton.A to NesButtonState.Up
    )

    @Test
    fun gameScreenTest_TestControls() {
        composeTestRule.setContent {
            VerticalControls { event ->
                if (event is GameViewModel.Event.OnButtonStateChanged) {
                    buttonStateMap[event.button] = event.state
                } else if (event is GameViewModel.Event.OnDpadStateChanged) {
                    buttonStateMap.putAll(event.buttonStates)
                }
            }
        }

        composeTestRule.onNodeWithTag("A").performTouchInput { down(0, center) }
        composeTestRule.onNodeWithTag("B").performTouchInput { down(1, center) }
        assertEquals(NesButtonState.Down, buttonStateMap[NesButton.A])
        assertEquals(NesButtonState.Down, buttonStateMap[NesButton.B])

        composeTestRule.onNodeWithTag("A").performTouchInput { up(0) }
        assertEquals(NesButtonState.Up, buttonStateMap[NesButton.A])
        assertEquals(NesButtonState.Down, buttonStateMap[NesButton.B])

        composeTestRule.onNodeWithTag("DPad").performTouchInput { down(0, centerLeft) }
        assertEquals(NesButtonState.Down, buttonStateMap[NesButton.DPadLeft])
        composeTestRule.onNodeWithTag("DPad").performTouchInput { up(0) }

        composeTestRule.onNodeWithTag("DPad").performTouchInput { down(0, bottomRight) }
        assertEquals(NesButtonState.Down, buttonStateMap[NesButton.DPadRight])
        assertEquals(NesButtonState.Down, buttonStateMap[NesButton.DPadDown])
        assertEquals(NesButtonState.Up, buttonStateMap[NesButton.DPadLeft])
    }
}