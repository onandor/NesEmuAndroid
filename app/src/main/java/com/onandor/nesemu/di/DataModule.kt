package com.onandor.nesemu.di

import android.content.Context
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

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Provides
    @Singleton
    fun provideNesGameDatabase(@ApplicationContext context: Context): NesEmuDatabase {
        return Room.databaseBuilder(
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

//@Module
//@InstallIn(SingletonComponent::class)
//class RepositoryModule {
//
//    @Provides
//    @Singleton
//    fun provideNesGameRepository()
//}