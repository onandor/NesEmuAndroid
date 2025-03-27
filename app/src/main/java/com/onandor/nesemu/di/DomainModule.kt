package com.onandor.nesemu.di

import com.onandor.nesemu.data.repository.LibraryEntryRepository
import com.onandor.nesemu.service.LibraryService
import com.onandor.nesemu.preferences.PreferenceManager
import com.onandor.nesemu.util.DocumentAccessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DomainModule {

    @Provides
    @Singleton
    fun provideLibraryManager(
        @DefaultDispatcher coroutineScope: CoroutineScope,
        prefManager: PreferenceManager,
        documentAccessor: DocumentAccessor,
        libraryEntryRepository: LibraryEntryRepository
    ) = LibraryService(coroutineScope, prefManager, documentAccessor, libraryEntryRepository)
}