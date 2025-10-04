package com.fleetmanager.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fleetmanager.data.local.dao.DailyEntryDao
import com.fleetmanager.data.local.dao.DriverDao
import com.fleetmanager.data.local.dao.VehicleDao
import com.fleetmanager.data.local.dao.ExpenseDao
import com.fleetmanager.data.dto.DailyEntryDto
import com.fleetmanager.data.dto.DriverDto
import com.fleetmanager.data.dto.VehicleDto
import com.fleetmanager.data.dto.ExpenseDto
import com.google.gson.Gson

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
    private val gson = Gson()

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `daily_entries_new` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL DEFAULT '',
                `date` INTEGER NOT NULL,
                `driverId` TEXT NOT NULL DEFAULT '',
                `vehicleId` TEXT NOT NULL DEFAULT '',
                `earnings` TEXT NOT NULL DEFAULT '[]',
                `odometer` REAL,
                `notes` TEXT NOT NULL,
                `photoUrl` TEXT,
                `localPhotoPath` TEXT,
                `photoUrls` TEXT NOT NULL DEFAULT '[]',
                `localPhotoPaths` TEXT NOT NULL DEFAULT '[]',
                `isSynced` INTEGER NOT NULL DEFAULT 0,
                `totalEarnings` REAL NOT NULL DEFAULT 0.0,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        database.query("SELECT * FROM daily_entries").use { cursor ->
            while (cursor.moveToNext()) {
                val earnings = buildLegacyEarnings(cursor)
                val legacyTotal = cursor.getDoubleOrDefault("uberEarnings") +
                    cursor.getDoubleOrDefault("careemEarnings") +
                    cursor.getDoubleOrDefault("yangoEarnings") +
                    cursor.getDoubleOrDefault("privateJobsEarnings")
                val computedTotal = if (earnings.isNotEmpty()) {
                    earnings.sumOf { it.cardEarnings + it.cashEarnings + it.tips }
                } else {
                    legacyTotal
                }
                val values = ContentValues().apply {
                    put("id", cursor.getStringOrDefault("id"))
                    put("userId", cursor.getStringOrDefault("userId"))
                    put("date", cursor.getLongOrDefault("date"))
                    put("driverId", cursor.getStringOrDefault("driverId"))
                    put("vehicleId", cursor.getStringOrDefault("vehicleId"))
                    put("earnings", gson.toJson(earnings))
                    val odometerReading = cursor.getDoubleOrNull("odometer")
                        ?: cursor.getDoubleOrNull("odometerEnd")
                        ?: cursor.getDoubleOrNull("odometerStart")
                    odometerReading?.let { put("odometer", it) }
                    put("notes", cursor.getStringOrDefault("notes"))
                    put("photoUrl", cursor.getStringOrNull("photoUrl"))
                    put("localPhotoPath", cursor.getStringOrNull("localPhotoPath"))
                    put("photoUrls", cursor.getStringOrDefault("photoUrls", "[]"))
                    put("localPhotoPaths", cursor.getStringOrDefault("localPhotoPaths", "[]"))
                    put("isSynced", cursor.getIntOrDefault("isSynced"))
                    put("totalEarnings", computedTotal)
                    put("createdAt", cursor.getLongOrDefault("createdAt"))
                    put("updatedAt", cursor.getLongOrDefault("updatedAt"))
                }

                database.insert("daily_entries_new", SupportSQLiteDatabase.CONFLICT_REPLACE, values)
            }
        }

        database.execSQL("DROP TABLE daily_entries")
        database.execSQL("ALTER TABLE daily_entries_new RENAME TO daily_entries")
    }

    private fun buildLegacyEarnings(cursor: Cursor): List<LegacyEarning> {
        val earnings = mutableListOf<LegacyEarning>()

        fun addIfPresent(provider: String, value: Double) {
            if (value != 0.0) {
                earnings.add(
                    LegacyEarning(
                        provider = provider,
                        cardEarnings = value
                    )
                )
            }
        }

        addIfPresent("Uber", cursor.getDoubleOrDefault("uberEarnings"))
        addIfPresent("Careem", cursor.getDoubleOrDefault("careemEarnings"))
        addIfPresent("Yango", cursor.getDoubleOrDefault("yangoEarnings"))
        addIfPresent("Private", cursor.getDoubleOrDefault("privateJobsEarnings"))

        return earnings
    }

    private fun Cursor.getStringOrDefault(column: String, default: String = ""): String {
        val index = getColumnIndex(column)
        if (index == -1 || isNull(index)) return default
        return getString(index)
    }

    private fun Cursor.getStringOrNull(column: String): String? {
        val index = getColumnIndex(column)
        if (index == -1 || isNull(index)) return null
        return getString(index)
    }

    private fun Cursor.getLongOrDefault(column: String, default: Long = 0L): Long {
        val index = getColumnIndex(column)
        if (index == -1 || isNull(index)) return default
        return getLong(index)
    }

    private fun Cursor.getIntOrDefault(column: String, default: Int = 0): Int {
        val index = getColumnIndex(column)
        if (index == -1 || isNull(index)) return default
        return getInt(index)
    }

    private fun Cursor.getDoubleOrDefault(column: String, default: Double = 0.0): Double {
        val index = getColumnIndex(column)
        if (index == -1 || isNull(index)) return default
        return getDouble(index)
    }

    private fun Cursor.getDoubleOrNull(column: String): Double? {
        val index = getColumnIndex(column)
        if (index == -1 || isNull(index)) return null
        return getDouble(index)
    }

    private data class LegacyEarning(
        val provider: String,
        val cardEarnings: Double,
        val cashEarnings: Double = 0.0,
        val tips: Double = 0.0,
        val tripCount: Int = 0,
        val hoursOnline: Double = 0.0
    )
}
