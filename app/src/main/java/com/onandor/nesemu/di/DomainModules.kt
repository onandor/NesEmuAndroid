package com.onandor.nesemu.di

import android.content.Context
import android.hardware.input.InputManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.onandor.nesemu.data.preferences.PreferenceManager
import com.onandor.nesemu.data.repository.CoverArtRepository
import com.onandor.nesemu.data.repository.LibraryEntryRepository
import com.onandor.nesemu.data.repository.SaveStateRepository
import com.onandor.nesemu.domain.service.CoverArtService
import com.onandor.nesemu.domain.service.EmulationService
import com.onandor.nesemu.domain.service.InputService
import com.onandor.nesemu.domain.service.LibraryService
import com.onandor.nesemu.domain.service.MainCoverArtService
import com.onandor.nesemu.domain.service.MainEmulationService
import com.onandor.nesemu.domain.service.MainInputService
import com.onandor.nesemu.domain.service.MainLibraryService
import com.onandor.nesemu.util.DocumentAccessor
import com.onandor.nesemu.util.GlobalLifecycleObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Singleton
    @Provides
    fun provideInputService(
        inputManager: InputManager,
        prefManager: PreferenceManager,
        @MainDispatcher mainScope: CoroutineScope,
        @IODispatcher ioScope: CoroutineScope,
        lifecycleObserver: GlobalLifecycleObserver
    ): InputService =
        MainInputService(
            inputManager = inputManager,
            prefManager = prefManager,
            mainScope = mainScope,
            ioScope = ioScope,
            lifecycleObserver = lifecycleObserver
        )

    @Provides
    @Singleton
    fun provideLibraryService(
        @DefaultDispatcher ioScope: CoroutineScope,
        prefManager: PreferenceManager,
        documentAccessor: DocumentAccessor,
        libraryEntryRepository: LibraryEntryRepository,
        coverArtService: CoverArtService
    ): LibraryService =
        MainLibraryService(
            ioScope = ioScope,
            prefManager = prefManager,
            documentAccessor = documentAccessor,
            libraryEntryRepository = libraryEntryRepository,
            coverArtService = coverArtService
        )

    @Provides
    @Singleton
    @JvmSuppressWildcards
    fun provideCoverArtService(
        @SteamGridDB httpClientFactory: (String) -> HttpClient,
        prefManager: PreferenceManager,
        @IODispatcher ioScope: CoroutineScope,
        libraryEntryRepository: LibraryEntryRepository,
        coverArtRepository: CoverArtRepository
    ) : CoverArtService =
        MainCoverArtService(
            httpClientFactory = httpClientFactory,
            prefManager = prefManager,
            ioScope = ioScope,
            libraryEntryRepository = libraryEntryRepository,
            coverArtRepository = coverArtRepository
        )

    @Provides
    @Singleton
    fun provideEmulationService(
        audioTrack: AudioTrack,
        saveStateRepository: SaveStateRepository,
        documentAccessor: DocumentAccessor,
        @DefaultDispatcher defaultScope: CoroutineScope,
        @IODispatcher ioScope: CoroutineScope,
        inputService: InputService
    ): EmulationService =
        MainEmulationService(
            audioTrack = audioTrack,
            inputService = inputService,
            saveStateRepository = saveStateRepository,
            documentAccessor = documentAccessor,
            defaultScope = defaultScope,
            ioScope = ioScope
        )
}

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideAudioTrack(@ApplicationContext context: Context) : AudioTrack {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val sampleRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE).toInt()
        val bufferSizeBytes = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT) * 2

        return AudioTrack.Builder()
            .setAudioFormat(AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build())
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            //.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferSizeBytes)
            .build()
    }
}