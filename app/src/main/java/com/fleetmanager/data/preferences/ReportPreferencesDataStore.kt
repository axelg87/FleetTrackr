package com.fleetmanager.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fleetmanager.ui.viewmodel.EntryTypeFilter
import com.fleetmanager.ui.viewmodel.SortOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reportPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "report_preferences"
)

/**
 * Data class representing user's filter preferences for the Report screen
 */
data class ReportFilterPreferences(
    val selectedDriver: String? = null,
    val selectedVehicle: String? = null,
    val selectedType: String? = null,
    val selectedEntryType: EntryTypeFilter = EntryTypeFilter.ALL,
    val startDate: Date? = null,
    val endDate: Date? = null,
    val sortOption: SortOption = SortOption.DATE_DESC
)

@Singleton
class ReportPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    companion object {
        private fun driverKey(userId: String) = stringPreferencesKey("${userId}_selected_driver")
        private fun vehicleKey(userId: String) = stringPreferencesKey("${userId}_selected_vehicle")
        private fun typeKey(userId: String) = stringPreferencesKey("${userId}_selected_type")
        private fun entryTypeKey(userId: String) = stringPreferencesKey("${userId}_selected_entry_type")
        private fun startDateKey(userId: String) = stringPreferencesKey("${userId}_start_date")
        private fun endDateKey(userId: String) = stringPreferencesKey("${userId}_end_date")
        private fun sortOptionKey(userId: String) = stringPreferencesKey("${userId}_sort_option")
    }
    
    /**
     * Get filter preferences for a specific user
     */
    fun getFilterPreferences(userId: String): Flow<ReportFilterPreferences> {
        return context.reportPreferencesDataStore.data.map { preferences ->
            ReportFilterPreferences(
                selectedDriver = preferences[driverKey(userId)],
                selectedVehicle = preferences[vehicleKey(userId)],
                selectedType = preferences[typeKey(userId)],
                selectedEntryType = preferences[entryTypeKey(userId)]?.let { 
                    try {
                        EntryTypeFilter.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        EntryTypeFilter.ALL
                    }
                } ?: EntryTypeFilter.ALL,
                startDate = preferences[startDateKey(userId)]?.let { dateString ->
                    try {
                        dateFormatter.parse(dateString)
                    } catch (e: Exception) {
                        null
                    }
                },
                endDate = preferences[endDateKey(userId)]?.let { dateString ->
                    try {
                        dateFormatter.parse(dateString)
                    } catch (e: Exception) {
                        null
                    }
                },
                sortOption = preferences[sortOptionKey(userId)]?.let {
                    try {
                        SortOption.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        SortOption.DATE_DESC
                    }
                } ?: SortOption.DATE_DESC
            )
        }
    }
    
    /**
     * Save filter preferences for a specific user
     */
    suspend fun saveFilterPreferences(userId: String, preferences: ReportFilterPreferences) {
        context.reportPreferencesDataStore.edit { prefs ->
            // Save driver preference
            preferences.selectedDriver?.let { 
                prefs[driverKey(userId)] = it 
            } ?: prefs.remove(driverKey(userId))
            
            // Save vehicle preference
            preferences.selectedVehicle?.let { 
                prefs[vehicleKey(userId)] = it 
            } ?: prefs.remove(vehicleKey(userId))
            
            // Save type preference
            preferences.selectedType?.let { 
                prefs[typeKey(userId)] = it 
            } ?: prefs.remove(typeKey(userId))
            
            // Save entry type preference
            prefs[entryTypeKey(userId)] = preferences.selectedEntryType.name
            
            // Save date preferences
            preferences.startDate?.let { 
                prefs[startDateKey(userId)] = dateFormatter.format(it) 
            } ?: prefs.remove(startDateKey(userId))
            
            preferences.endDate?.let { 
                prefs[endDateKey(userId)] = dateFormatter.format(it) 
            } ?: prefs.remove(endDateKey(userId))
            
            // Save sort option preference
            prefs[sortOptionKey(userId)] = preferences.sortOption.name
        }
    }
    
    /**
     * Clear all filter preferences for a specific user
     */
    suspend fun clearFilterPreferences(userId: String) {
        context.reportPreferencesDataStore.edit { prefs ->
            prefs.remove(driverKey(userId))
            prefs.remove(vehicleKey(userId))
            prefs.remove(typeKey(userId))
            prefs.remove(entryTypeKey(userId))
            prefs.remove(startDateKey(userId))
            prefs.remove(endDateKey(userId))
            prefs.remove(sortOptionKey(userId))
        }
    }
}