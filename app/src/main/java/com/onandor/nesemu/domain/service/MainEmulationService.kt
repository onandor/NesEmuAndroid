package com.onandor.nesemu.domain.service

import android.graphics.Bitmap
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.repository.SaveStateRepository
import com.onandor.nesemu.data.entity.SaveState
import com.onandor.nesemu.di.IODispatcher
import com.onandor.nesemu.domain.emulation.EmulationListener
import com.onandor.nesemu.domain.emulation.Emulator
import com.onandor.nesemu.ui.components.game.NesRenderer
import com.onandor.nesemu.util.DocumentAccessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
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
class MainEmulationService @Inject constructor(
    private val emulator: Emulator,
    private val saveStateRepository: SaveStateRepository,
    private val documentAccessor: DocumentAccessor,
    @IODispatcher private val ioScope: CoroutineScope
) : EmulationService {

    override var loadedGame: LibraryEntry? = null
        private set
    override var state: EmulationState = EmulationState.Uninitialized
        private set

    private val timeSource = TimeSource.Monotonic
    private var playtime: Long = 0
    private var lastResumed: ValueTimeMark = timeSource.markNow()

    override var renderer: NesRenderer? = null

    override fun loadGame(game: LibraryEntry, saveState: SaveState?) {
        val rom = documentAccessor.readBytes(game.uri)
        loadedGame = game
        emulator.loadRom(rom)
        emulator.reset()
        saveState?.let {
            emulator.loadSaveState(it.nesState)
            playtime = it.playtime
        }
        state = EmulationState.Ready
    }

    override fun loadSave(saveState: SaveState) {
        if (state == EmulationState.Uninitialized || loadedGame == null) {
            return
        }

        val isRunning = state == EmulationState.Running
        if (isRunning) {
            pause()
        }

        emulator.reset()
        //emulator.loadSaveState(saveState.nesState)
        playtime = saveState.playtime

        if (isRunning) {
            start()
        }
    }

    override fun saveGame(slot: Int, blocking: Boolean) {
        if (state == EmulationState.Uninitialized ||
            state == EmulationState.Ready ||
            loadedGame == null) {
            return
        }

        val isRunning = state == EmulationState.Running
        if (isRunning) {
            pause()
        }

//        val saveState = SaveState(
//            playtime = playtime,
//            modificationDate = OffsetDateTime.now(),
//            nesState = emulator.createSaveState(),
//            romHash = loadedGame!!.romHash,
//            slot = slot,
//            preview = createPreview()
//        )
//        if (blocking) {
//            runBlocking { saveStateRepository.upsert(saveState) }
//        } else {
//            ioScope.launch { saveStateRepository.upsert(saveState) }
//        }

        if (isRunning) {
            start()
        }
    }

    override fun start() {
        if (state != EmulationState.Ready && state != EmulationState.Paused) {
            return
        }
        emulator.initAudioPlayer()
        emulator.start()
        lastResumed = timeSource.markNow()
        state = EmulationState.Running
    }

    override fun stop(immediate: Boolean) {
        if (state == EmulationState.Uninitialized || state == EmulationState.Ready) {
            return
        }
        if (state == EmulationState.Running) {
            pause()
        } else {
            emulator.stop()
        }
        emulator.destroyAudioPlayer()
        //saveGame(0, immediate)

        state = EmulationState.Ready
    }

    override fun pause() {
        if (state != EmulationState.Running) {
            return
        }
        emulator.stop()
        playtime += lastResumed.elapsedNow().inWholeSeconds
        state = EmulationState.Paused
    }

    override fun reset() {
        emulator.stop()
        emulator.reset()
        emulator.start()
    }

    override fun registerListener(listener: EmulationListener) {
        emulator.registerListener(listener)
    }

    override fun unregisterListener(listener: EmulationListener) {
        emulator.unregisterListener(listener)
    }

    private fun createPreview(): ByteArray {
        renderer?.let { renderer ->
            return ByteArrayOutputStream().use { out ->
                renderer.captureFrame().compress(Bitmap.CompressFormat.JPEG, 80, out)
                out.toByteArray()
            }
        }
        return ByteArray(0)
    }
}