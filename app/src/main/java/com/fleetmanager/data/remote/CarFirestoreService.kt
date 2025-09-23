package com.fleetmanager.data.remote

import android.content.Context
import android.util.Log
import com.fleetmanager.auth.AuthService
import com.fleetmanager.domain.model.Car
import com.fleetmanager.ui.utils.ToastHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authService: AuthService,
    @ApplicationContext private val context: Context,
    private val toastHelper: ToastHelper
) {

    companion object {
        private const val TAG = "CarFirestoreService"
        private const val CARS_COLLECTION = "cars"
    }

    private fun getCollection() = firestore.collection(CARS_COLLECTION)

    fun getCarsFlow(): Flow<List<Car>> {
        val userId = authService.getCurrentUserId()
        return if (userId != null) {
            getCollection()
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot -> snapshot.documents.mapNotNull { it.toObject<Car>() } }
        } else {
            getCollection()
                .snapshots()
                .map { snapshot -> snapshot.documents.mapNotNull { it.toObject<Car>() } }
        }
    }

    suspend fun saveCar(car: Car) {
        val currentUserId = authService.getCurrentUserId() ?: car.userId
        val carToSave = car.copy(
            userId = currentUserId.ifBlank { car.userId }
        )
        try {
            getCollection()
                .document(carToSave.id)
                .set(carToSave)
                .await()
            Log.d(TAG, "Successfully saved car: ${carToSave.id}")
        } catch (e: Exception) {
            val errorMessage = "Failed to save car: ${e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        }
    }

    suspend fun deleteCar(carId: String) {
        try {
            getCollection()
                .document(carId)
                .delete()
                .await()
            Log.d(TAG, "Successfully deleted car: $carId")
        } catch (e: Exception) {
            val errorMessage = "Failed to delete car: ${e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        }
    }
}
