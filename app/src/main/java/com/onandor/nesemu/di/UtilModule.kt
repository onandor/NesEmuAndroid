package com.onandor.nesemu.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.onandor.nesemu.emulation.Emulator
import com.onandor.nesemu.input.NesInputManager
import com.onandor.nesemu.preferences.PreferencesSerializer
import com.onandor.nesemu.preferences.proto.Preferences
import com.onandor.nesemu.util.FileAccessor
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
class UtilModule {

    @Singleton
    @Provides
    fun provideGlobalLifecycleObserver(
        emulator: Emulator,
        inputManager: NesInputManager
    ): GlobalLifecycleObserver = GlobalLifecycleObserver(emulator, inputManager)

    @Singleton
    @Provides
    fun provideDataStore(@ApplicationContext context: Context) = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() }
        ),
        produceFile = { context.preferencesDataStoreFile("prefs.preferences_pb") }
    )

    @Singleton
    @Provides
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
        preferencesSerializer: PreferencesSerializer,
        @IODispatcher coroutineScope: CoroutineScope
    ): DataStore<Preferences> = DataStoreFactory.create(
        serializer = preferencesSerializer,
        scope = coroutineScope,
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { Preferences.getDefaultInstance() }
        ),
        produceFile = { context.dataStoreFile("proto_prefs.preferences_pb") }
    )

    @Singleton
    @Provides
    fun provideFileAccessor(
        @ApplicationContext context: Context
    ): FileAccessor = FileAccessor(context)
}