package com.onandor.nesemu.service

import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.repository.LibraryEntryRepository
import com.onandor.nesemu.data.repository.SaveStateRepository
import com.onandor.nesemu.data.entity.SaveState
import com.onandor.nesemu.data.entity.SaveStateType
import com.onandor.nesemu.di.IODispatcher
import com.onandor.nesemu.emulation.EmulationListener
import com.onandor.nesemu.emulation.Emulator
import com.onandor.nesemu.util.DocumentAccessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

enum class EmulationState {
    Running,
    Paused,
    Ready,
    Uninitialized
}

@Singleton
class EmulationService @Inject constructor(
    private val emulator: Emulator,
    private val libraryEntryRepository: LibraryEntryRepository,
    private val saveStateRepository: SaveStateRepository,
    private val documentAccessor: DocumentAccessor,
    @IODispatcher private val coroutineScope: CoroutineScope
) {

    private lateinit var loadedGame: LibraryEntry
    private var loadedSaveState: SaveState? = null

    var state: EmulationState = EmulationState.Uninitialized
        private set
    private val timeSource = TimeSource.Monotonic
    private var sessionPlaytime: Long = 0
    private var lastResumed: ValueTimeMark = timeSource.markNow()

    fun loadGame(game: LibraryEntry, saveState: SaveState? = null) {
        val rom = documentAccessor.readBytes(game.uri)
        loadedGame = game
        emulator.loadRom(rom)
        saveState?.let {
            loadedSaveState = it
            emulator.loadSaveState(it.nesState)
        }
        state = EmulationState.Ready
    }

    fun saveGame() {
        loadedSaveState = if (loadedSaveState != null) {
            loadedSaveState!!.copy(
                playtime = loadedSaveState!!.playtime + sessionPlaytime,
                nesState = emulator.createSaveState(),
                modificationDate = OffsetDateTime.now()
            )
        } else {
            SaveState(
                libraryEntryId = loadedGame.id,
                playtime = sessionPlaytime,
                modificationDate = OffsetDateTime.now(),
                nesState = emulator.createSaveState(),
                type = SaveStateType.Manual,
                romHash = loadedGame.romHash
            )
        }
    }

    fun resetAndStart() {
        if (state != EmulationState.Ready) {
            return
        }
        emulator.reset()
        emulator.start()
        state = EmulationState.Running
    }

    fun stop() {
        if (state == EmulationState.Uninitialized) {
            return
        }

        emulator.stop()

        coroutineScope.launch {
            saveStateRepository.upsertAutosave(
                libraryEntryId = loadedGame.id,
                sessionPlaytime = sessionPlaytime,
                nesState = emulator.createSaveState(),
                romHash = loadedGame.romHash
            )
        }

        loadedSaveState = null
        sessionPlaytime = 0
        state = EmulationState.Ready
    }

    fun resume() {
        if (state != EmulationState.Paused) {
            return
        }
        emulator.start()
        lastResumed = timeSource.markNow()
        state = EmulationState.Running
    }

    fun pause() {
        if (state != EmulationState.Running) {
            return
        }
        emulator.stop()
        sessionPlaytime += lastResumed.elapsedNow().inWholeSeconds
        state = EmulationState.Paused
    }

    fun reset() {
        emulator.stop()
        emulator.reset()
        emulator.start()
    }

    fun registerListener(listener: EmulationListener) {
        emulator.registerListener(listener)
    }

    fun unregisterListener(listener: EmulationListener) {
        emulator.unregisterListener(listener)
    }
}