package com.onandor.nesemu.di

import com.onandor.nesemu.emulation.Emulator
import com.onandor.nesemu.input.NesInputManager
import com.onandor.nesemu.util.GlobalLifecycleObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class UtilModule {

    @Singleton
    @Provides
    fun provideGlobalLifecycleObserver(
        emulator: Emulator,
        inputManager: NesInputManager
    ): GlobalLifecycleObserver = GlobalLifecycleObserver(emulator, inputManager)
}