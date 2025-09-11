package com.fleetmanager.ui.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to show Toast messages from any layer of the application.
 * This is particularly useful for debugging Firestore errors when logs are not accessible.
 */
@Singleton
class ToastHelper @Inject constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: ToastHelper? = null
        
        fun getInstance(): ToastHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ToastHelper().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Show a Toast message from any thread.
     * This method ensures the Toast is shown on the UI thread.
     */
    fun showError(context: Context?, message: String) {
        context?.let { ctx ->
            if (Looper.myLooper() == Looper.getMainLooper()) {
                // Already on UI thread
                Toast.makeText(ctx, "Error: $message", Toast.LENGTH_LONG).show()
            } else {
                // Post to UI thread
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(ctx, "Error: $message", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    /**
     * Show a general Toast message from any thread.
     */
    fun showMessage(context: Context?, message: String) {
        context?.let { ctx ->
            if (Looper.myLooper() == Looper.getMainLooper()) {
                // Already on UI thread
                Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
            } else {
                // Post to UI thread
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}