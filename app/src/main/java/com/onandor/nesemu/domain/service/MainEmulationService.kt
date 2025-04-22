package com.onandor.nesemu.domain.service

import android.graphics.Bitmap
import android.media.AudioTrack
import android.util.Log
import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.repository.SaveStateRepository
import com.onandor.nesemu.data.entity.SaveState
import com.onandor.nesemu.di.DefaultDispatcher
import com.onandor.nesemu.di.IODispatcher
import com.onandor.nesemu.domain.emulation.nes.Cartridge
import com.onandor.nesemu.domain.emulation.nes.DebugFeature
import com.onandor.nesemu.domain.emulation.nes.Nes
import com.onandor.nesemu.ui.components.game.NesRenderer
import com.onandor.nesemu.util.DocumentAccessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.time.OffsetDateTime
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
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
    private val audioTrack: AudioTrack,
    inputService: InputService,
    private val saveStateRepository: SaveStateRepository,
    private val documentAccessor: DocumentAccessor,
    @DefaultDispatcher private val defaultScope: CoroutineScope,
    @IODispatcher private val ioScope: CoroutineScope
) : EmulationService {

    private val nes = Nes(
        onPollController1 = { inputService.getButtonStates(InputService.PLAYER_1) },
        onPollController2 = { inputService.getButtonStates(InputService.PLAYER_2) }
    )
    private lateinit var cartridge: Cartridge
    private var emulationRunning: Boolean = false
    private var stopLatch = CountDownLatch(1)

    private val _frames = MutableSharedFlow<Nes.Frame>(
        extraBufferCapacity = 2,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val frames = _frames.asSharedFlow()

    override var renderer: NesRenderer? = null
    override var loadedGame: LibraryEntry? = null
        private set
    override var state: EmulationState = EmulationState.Uninitialized
        private set

    private var emulatorJob: Job? = null
    private val timeSource = TimeSource.Monotonic
    private var playtime: Long = 0
    private var lastResumed: ValueTimeMark = timeSource.markNow()

    init {
        nes.apu.setSampleRate(audioTrack.sampleRate)
    }

    override fun loadGame(game: LibraryEntry, saveState: SaveState?) {
        loadedGame = game

        val rom = documentAccessor.readBytes(game.uri)
        cartridge = Cartridge()
        cartridge.parseRom(rom)
        nes.insertCartridge(cartridge)
        nes.reset()

        saveState?.let {
            nes.loadState(it.nesState)
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

        nes.reset()
        nes.loadState(saveState.nesState)
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

        val saveState = SaveState(
            playtime = playtime,
            modificationDate = OffsetDateTime.now(),
            nesState = nes.captureState(),
            romHash = loadedGame!!.romHash,
            slot = slot,
            preview = createPreview()
        )
        if (blocking) {
            runBlocking { saveStateRepository.upsert(saveState) }
        } else {
            ioScope.launch { saveStateRepository.upsert(saveState) }
        }

        if (isRunning) {
            start()
        }
    }

    override fun start() {
        if (state != EmulationState.Ready && state != EmulationState.Paused) {
            return
        }

        startAudioPlayback()
        emulatorJob = defaultScope.launch {
            Thread.currentThread().priority = Thread.MAX_PRIORITY
            try {
                runEmulation()
            } catch (e: Exception) {
                Log.e(TAG, e.localizedMessage, e)
                state = EmulationState.Ready
            }
        }

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
            stopAudioPlayback()
            stopEmulation()
        }
        saveGame(0, immediate)

        state = EmulationState.Ready
    }

    override fun pause() {
        if (state != EmulationState.Running) {
            return
        }

        stopAudioPlayback()
        stopEmulation()

        playtime += lastResumed.elapsedNow().inWholeSeconds
        state = EmulationState.Paused
    }

    override fun reset() {
        stopAudioPlayback()
        stopEmulation()
        nes.reset()
    }

    private fun runEmulation() {
        val timeSource = TimeSource.Monotonic
        var fpsMeasureStart = timeSource.markNow()
        var numFrames = 0
        emulationRunning = true

        val targetFrameTime = (1_000_000 / 60).microseconds

        while (emulationRunning) {
            val frameStart = timeSource.markNow()

            val frame = nes.generateFrame()
            val audioSamples = nes.drainAudioBuffer()

            _frames.tryEmit(frame)
            audioTrack.write(audioSamples, 0, audioSamples.size, AudioTrack.WRITE_NON_BLOCKING)

            val frameEnd = timeSource.markNow()
            val targetFrameEnd = frameStart + targetFrameTime

            while (targetFrameEnd.hasNotPassedNow()) {
                // Busy waiting
            }

//            val sleepMicros = 1_000_000 / 60 - (frameEnd - frameStart).inWholeMicroseconds
//            if (sleepMicros > 0) {
//                val millis = sleepMicros / 1000
//                val nanos = ((sleepMicros % 1_000) * 1_000).toInt()
//                Thread.sleep(millis, nanos)
//            }

            numFrames += 1

            if ((frameEnd - fpsMeasureStart).inWholeMilliseconds >= 3000) {
                val fps = numFrames / 3f
                numFrames = 0
                fpsMeasureStart = timeSource.markNow()
                Log.i(TAG, "FPS: $fps")
            }
        }

        stopLatch.countDown()
    }

    private fun stopEmulation() {
        if (!emulationRunning) {
            return
        }

        emulationRunning = false
        stopLatch.await()
        stopLatch = CountDownLatch(1)

        emulatorJob?.cancel()
        runBlocking { emulatorJob?.join() }
        emulatorJob = null
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

    private fun startAudioPlayback() {
        if (audioTrack.playState != AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.play()
        }
    }

    private fun stopAudioPlayback() {
        if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.pause()
            audioTrack.flush()
        }
    }

    override fun setDebugFeatureBool(feature: DebugFeature, value: Boolean) {
        nes.setDebugFeatureBool(feature, value)
    }

    override fun setDebugFeatureInt(feature: DebugFeature, value: Int) {
        nes.setDebugFeatureInt(feature, value)
    }

    companion object {
        private const val TAG = "MainEmulationService"
    }
}