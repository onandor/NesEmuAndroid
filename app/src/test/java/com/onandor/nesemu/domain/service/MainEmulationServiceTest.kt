package com.onandor.nesemu.domain.service

import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.entity.SaveState
import com.onandor.nesemu.data.repository.SaveStateRepository
import com.onandor.nesemu.domain.emulation.Emulator
import com.onandor.nesemu.util.DocumentAccessor
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class MainEmulationServiceTest {

    val mockEmulator = mockk<Emulator>(relaxed = true)
    val mockSaveStateRepository = mockk<SaveStateRepository>(relaxed = true)
    val mockDocumentAccessor = mockk<DocumentAccessor> {
        every { readBytes(any()) } returns ByteArray(0)
    }
    val testScope = CoroutineScope(Dispatchers.Unconfined)
    val mockSaveState = mockk<SaveState> {
        every { nesState } returns mockk {
            every { playtime } returns 0L
        }
    }

    lateinit var emulationService: MainEmulationService

    @Before
    fun setup() {
        emulationService = MainEmulationService(mockEmulator, mockSaveStateRepository,
            mockDocumentAccessor, testScope)
    }

    @Test
    fun emulationService_LoadGame_StateChangesToReady() {
        emulationService.loadGame(testGame)

        assertEquals(EmulationState.Ready, emulationService.state)
        verify(exactly = 1) { mockEmulator.loadRom(any()) }
        verify(exactly = 1) { mockEmulator.reset() }
    }

    @Test
    fun emulationService_LoadSaveWhileRunning_SaveLoadedAndEmulatorRestarted() {
        emulationService.loadGame(testGame)
        emulationService.start()

        assertEquals(EmulationState.Running, emulationService.state)

        emulationService.loadSave(mockSaveState)

        assertEquals(EmulationState.Running, emulationService.state)
        verify {
            mockEmulator.reset()
            mockEmulator.loadSaveState(any())
        }
    }

    @Test
    fun emulationService_SaveGame_GameSaved() {
        emulationService.loadGame(testGame)
        emulationService.start()

        assertEquals(EmulationState.Running, emulationService.state)

        emulationService.saveGame(0, true)

        assertEquals(EmulationState.Running, emulationService.state)
        coVerify {
            mockSaveStateRepository.upsert(any())
        }
    }

    companion object {
        private val testGame = LibraryEntry(
            romHash = "romHash",
            name = "testGame",
            uri = "",
            isDirectory = false
        )
    }
}