package com.onandor.nesemu.di

import android.content.Context
import android.hardware.input.InputManager
import com.onandor.nesemu.input.NesInputManager
import com.onandor.nesemu.preferences.PreferenceManager
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
class InputModule {

    @Singleton
    @Provides
    fun provideInputManager(
        @ApplicationContext context: Context,
        prefManager: PreferenceManager,
        @MainDispatcher mainScope: CoroutineScope,
        @IODispatcher ioScope: CoroutineScope,
        lifecycleObserver: GlobalLifecycleObserver
    ): NesInputManager =
        NesInputManager(
            inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager,
            prefManager = prefManager,
            mainScope = mainScope,
            ioScope = ioScope,
            lifecycleObserver = lifecycleObserver
        )
}