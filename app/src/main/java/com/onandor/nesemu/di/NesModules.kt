package com.onandor.nesemu.di

import com.onandor.nesemu.nes.Nes
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NesModules {

    @Singleton
    @Provides
    fun provideNes(): Nes = Nes()
}