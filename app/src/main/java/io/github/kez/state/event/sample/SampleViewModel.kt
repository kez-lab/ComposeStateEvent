package io.github.kez.state.event.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SampleViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(SampleUiState())
    val uiState: StateFlow<SampleUiState> = _uiState.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Simulate network call
                delay(2000)
                val data = listOf("Item 1", "Item 2", "Item 3")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        data = data,
                        showSuccessMessage = "Data loaded successfully!"
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load data: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun onItemClick(item: String) {
        _uiState.update {
            it.copy(navigateToDetail = item)
        }
    }
}