package com.onandor.nesemu.domain.service

import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import com.google.common.collect.HashBiMap
import com.onandor.nesemu.data.preferences.PreferenceManager
import com.onandor.nesemu.domain.input.ButtonMapping
import com.onandor.nesemu.domain.input.NesButton
import com.onandor.nesemu.domain.input.NesInputDeviceType
import com.onandor.nesemu.util.GlobalLifecycleObserver
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.junit4.MockKRule
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class MainInputServiceTest {

    val mockInputManager = mockk<InputManager>()
    val mockPrefManager = mockk<PreferenceManager> {
        coEvery { updateInputDevices(any(), any()) } just Runs
        coEvery { updateButtonMappings(any()) } just Runs
        coEvery { getController1Device() } returns null
        coEvery { getController2Device() } returns null
        coEvery { getButtonMappings() } returns emptyMap()
    }
    val testScope = CoroutineScope(Dispatchers.Unconfined)
    val mockLifecycleObserver = mockk<GlobalLifecycleObserver> {
        every { events } returns MutableSharedFlow()
    }

    @Test
    fun mainInputServiceTest_ChangePlayer2InputDevice_Player1DeviceCleared() {
        val inputService = MainInputService(mockInputManager, mockPrefManager, testScope,
            testScope, mockLifecycleObserver)

        Thread.sleep(100)

        assertEquals(MainInputService.VIRTUAL_CONTROLLER, inputService.state.value.player1InputDevice)
        assertEquals(null, inputService.state.value.player2InputDevice)

        inputService.changeInputDevice(InputService.PLAYER_2, MainInputService.VIRTUAL_CONTROLLER)

        assertEquals(null, inputService.state.value.player1InputDevice)
        assertEquals(MainInputService.VIRTUAL_CONTROLLER, inputService.state.value.player2InputDevice)
        coVerify(exactly = 2) { mockPrefManager.updateInputDevices(any(), any()) }
    }

    @Test
    fun mainInputServiceTest_ChangeButtonMapping_ItGetsSaved() {
        val inputService = MainInputService(mockInputManager, mockPrefManager, testScope,
            testScope, mockLifecycleObserver)

        Thread.sleep(100)

        val key = InputService.ButtonMapKey(InputService.PLAYER_1, NesInputDeviceType.Controller)
        assertEquals(initialControllerButtonMap, inputService.state.value.buttonMappings[key])
        assertEquals(
            expected = NesButton.B,
            actual = inputService.state.value.buttonMappings[key]?.get(KeyEvent.KEYCODE_BUTTON_A)
        )

        inputService.changeButtonMapping(
            keyCode = KeyEvent.KEYCODE_BUTTON_A,
            button = NesButton.Start,
            playerId = InputService.PLAYER_1,
            deviceType = NesInputDeviceType.Controller
        )

        assertEquals(
            expected = NesButton.Start,
            actual = inputService.state.value.buttonMappings[key]?.get(KeyEvent.KEYCODE_BUTTON_A)
        )
        coVerify(exactly = 1) { mockPrefManager.updateButtonMappings(any()) }
    }

    companion object {

        @JvmStatic
        @BeforeClass
        fun setupClass() {
            mockkStatic(InputDevice::class)
            every { InputDevice.getDeviceIds() } returns IntArray(0)
        }

        @JvmStatic
        @AfterClass
        fun teardownClass() {
            unmockkStatic(InputDevice::class)
        }

        private val initialControllerButtonMap =
            HashBiMap.create(ButtonMapping.DEFAULT_CONTROLLER_BUTTON_MAP)
    }
}