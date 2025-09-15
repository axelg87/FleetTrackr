package com.fleetmanager.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.fleetmanager.ui.utils.ToastHelper
import com.fleetmanager.data.excel.ExcelImportService
import com.fleetmanager.data.excel.ExcelImportManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
    
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
    
    @Provides
    @Singleton
    fun provideToastHelper(): ToastHelper = ToastHelper()
    
    @Provides
    @Singleton
    fun provideExcelImportService(
        @ApplicationContext context: Context
    ): ExcelImportService = ExcelImportService(context)
}