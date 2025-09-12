package com.fleetmanager.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import com.fleetmanager.domain.model.ExpenseTypeItem
import com.fleetmanager.ui.utils.ToastHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseTypeFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context,
    private val toastHelper: ToastHelper
) {
    
    companion object {
        private const val TAG = "ExpenseTypeFirestoreService"
        private const val EXPENSE_TYPES_COLLECTION = "expenseTypes"
    }
    
    private fun getCollection() = firestore.collection(EXPENSE_TYPES_COLLECTION)
    
    suspend fun saveExpenseType(expenseType: ExpenseTypeItem) {
        try {
            getCollection()
                .document(expenseType.id)
                .set(expenseType)
                .await()
            Log.d(TAG, "Successfully saved expense type: ${expenseType.id}")
        } catch (e: Exception) {
            val errorMessage = "Failed to save expense type: ${e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        }
    }
    
    suspend fun getExpenseTypes(): List<ExpenseTypeItem> {
        return try {
            getCollection()
                .whereEqualTo("isActive", true)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject<ExpenseTypeItem>() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch expense types: ${e.message}", e)
            emptyList()
        }
    }
    
    fun getExpenseTypesFlow(): Flow<List<ExpenseTypeItem>> {
        return getCollection()
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject<ExpenseTypeItem>() }
            }
    }
    
    suspend fun createExpenseType(name: String, displayName: String): ExpenseTypeItem {
        val expenseTypeId = java.util.UUID.randomUUID().toString()
        val expenseType = ExpenseTypeItem(
            id = expenseTypeId,
            name = name.uppercase().replace(" ", "_"),
            displayName = displayName,
            isActive = true
        )
        
        saveExpenseType(expenseType)
        return expenseType
    }
    
    suspend fun initializeDefaultExpenseTypes() {
        val existingExpenseTypes = getExpenseTypes()
        if (existingExpenseTypes.isEmpty()) {
            Log.d(TAG, "Initializing default expense types...")
            val defaultExpenseTypes = listOf(
                ExpenseTypeItem(
                    id = "fuel",
                    name = "FUEL",
                    displayName = "Fuel",
                    isActive = true
                ),
                ExpenseTypeItem(
                    id = "maintenance",
                    name = "MAINTENANCE",
                    displayName = "Maintenance",
                    isActive = true
                ),
                ExpenseTypeItem(
                    id = "service",
                    name = "SERVICE",
                    displayName = "Service",
                    isActive = true
                ),
                ExpenseTypeItem(
                    id = "car_wash",
                    name = "CAR_WASH",
                    displayName = "Car Wash",
                    isActive = true
                ),
                ExpenseTypeItem(
                    id = "fine",
                    name = "FINE",
                    displayName = "Fine",
                    isActive = true
                ),
                ExpenseTypeItem(
                    id = "other",
                    name = "OTHER",
                    displayName = "Other",
                    isActive = true
                )
            )
            
            defaultExpenseTypes.forEach { expenseType ->
                saveExpenseType(expenseType)
            }
            Log.d(TAG, "✅ Default expense types initialized")
        }
    }
}