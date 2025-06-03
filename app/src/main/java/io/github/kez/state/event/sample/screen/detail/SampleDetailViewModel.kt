package io.github.kez.state.event.sample.screen.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kez.state.event.annotations.StateEventHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SampleDetailViewModel : ViewModel(), StateEventHandler<SampleDetailUiState> {

    private val _uiState = MutableStateFlow(SampleDetailUiState())
    val uiState: StateFlow<SampleDetailUiState> = _uiState.asStateFlow()

    override fun updateUiState(transform: SampleDetailUiState.() -> SampleDetailUiState) {
        _uiState.update(transform)
    }

    fun loadDetail(itemName: String) {
        viewModelScope.launch {
            updateUiState { copy(isLoading = true, itemName = itemName) }

            try {
                // Simulate network call
                delay(1500)
                val description = "This is a detailed description of $itemName. " +
                        "It contains various information about the selected item and its features."

                updateUiState {
                    copy(
                        isLoading = false,
                        description = description,
                        showToast = "Detail loaded successfully!"
                    )
                }
            } catch (e: Exception) {
                updateUiState {
                    copy(
                        isLoading = false,
                        showDialog = "Failed to load detail: ${e.message}"
                    )
                }
            }
        }
    }

    fun toggleFavorite() {
        val currentState = _uiState.value
        updateUiState {
            copy(
                isFavorite = !currentState.isFavorite,
                showToast = if (!currentState.isFavorite) "Added to favorites" else "Removed from favorites"
            )
        }
    }

    fun shareItem() {
        val currentState = _uiState.value
        updateUiState {
            copy(
                shareContent = "Check out this amazing item: ${currentState.itemName}\n${currentState.description}"
            )
        }
    }

    fun deleteItem() {
        updateUiState {
            copy(
                showDialog = "Are you sure you want to delete '${itemName}'?"
            )
        }
    }

    fun dismissDialog() {
        updateUiState {
            copy(showDialog = null)
        }
    }

    fun confirmDelete() {
        viewModelScope.launch {
            updateUiState { copy(isLoading = true) }

            // Simulate delete operation
            delay(1000)

            updateUiState {
                copy(
                    isLoading = false,
                    showToast = "Item deleted successfully",
                    navigateBack = true
                )
            }
        }
    }

    fun goBack() {
        updateUiState {
            copy(navigateBack = true)
        }
    }
}