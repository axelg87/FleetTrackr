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
    version = 9,
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
                    // MIGRATION STRATEGY:
                    // - Keep MIGRATION_5_6 for users on v5
                    // - v6→v7→v8 migrations removed - use destructive migration instead
                    // - Rationale: All data migrated to Firestore collection "entriesNEW"
                    // - App syncs from Firestore on first launch after update
                    // - Local Room database recreated fresh with correct schema
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

// MIGRATION 6→7: OBSOLETE - Removed due to schema assumptions
// Problem: This migration assumed v6 had flat earnings columns (uberEarnings, etc.)
// but MIGRATION_5_6 doesn't create them, causing crashes on upgrade.
// Solution: Use fallbackToDestructiveMigration() since all data is in Firestore.
// Users upgrading from v6→v8 will have local DB recreated and sync from entriesNEW collection.
//
// Original migration code preserved below for reference:
/*
private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        // This assumed these columns existed in v6:
        // - uberEarnings, careemEarnings, yangoEarnings, privateJobsEarnings
        // But they don't exist in all v6 schemas, causing SQLiteException
    }
}
*/

// MIGRATION 7→8: OBSOLETE - Removed, using destructive migration
// Problem: Tried to handle intermediate schemas but added complexity
// Solution: Let fallbackToDestructiveMigration() recreate DB, sync from Firestore
//
// Original migration code preserved below for reference:
/*
private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        // Attempted to rename providersJson → providers
        // But column existence checks were unreliable
    }
}
*/