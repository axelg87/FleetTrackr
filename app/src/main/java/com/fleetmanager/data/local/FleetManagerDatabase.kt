package com.fleetmanager.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.fleetmanager.data.local.dao.DailyEntryDao
import com.fleetmanager.data.local.dao.DriverDao
import com.fleetmanager.data.local.dao.VehicleDao
import com.fleetmanager.data.local.dao.ExpenseDao
import com.fleetmanager.data.dto.DailyEntryDto
import com.fleetmanager.data.dto.DriverDto
import com.fleetmanager.data.dto.VehicleDto
import com.fleetmanager.data.dto.ExpenseDto

@Database(
    entities = [DailyEntryDto::class, DriverDto::class, VehicleDto::class, ExpenseDto::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FleetManagerDatabase : RoomDatabase() {
    
    abstract fun dailyEntryDao(): DailyEntryDao
    abstract fun driverDao(): DriverDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun expenseDao(): ExpenseDao
    
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
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE expenses ADD COLUMN driverId TEXT NOT NULL DEFAULT ''")
        database.execSQL(
            "UPDATE expenses SET driverId = CASE " +
                "WHEN TRIM(driverId) <> '' THEN driverId " +
                "WHEN TRIM(userId) <> '' THEN userId " +
                "ELSE '' END"
        )
    }
}