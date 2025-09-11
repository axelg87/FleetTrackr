package com.fleetmanager.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import com.fleetmanager.auth.AuthService
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authService: AuthService
) {
    
    private fun getUserCollection(collection: String) = 
        firestore.collection("users")
            .document(authService.getCurrentUserId() ?: "")
            .collection(collection)
    
    // Daily Entries
    suspend fun saveDailyEntry(entry: DailyEntry) {
        getUserCollection("dailyEntries")
            .document(entry.id)
            .set(entry)
            .await()
    }
    
    suspend fun getDailyEntries(): List<DailyEntry> {
        return getUserCollection("dailyEntries")
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject<DailyEntry>() }
    }
    
    fun getDailyEntriesFlow(): Flow<List<DailyEntry>> {
        return getUserCollection("dailyEntries")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject<DailyEntry>() }
            }
    }
    
    suspend fun deleteDailyEntry(entryId: String) {
        getUserCollection("dailyEntries")
            .document(entryId)
            .delete()
            .await()
    }
    
    // Drivers
    suspend fun saveDriver(driver: Driver) {
        getUserCollection("drivers")
            .document(driver.id)
            .set(driver)
            .await()
    }
    
    suspend fun getDrivers(): List<Driver> {
        return getUserCollection("drivers")
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject<Driver>() }
    }
    
    // Vehicles
    suspend fun saveVehicle(vehicle: Vehicle) {
        getUserCollection("vehicles")
            .document(vehicle.id)
            .set(vehicle)
            .await()
    }
    
    suspend fun getVehicles(): List<Vehicle> {
        return getUserCollection("vehicles")
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject<Vehicle>() }
    }
    
    // Expenses
    suspend fun saveExpense(expense: Expense) {
        getUserCollection("expenses")
            .document(expense.id)
            .set(expense)
            .await()
    }
    
    suspend fun getExpenses(): List<Expense> {
        return getUserCollection("expenses")
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject<Expense>() }
    }
    
    fun getExpensesFlow(): Flow<List<Expense>> {
        return getUserCollection("expenses")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject<Expense>() }
            }
    }
    
    suspend fun deleteExpense(expenseId: String) {
        getUserCollection("expenses")
            .document(expenseId)
            .delete()
            .await()
    }
}