package com.fleetmanager.data.local.dao

import androidx.room.*
import com.fleetmanager.data.dto.DailyEntryDto
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface DailyEntryDao {
    
    @Query("SELECT * FROM daily_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<DailyEntryDto>>
    
    @Query("SELECT * FROM daily_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getEntriesByDateRange(startDate: Date, endDate: Date): Flow<List<DailyEntryDto>>
    
    @Query("SELECT * FROM daily_entries WHERE isSynced = 0")
    suspend fun getUnsyncedEntries(): List<DailyEntryDto>
    
    @Query("SELECT * FROM daily_entries WHERE id = :id")
    suspend fun getEntryById(id: String): DailyEntryDto?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DailyEntryDto)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<DailyEntryDto>)
    
    @Update
    suspend fun updateEntry(entry: DailyEntryDto)
    
    @Delete
    suspend fun deleteEntry(entry: DailyEntryDto)
    
    @Query("UPDATE daily_entries SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
    
    @Query("SELECT SUM(uberEarnings + yangoEarnings + privateJobsEarnings) FROM daily_entries WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalEarningsForPeriod(startDate: Date, endDate: Date): Double
}