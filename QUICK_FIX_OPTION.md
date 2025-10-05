# Quick Fix Option: Skip Migrations Temporarily

If you need the app working IMMEDIATELY, here's a quick fix that will work but **will clear local data**:

## Option A: Destructive Migration Only (Fastest)

Replace the database builder in `FleetManagerDatabase.kt`:

```kotlin
val instance = Room.databaseBuilder(
    context.applicationContext,
    FleetManagerDatabase::class.java,
    DATABASE_NAME
)
    // Comment out migrations temporarily
    // .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
    .fallbackToDestructiveMigration()  // ← This will drop all tables and recreate
    .build()
```

**Effect**: App will work immediately, but users will need to re-sync data from Firestore.

## Option B: Keep Migrations But Add Try-Catch

Wrap migration code in try-catch to prevent crashes:

```kotlin
private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        try {
            // ... existing migration code ...
        } catch (e: Exception) {
            Log.e("Migration", "Migration 7→8 failed: ${e.message}", e)
            // Let fallbackToDestructiveMigration handle it
        }
    }
}
```

## Option C: Bump Version Higher (v9)

If users have conflicting v8 schemas:

```kotlin
@Database(
    entities = [...],
    version = 9,  // ← Bump again
    exportSchema = false
)
```

Then add empty MIGRATION_8_9:
```kotlin
private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // No-op migration, schema is already correct
    }
}
```

## Which Option to Choose?

- **App must work NOW**: Option A (destructive)
- **Save some user data**: Option B (try-catch)
- **Clean migration path**: Option C (bump to v9)

---

**Recommendation**: Try the fixes I already applied first. If still crashing, use Option A for immediate recovery.
