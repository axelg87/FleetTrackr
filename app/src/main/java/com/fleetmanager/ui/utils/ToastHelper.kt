package com.fleetmanager.ui.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Toast
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to show Toast messages from any layer of the application.
 * Provides centralized toast management with different message types and positioning.
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
     * Show an error Toast message from any thread.
     */
    fun showError(context: Context?, message: String) {
        showToast(context, "❌ $message", Toast.LENGTH_LONG, Gravity.BOTTOM)
    }
    
    /**
     * Show a success Toast message from any thread.
     */
    fun showSuccess(context: Context?, message: String) {
        showToast(context, "✅ $message", Toast.LENGTH_SHORT, Gravity.BOTTOM)
    }
    
    /**
     * Show a general Toast message from any thread.
     */
    fun showMessage(context: Context?, message: String) {
        showToast(context, message, Toast.LENGTH_SHORT, Gravity.BOTTOM)
    }
    
    /**
     * Show a warning Toast message from any thread.
     */
    fun showWarning(context: Context?, message: String) {
        showToast(context, "⚠️ $message", Toast.LENGTH_LONG, Gravity.BOTTOM)
    }
    
    /**
     * Show an info Toast message from any thread.
     */
    fun showInfo(context: Context?, message: String) {
        showToast(context, "ℹ️ $message", Toast.LENGTH_SHORT, Gravity.BOTTOM)
    }
    
    /**
     * Internal method to show Toast with specified gravity and duration.
     */
    private fun showToast(context: Context?, message: String, duration: Int, gravity: Int) {
        context?.let { ctx ->
            if (Looper.myLooper() == Looper.getMainLooper()) {
                // Already on UI thread
                createAndShowToast(ctx, message, duration, gravity)
            } else {
                // Post to UI thread
                Handler(Looper.getMainLooper()).post {
                    createAndShowToast(ctx, message, duration, gravity)
                }
            }
        }
    }
    
    /**
     * Create and show the actual Toast with positioning.
     */
    private fun createAndShowToast(context: Context, message: String, duration: Int, gravity: Int) {
        val toast = Toast.makeText(context, message, duration)
        toast.setGravity(gravity, 0, 150) // 150dp from bottom
        toast.show()
    }
}