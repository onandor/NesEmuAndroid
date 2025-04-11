package com.onandor.nesemu.di

import android.content.Context
import android.hardware.input.InputManager
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
object UtilModule {

    @Singleton
    @Provides
    fun provideGlobalLifecycleObserver(
        @DefaultDispatcher defaultScope: CoroutineScope
    ): GlobalLifecycleObserver = GlobalLifecycleObserver(defaultScope)

    @Singleton
    @Provides
    fun provideFileAccessor(
        @ApplicationContext context: Context
    ): DocumentAccessor = DocumentAccessor(context)

    @Provides
    fun provideInputManager(
        @ApplicationContext context: Context
    ): InputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
}