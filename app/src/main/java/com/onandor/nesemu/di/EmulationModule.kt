package com.onandor.nesemu.di

import com.onandor.nesemu.emulation.Emulator
import com.onandor.nesemu.input.NesInputManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
        inputManager: NesInputManager
    ): Emulator = Emulator(coroutineScope, inputManager)
}