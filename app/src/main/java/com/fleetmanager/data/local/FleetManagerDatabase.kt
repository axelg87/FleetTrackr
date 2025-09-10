package com.fleetmanager.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.fleetmanager.data.local.dao.DailyEntryDao
import com.fleetmanager.data.local.dao.DriverDao
import com.fleetmanager.data.local.dao.VehicleDao
import com.fleetmanager.data.dto.DailyEntryDto
import com.fleetmanager.data.dto.DriverDto
import com.fleetmanager.data.dto.VehicleDto

@Database(
    entities = [DailyEntryDto::class, DriverDto::class, VehicleDto::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FleetManagerDatabase : RoomDatabase() {
    
    abstract fun dailyEntryDao(): DailyEntryDao
    abstract fun driverDao(): DriverDao
    abstract fun vehicleDao(): VehicleDao
    
    companion object {
        const val DATABASE_NAME = "fleet_manager_database"
        
        @Volatile
        private var INSTANCE: FleetManagerDatabase? = null
        
        fun getInstance(context: Context): FleetManagerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FleetManagerDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}