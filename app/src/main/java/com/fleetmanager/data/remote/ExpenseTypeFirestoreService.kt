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
        private const val EXPENSE_TYPES_COLLECTION = "expense_types"
    }
    
    private fun getCollection() = firestore.collection(EXPENSE_TYPES_COLLECTION)
    
    suspend fun saveExpenseType(expenseType: ExpenseTypeItem) {
        try {
            getCollection()
                .document(expenseType.id)
                .set(expenseType)
                .await()
            Log.d(TAG, "Successfully saved expense type: AED{expenseType.id}")
        } catch (e: Exception) {
            val errorMessage = "Failed to save expense type: AED{e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        }
    }
    
    suspend fun getExpenseTypes(): List<ExpenseTypeItem> {
        return try {
            getCollection()
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject<ExpenseTypeItem>() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch expense types: AED{e.message}", e)
            emptyList()
        }
    }
    
    fun getExpenseTypesFlow(): Flow<List<ExpenseTypeItem>> {
        return getCollection()
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
    
}