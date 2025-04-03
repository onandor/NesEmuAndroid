package com.onandor.nesemu.di

import android.content.Context
import android.hardware.input.InputManager
import com.onandor.nesemu.data.preferences.PreferenceManager
import com.onandor.nesemu.data.repository.LibraryEntryRepository
import com.onandor.nesemu.domain.emulation.Emulator
import com.onandor.nesemu.domain.service.InputService
import com.onandor.nesemu.domain.service.LibraryService
import com.onandor.nesemu.util.DocumentAccessor
import com.onandor.nesemu.util.GlobalLifecycleObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class EmulationModule {

    @Singleton
    @Provides
    fun provideEmulator(
        @DefaultDispatcher coroutineScope: CoroutineScope,
        inputService: InputService
    ): Emulator = Emulator(coroutineScope, inputService)
}

@Module
@InstallIn(SingletonComponent::class)
class ServiceModule {

    @Singleton
    @Provides
    fun provideInputService(
        @ApplicationContext context: Context,
        prefManager: PreferenceManager,
        @MainDispatcher mainScope: CoroutineScope,
        @IODispatcher ioScope: CoroutineScope,
        lifecycleObserver: GlobalLifecycleObserver
    ): InputService =
        InputService(
            inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager,
            prefManager = prefManager,
            mainScope = mainScope,
            ioScope = ioScope,
            lifecycleObserver = lifecycleObserver
        )

    @Provides
    @Singleton
    fun provideLibraryService(
        @DefaultDispatcher coroutineScope: CoroutineScope,
        prefManager: PreferenceManager,
        documentAccessor: DocumentAccessor,
        libraryEntryRepository: LibraryEntryRepository
    ) = LibraryService(coroutineScope, prefManager, documentAccessor, libraryEntryRepository)
}