package com.onandor.nesemu.di

import com.onandor.nesemu.emulation.Emulator
import com.onandor.nesemu.emulation.nes.Nes
import com.onandor.nesemu.input.NesInputManager
import com.onandor.nesemu.util.FileAccessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class EmulationModule {

    @Singleton
    @Provides
    fun provideEmulator(
        inputManager: NesInputManager,
        fileAccessor: FileAccessor
    ): Emulator = Emulator(inputManager, fileAccessor)
}