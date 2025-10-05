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
    version = 7,
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
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
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

private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        // Migration from flat earnings to provider-based model
        
        // 1. Add the new providers column (temporary name: providersJson for migration)
        database.execSQL("ALTER TABLE daily_entries ADD COLUMN providersJson TEXT NOT NULL DEFAULT '[]'")
        
        // 2. Migrate existing data: Convert flat earnings to JSON providers format
        // Handles all 4 legacy providers: UBER, CAREEM, YANGO, PRIVATE
        database.execSQL("""
            UPDATE daily_entries 
            SET providersJson = (
                SELECT '[' || 
                    GROUP_CONCAT(provider_json) || 
                ']'
                FROM (
                    SELECT '{"type":"UBER","amount":' || uberEarnings || ',"currency":"AED"}' as provider_json
                    FROM daily_entries AS inner_table
                    WHERE inner_table.id = daily_entries.id AND uberEarnings > 0
                    
                    UNION ALL
                    
                    SELECT '{"type":"CAREEM","amount":' || careemEarnings || ',"currency":"AED"}' as provider_json
                    FROM daily_entries AS inner_table
                    WHERE inner_table.id = daily_entries.id AND careemEarnings > 0
                    
                    UNION ALL
                    
                    SELECT '{"type":"YANGO","amount":' || yangoEarnings || ',"currency":"AED"}' as provider_json
                    FROM daily_entries AS inner_table
                    WHERE inner_table.id = daily_entries.id AND yangoEarnings > 0
                    
                    UNION ALL
                    
                    SELECT '{"type":"PRIVATE","amount":' || privateJobsEarnings || ',"currency":"AED"}' as provider_json
                    FROM daily_entries AS inner_table
                    WHERE inner_table.id = daily_entries.id AND privateJobsEarnings > 0
                ) AS providers_table
            )
            WHERE uberEarnings > 0 OR careemEarnings > 0 OR yangoEarnings > 0 OR privateJobsEarnings > 0
        """.trimIndent())
        
        // 3. Create a new table with the updated schema (without flat earnings columns)
        // Schema matches DailyEntryDto exactly - TypeConverter handles List<ProviderEarning> as TEXT
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS daily_entries_new (
                id TEXT PRIMARY KEY NOT NULL,
                userId TEXT NOT NULL DEFAULT '',
                date INTEGER NOT NULL,
                driverId TEXT NOT NULL DEFAULT '',
                vehicleId TEXT NOT NULL DEFAULT '',
                providers TEXT NOT NULL,
                notes TEXT NOT NULL,
                photoUrl TEXT,
                localPhotoPath TEXT,
                photoUrls TEXT NOT NULL,
                localPhotoPaths TEXT NOT NULL,
                isSynced INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())
        
        // 4. Copy data from old table to new table
        // Column name is "providers" not "providersJson" to match DailyEntryDto field name
        database.execSQL("""
            INSERT INTO daily_entries_new (
                id, userId, date, driverId, vehicleId, providers, 
                notes, photoUrl, localPhotoPath, photoUrls, localPhotoPaths, 
                isSynced, createdAt, updatedAt
            )
            SELECT 
                id, userId, date, driverId, vehicleId, providersJson,
                notes, photoUrl, localPhotoPath, photoUrls, localPhotoPaths,
                isSynced, createdAt, updatedAt
            FROM daily_entries
        """.trimIndent())
        
        // 5. Drop old table and rename new table
        database.execSQL("DROP TABLE daily_entries")
        database.execSQL("ALTER TABLE daily_entries_new RENAME TO daily_entries")
        
        // 6. Recreate any indices if they existed (add if needed)
        // Example: database.execSQL("CREATE INDEX index_daily_entries_date ON daily_entries(date)")
    }
}