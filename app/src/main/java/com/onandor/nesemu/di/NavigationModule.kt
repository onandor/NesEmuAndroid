package com.onandor.nesemu.di

import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.navigation.NavigationManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NavigationModule {

    @Singleton
    @Provides
    fun provideNavigationManager(): NavigationManager = NavigationManagerImpl()
}