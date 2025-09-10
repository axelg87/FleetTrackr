package com.fleetmanager.ui.viewmodel

import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.usecase.GetEntryByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class EntryDetailUiState(
    val entry: DailyEntry? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    private val getEntryByIdUseCase: GetEntryByIdUseCase
) : BaseViewModel<EntryDetailUiState>() {
    
    override fun getInitialState() = EntryDetailUiState()
    
    fun loadEntry(entryId: String) {
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isLoading = isLoading, errorMessage = null) }
            },
            onError = { error ->
                updateState { it.copy(isLoading = false, errorMessage = error) }
            }
        ) {
            getEntryByIdUseCase(entryId)
                .collect { entry ->
                    updateState {
                        it.copy(
                            entry = entry,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }
}