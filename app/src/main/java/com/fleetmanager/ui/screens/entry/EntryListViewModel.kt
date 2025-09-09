package com.fleetmanager.ui.screens.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleetmanager.data.model.DailyEntry
import com.fleetmanager.data.repository.FleetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EntryListUiState(
    val entries: List<DailyEntry> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class EntryListViewModel @Inject constructor(
    private val fleetRepository: FleetRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EntryListUiState())
    val uiState: StateFlow<EntryListUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            fleetRepository.getAllDailyEntries()
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message
                    )
                }
                .collect { entries ->
                    _uiState.value = _uiState.value.copy(
                        entries = entries,
                        isLoading = false,
                        errorMessage = null
                    )
                }
        }
    }
}