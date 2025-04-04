package com.onandor.nesemu.di

import android.content.Context
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
class UtilModule {

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
}