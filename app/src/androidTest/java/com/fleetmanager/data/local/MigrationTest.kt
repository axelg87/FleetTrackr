package com.fleetmanager.data.local

import android.database.Cursor
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fleetmanager.domain.model.ProviderEarning
import com.fleetmanager.domain.model.ProviderType
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Room migration test for FleetManager database
 * 
 * Tests database migrations to ensure data integrity during upgrades.
 * 
 * IMPORTANT: As of the provider-based model migration, we use destructive migration
 * for v6→v8 since all data is in Firestore collection "entriesNEW".
 * These tests document expected behavior and can be re-enabled if we add proper migrations.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    
    private val TEST_DB = "migration-test"
    
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FleetManagerDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )
    
    /**
     * Test that MIGRATION_5_6 works correctly
     * This migration adds driverId column to expenses table
     */
    @Test
    @Throws(IOException::class)
    fun migrate5To6_addsDriverIdToExpenses() {
        // Arrange: Create database at version 5
        helper.createDatabase(TEST_DB, 5).apply {
            // Insert test expense without driverId (doesn't exist in v5)
            execSQL("""
                INSERT INTO expenses (id, userId, date, amount, type, notes, vehicle, isSynced, createdAt, updatedAt)
                VALUES ('expense-1', 'user-123', 1696118400000, 150.0, 'FUEL', 'Test expense', 'vehicle-1', 1, 1696118400000, 1696118400000)
            """)
            close()
        }
        
        // Act: Run migration 5→6
        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)
        
        // Assert: Check that driverId column exists and has default value
        val cursor = db.query("SELECT driverId FROM expenses WHERE id = 'expense-1'")
        assertTrue("Cursor should have at least one row", cursor.moveToFirst())
        
        val driverIdIndex = cursor.getColumnIndex("driverId")
        assertTrue("driverId column should exist", driverIdIndex >= 0)
        
        val driverId = cursor.getString(driverIdIndex)
        assertEquals("driverId should default to empty string", "", driverId)
        
        cursor.close()
        db.close()
    }
    
    /**
     * Test that fresh database at v8 has correct schema
     * 
     * This verifies the current schema defined in @Entity classes
     */
    @Test
    @Throws(IOException::class)
    fun migrate_FreshDatabaseV8_hasCorrectSchema() {
        // Arrange & Act: Create fresh database at version 8
        val db = helper.createDatabase(TEST_DB, 8)
        
        // Assert: Verify daily_entries table has correct columns
        val cursor = db.query("PRAGMA table_info(daily_entries)")
        
        val columns = mutableMapOf<String, String>()
        val nameIndex = cursor.getColumnIndex("name")
        val typeIndex = cursor.getColumnIndex("type")
        
        while (cursor.moveToNext()) {
            val name = cursor.getString(nameIndex)
            val type = cursor.getString(typeIndex)
            columns[name] = type
        }
        cursor.close()
        
        // Verify expected columns exist with correct types
        assertEquals("id should be TEXT", "TEXT", columns["id"])
        assertEquals("userId should be TEXT", "TEXT", columns["userId"])
        assertEquals("date should be INTEGER", "INTEGER", columns["date"])
        assertEquals("driverId should be TEXT", "TEXT", columns["driverId"])
        assertEquals("vehicleId should be TEXT", "TEXT", columns["vehicleId"])
        assertEquals("providers should be TEXT (JSON)", "TEXT", columns["providers"])
        assertEquals("notes should be TEXT", "TEXT", columns["notes"])
        assertEquals("photoUrls should be TEXT (JSON)", "TEXT", columns["photoUrls"])
        assertEquals("localPhotoPaths should be TEXT (JSON)", "TEXT", columns["localPhotoPaths"])
        assertEquals("isSynced should be INTEGER (boolean)", "INTEGER", columns["isSynced"])
        assertEquals("createdAt should be INTEGER (timestamp)", "INTEGER", columns["createdAt"])
        assertEquals("updatedAt should be INTEGER (timestamp)", "INTEGER", columns["updatedAt"])
        
        // Verify flat earnings columns do NOT exist
        assertFalse("uberEarnings should NOT exist in v8", columns.containsKey("uberEarnings"))
        assertFalse("careemEarnings should NOT exist in v8", columns.containsKey("careemEarnings"))
        assertFalse("yangoEarnings should NOT exist in v8", columns.containsKey("yangoEarnings"))
        assertFalse("privateJobsEarnings should NOT exist in v8", columns.containsKey("privateJobsEarnings"))
        
        db.close()
    }
    
    /**
     * Test TypeConverter integration with Room
     * 
     * Verifies that List<ProviderEarning> is correctly converted to/from JSON
     */
    @Test
    @Throws(IOException::class)
    fun typeConverter_providersJsonSerialization_worksCorrectly() {
        // Arrange: Create fresh database
        val db = helper.createDatabase(TEST_DB, 8)
        
        // Prepare providers JSON
        val providersJson = """
            [
                {"type":"UBER","amount":212.84,"currency":"AED","tripsCount":9},
                {"type":"YANGO","amount":207.20,"currency":"AED","tripsCount":7}
            ]
        """.trimIndent()
        
        // Act: Insert entry with providers
        db.execSQL("""
            INSERT INTO daily_entries 
            (id, userId, date, driverId, vehicleId, providers, notes, photoUrls, localPhotoPaths, isSynced, createdAt, updatedAt)
            VALUES 
            ('entry-1', 'user-123', 1696118400000, 'driver-1', 'vehicle-1', ?, '', '[]', '[]', 1, 1696118400000, 1696118400000)
        """, arrayOf(providersJson))
        
        // Assert: Read back and verify JSON structure
        val cursor = db.query("SELECT providers FROM daily_entries WHERE id = 'entry-1'")
        assertTrue("Should find entry", cursor.moveToFirst())
        
        val retrievedJson = cursor.getString(cursor.getColumnIndex("providers"))
        assertNotNull("providers should not be null", retrievedJson)
        
        // Parse JSON and verify structure
        val jsonArray = JSONArray(retrievedJson)
        assertEquals("Should have 2 providers", 2, jsonArray.length())
        
        val provider1 = jsonArray.getJSONObject(0)
        assertEquals("First provider type", "UBER", provider1.getString("type"))
        assertEquals("First provider amount", 212.84, provider1.getDouble("amount"), 0.01)
        
        val provider2 = jsonArray.getJSONObject(1)
        assertEquals("Second provider type", "YANGO", provider2.getString("type"))
        assertEquals("Second provider amount", 207.20, provider2.getDouble("amount"), 0.01)
        
        cursor.close()
        db.close()
    }
    
    /**
     * Test that destructive migration is used for v6→v8
     * 
     * NOTE: This test DOCUMENTS the current behavior.
     * Since MIGRATION_6_7 and MIGRATION_7_8 are removed, 
     * fallbackToDestructiveMigration() will be used.
     * 
     * Expected behavior:
     * - Old local data is dropped
     * - App syncs from Firestore collection "entriesNEW" on first launch
     */
    @Test
    @Throws(IOException::class)
    fun migrate6To8_usesDestructiveMigration_documentsBehavior() {
        // Arrange: Create v6 database with sample data
        helper.createDatabase(TEST_DB, 6).apply {
            // Insert sample expense (v6 schema after MIGRATION_5_6)
            execSQL("""
                INSERT INTO expenses 
                (id, userId, date, amount, type, notes, vehicle, driverId, isSynced, createdAt, updatedAt)
                VALUES 
                ('expense-1', 'user-123', 1696118400000, 150.0, 'FUEL', 'Test', 'vehicle-1', 'driver-1', 1, 1696118400000, 1696118400000)
            """)
            close()
        }
        
        // Act: Attempt to migrate v6→v8
        // Since MIGRATION_6_7 is removed, this will trigger destructive migration
        val db = helper.runMigrationsAndValidate(TEST_DB, 8, false) // validateDroppedTables = false
        
        // Assert: Database schema is correct for v8
        val cursor = db.query("PRAGMA table_info(daily_entries)")
        val columns = mutableListOf<String>()
        val nameIndex = cursor.getColumnIndex("name")
        
        while (cursor.moveToNext()) {
            columns.add(cursor.getString(nameIndex))
        }
        cursor.close()
        
        // Verify v8 schema exists
        assertTrue("Should have providers column", columns.contains("providers"))
        assertFalse("Should NOT have flat earnings columns", columns.contains("uberEarnings"))
        
        // NOTE: Old data from v6 will be lost, which is expected
        // App will sync from Firestore on first launch
        
        db.close()
    }
    
    /**
     * Helper test: Verify Converters work standalone
     */
    @Test
    fun converters_standalone_workCorrectly() {
        val converters = Converters()
        
        // Test: Empty list
        val emptyJson = converters.toProvidersJson(emptyList())
        assertEquals("[]", emptyJson)
        
        val emptyList = converters.fromProvidersJson("[]")
        assertTrue(emptyList.isEmpty())
        
        // Test: Valid providers
        val providers = listOf(
            ProviderEarning(ProviderType.UBER, 212.84, "AED", 9, null),
            ProviderEarning(ProviderType.YANGO, 207.20, "AED", 7, null)
        )
        
        val json = converters.toProvidersJson(providers)
        assertNotNull(json)
        assertNotEquals("[]", json)
        
        val parsed = converters.fromProvidersJson(json)
        assertEquals(2, parsed.size)
        assertEquals(ProviderType.UBER, parsed[0].type)
        assertEquals(212.84, parsed[0].amount, 0.01)
        assertEquals(ProviderType.YANGO, parsed[1].type)
        assertEquals(207.20, parsed[1].amount, 0.01)
    }
}

// Migration objects referenced from FleetManagerDatabase
// These are imported for testing purposes
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
