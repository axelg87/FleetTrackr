package com.fleetmanager.di

import com.fleetmanager.data.preferences.ReportPreferencesDataStore
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    // ReportPreferencesDataStore is already injectable via @Inject constructor
    // No additional bindings needed since it has @Singleton annotation
}