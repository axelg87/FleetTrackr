package com.fleetmanager.ui.viewmodel

import android.util.Log
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.usecase.GetAllEntriesRealtimeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class EntryListUiState(
    val entries: List<DailyEntry> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class EntryListViewModel @Inject constructor(
    private val getAllEntriesRealtimeUseCase: GetAllEntriesRealtimeUseCase
) : BaseViewModel<EntryListUiState>() {
    
    companion object {
        private const val TAG = "EntryListViewModel"
    }
    
    override fun getInitialState() = EntryListUiState()
    
    init {
        observeEntriesRealtime()
    }
    
    private fun observeEntriesRealtime() {
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isLoading = isLoading) }
            },
            onError = { error ->
                Log.e(TAG, "Error observing entries: $error")
                updateState { it.copy(isLoading = false, errorMessage = error) }
            }
        ) {
            getAllEntriesRealtimeUseCase()
                .catch { e ->
                    Log.e(TAG, "Firestore snapshot listener error", e)
                    updateState { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = "Failed to load entries: ${e.message}"
                        ) 
                    }
                }
                .collect { entries ->
                    Log.d(TAG, "Received ${entries.size} entries from Firestore")
                    updateState {
                        it.copy(
                            entries = entries,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }
}