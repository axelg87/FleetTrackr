package com.fleetmanager.di

import android.content.Context
import androidx.room.Room
import com.fleetmanager.data.local.FleetManagerDatabase
import com.fleetmanager.data.local.dao.DailyEntryDao
import com.fleetmanager.data.local.dao.DriverDao
import com.fleetmanager.data.local.dao.VehicleDao
import com.fleetmanager.data.local.dao.ExpenseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideFleetManagerDatabase(
        @ApplicationContext context: Context
    ): FleetManagerDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            FleetManagerDatabase::class.java,
            FleetManagerDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }
    
    @Provides
    fun provideDailyEntryDao(database: FleetManagerDatabase): DailyEntryDao = 
        database.dailyEntryDao()
    
    @Provides
    fun provideDriverDao(database: FleetManagerDatabase): DriverDao = 
        database.driverDao()
    
    @Provides
    fun provideVehicleDao(database: FleetManagerDatabase): VehicleDao = 
        database.vehicleDao()
    
    @Provides
    fun provideExpenseDao(database: FleetManagerDatabase): ExpenseDao = 
        database.expenseDao()
}