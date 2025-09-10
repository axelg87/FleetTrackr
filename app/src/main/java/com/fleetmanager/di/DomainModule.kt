package com.fleetmanager.di

import com.fleetmanager.data.repository.AuthRepositoryImpl
import com.fleetmanager.data.repository.FleetRepositoryImpl
import com.fleetmanager.data.repository.StorageRepositoryImpl
import com.fleetmanager.domain.repository.AuthRepository
import com.fleetmanager.domain.repository.FleetRepository
import com.fleetmanager.domain.repository.StorageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {
    
    @Binds
    @Singleton
    abstract fun bindFleetRepository(
        fleetRepositoryImpl: FleetRepositoryImpl
    ): FleetRepository
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindStorageRepository(
        storageRepositoryImpl: StorageRepositoryImpl
    ): StorageRepository
}