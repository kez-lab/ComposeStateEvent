package io.github.kez.state.event.sample.screen.detail

import io.github.kez.state.event.annotations.StateEvent
import io.github.kez.state.event.annotations.UIState

@UIState
data class SampleDetailUiState(
    val itemName: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
    val isFavorite: Boolean = false,
    val showDialog: String? = null,

    @StateEvent
    val showToast: String? = null,

    @StateEvent
    val navigateBack: Boolean? = null,

    @StateEvent
    val shareContent: String? = null
)