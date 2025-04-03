package com.onandor.nesemu.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.onandor.nesemu.data.NesEmuDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.onandor.nesemu.R
import com.onandor.nesemu.data.dao.LibraryEntryDao
import com.onandor.nesemu.data.dao.SaveStateDao
import com.onandor.nesemu.data.preferences.PreferencesSerializer
import com.onandor.nesemu.data.preferences.proto.Preferences
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Provides
    @Singleton
    fun provideNesGameDatabase(@ApplicationContext context: Context): NesEmuDatabase {
        return Room
            .databaseBuilder(
                context = context,
                klass = NesEmuDatabase::class.java,
                name = context.getString(R.string.app_database_name)
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideNesGameDao(database: NesEmuDatabase): LibraryEntryDao = database.libraryEntryDao()

    @Provides
    fun provideSaveStateDao(database: NesEmuDatabase): SaveStateDao = database.saveStateDao()
}

@Module
@InstallIn(SingletonComponent::class)
class PreferencesModule {

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
}