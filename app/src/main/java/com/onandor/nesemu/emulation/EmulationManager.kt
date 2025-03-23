package com.onandor.nesemu.emulation

import com.onandor.nesemu.data.entity.NesGame
import com.onandor.nesemu.data.repository.NesGameRepository
import com.onandor.nesemu.data.repository.SaveStateRepository
import com.onandor.nesemu.data.entity.SaveState
import com.onandor.nesemu.data.entity.SaveStateType
import com.onandor.nesemu.di.IODispatcher
import com.onandor.nesemu.emulation.nes.Cartridge
import com.onandor.nesemu.util.FileAccessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

private enum class EmulationState {
    Running,
    Paused,
    Ready,
    Uninitialized
}

class EmulationManager @Inject constructor(
    private val emulator: Emulator,
    private val nesGameRepository: NesGameRepository,
    private val saveStateRepository: SaveStateRepository,
    private val fileAccessor: FileAccessor,
    @IODispatcher private val coroutineScope: CoroutineScope
) {

    private lateinit var loadedGame: NesGame
    private var loadedSaveState: SaveState? = null

    private var state: EmulationState = EmulationState.Uninitialized
    private val timeSource = TimeSource.Monotonic
    private var sessionPlaytime: Long = 0
    private var lastResumed: ValueTimeMark = timeSource.markNow()

    fun load(fileUri: String) {
        val rom = fileAccessor.readBytes(fileUri)
        val fileName = fileAccessor.getFileName(fileUri)!!
        val hash = Cartridge.calculateRomHash(rom)

        val existingGame = runBlocking { nesGameRepository.findByRomHash(hash) }

        if (existingGame != null) {
            if (existingGame.fileUri != fileUri || existingGame.fileName != fileName) {
                loadedGame = existingGame.copy(
                    fileName = fileName,
                    fileUri = fileUri
                )
                coroutineScope.launch { nesGameRepository.upsert(loadedGame) }
            } else {
                loadedGame = existingGame
            }
        } else {
            loadedGame = NesGame(
                fileName = fileName,
                fileUri = fileUri,
                romHash = hash
            )
            coroutineScope.launch { nesGameRepository.upsert(loadedGame) }
        }

        emulator.loadRom(rom)
        state = EmulationState.Ready
    }

    fun load(game: NesGame, saveState: SaveState? = null) {
        val rom = fileAccessor.readBytes(game.fileUri)
        loadedGame = game
        emulator.loadRom(rom)
        saveState?.let {
            loadedSaveState = it
            emulator.loadSaveState(it.nesState)
        }
        state = EmulationState.Ready
    }

    fun save() {
        loadedSaveState = if (loadedSaveState != null) {
            loadedSaveState!!.copy(
                playtime = loadedSaveState!!.playtime + sessionPlaytime,
                nesState = emulator.createSaveState(),
                modificationDate = OffsetDateTime.now()
            )
        } else {
            SaveState(
                romHash = loadedGame.romHash,
                playtime = sessionPlaytime,
                modificationDate = OffsetDateTime.now(),
                nesState = emulator.createSaveState(),
                type = SaveStateType.Manual
            )
        }
    }

    fun start() {
        if (state != EmulationState.Ready) {
            return
        }
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
                romHash = loadedGame.romHash,
                sessionPlaytime = sessionPlaytime,
                nesState = emulator.createSaveState()
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
        // TODO
        lastResumed = timeSource.markNow()
        state = EmulationState.Running
    }

    fun pause() {
        if (state != EmulationState.Running) {
            return
        }
        // TODO
        sessionPlaytime += lastResumed.elapsedNow().inWholeSeconds
        state = EmulationState.Paused
    }

    fun reset() {
        emulator.reset()
    }
}