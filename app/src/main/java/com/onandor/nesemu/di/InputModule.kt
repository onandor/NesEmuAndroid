package com.onandor.nesemu.di

import android.content.Context
import android.hardware.input.InputManager
import com.onandor.nesemu.input.NesInputManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class InputModule {

    @Singleton
    @Provides
    fun provideInputManager(@ApplicationContext context: Context): NesInputManager =
        NesInputManager(context.getSystemService(Context.INPUT_SERVICE) as InputManager)
}