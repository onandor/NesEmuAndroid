package com.onandor.nesemu.di

import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.navigation.MainNavigationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {

    @Singleton
    @Provides
    fun provideNavigationManager(): NavigationManager = MainNavigationManager()
}