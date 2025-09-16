package com.fleetmanager.di

import com.fleetmanager.data.preferences.ReportPreferencesDataStore
import com.fleetmanager.data.preferences.SettingsPreferencesDataStore
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    // ReportPreferencesDataStore is already injectable via @Inject constructor
    // SettingsPreferencesDataStore is already injectable via @Inject constructor
    // No additional bindings needed since they have @Singleton annotation
}