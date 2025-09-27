package com.fleetmanager.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.ui.utils.ToastHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context,
    private val toastHelper: ToastHelper
) {
    
    companion object {
        private const val TAG = "VehicleFirestoreService"
        private const val VEHICLES_COLLECTION = "vehicles"
    }
    
    private fun getCollection() = firestore.collection(VEHICLES_COLLECTION)
    
    suspend fun saveVehicle(vehicle: Vehicle) {
        try {
            getCollection()
                .document(vehicle.id)
                .set(vehicle)
                .await()
            Log.d(TAG, "Successfully saved vehicle: AED{vehicle.id}")
        } catch (e: Exception) {
            val errorMessage = "Failed to save vehicle: AED{e.message}"
            Log.e(TAG, errorMessage, e)
            toastHelper.showError(context, errorMessage)
            throw e
        }
    }
    
    suspend fun getVehicles(): List<Vehicle> {
        return try {
            getCollection()
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject<Vehicle>() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch vehicles: AED{e.message}", e)
            emptyList()
        }
    }
    
    fun getVehiclesFlow(): Flow<List<Vehicle>> {
        return getCollection()
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject<Vehicle>() }
            }
    }
    
    suspend fun createVehicle(make: String, model: String, year: Int, licensePlate: String): Vehicle {
        val vehicleId = java.util.UUID.randomUUID().toString()
        val vehicle = Vehicle(
            id = vehicleId,
            make = make,
            model = model,
            year = year,
            licensePlate = licensePlate,
            isActive = true
        )
        
        saveVehicle(vehicle)
        return vehicle
    }
    
}