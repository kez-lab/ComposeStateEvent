package io.github.kez.state.event.sample

import io.github.kez.state.event.annotations.EventType
import io.github.kez.state.event.annotations.StateEvent
import io.github.kez.state.event.annotations.UIState

@UIState
data class SampleUiState(
    val isLoading: Boolean = false,
    val data: List<String> = emptyList(),

    @StateEvent
    val showSuccessMessage: String? = null,

    @StateEvent
    val errorMessage: String? = null,

    @StateEvent(eventType = EventType.NAVIGATION)
    val navigateToDetail: String? = null
)