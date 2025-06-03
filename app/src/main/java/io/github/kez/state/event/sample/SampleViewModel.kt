package io.github.kez.state.event.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kez.state.event.annotations.StateEventHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SampleViewModel : ViewModel(), StateEventHandler<SampleUiState> {

    private val _uiState = MutableStateFlow(SampleUiState())
    val uiState: StateFlow<SampleUiState> = _uiState.asStateFlow()

    override fun updateUiState(transform: SampleUiState.() -> SampleUiState) {
        _uiState.update(transform)
    }

    fun loadData() {
        viewModelScope.launch {
            updateUiState {
                copy(isLoading = true)
            }

            try {
                delay(2000)
                val data = listOf("Item 1", "Item 2", "Item 3")
                updateUiState {
                    copy(
                        data = data,
                        isLoading = false,
                        showSuccessMessage = "Data loaded successfully!"
                    )
                }
            } catch (e: Exception) {
                updateUiState {
                    copy(
                        isLoading = false,
                        errorMessage = "Failed to load data: ${e.message}"
                    )
                }
            }
        }
    }

    fun onItemClick(item: String) {
        updateUiState {
            copy(navigateToDetail = item)
        }
    }
}