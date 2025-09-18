package com.fleetmanager.di

import com.fleetmanager.ui.navigation.NavigationState
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency Injection Module for Navigation State
 * Provides the NavigationState singleton for enterprise-grade navigation
 */
@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {
    
    @Provides
    @Singleton
    fun provideNavigationState(): NavigationState {
        return NavigationState()
    }
}