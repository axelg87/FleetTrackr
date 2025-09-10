package com.fleetmanager.ui.viewmodel

import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.usecase.GetAllEntriesUseCase
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
    private val getAllEntriesUseCase: GetAllEntriesUseCase
) : BaseViewModel<EntryListUiState>() {
    
    override fun getInitialState() = EntryListUiState()
    
    init {
        loadEntries()
    }
    
    private fun loadEntries() {
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isLoading = isLoading) }
            },
            onError = { error ->
                updateState { it.copy(isLoading = false, errorMessage = error) }
            }
        ) {
            getAllEntriesUseCase()
                .collect { entries ->
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