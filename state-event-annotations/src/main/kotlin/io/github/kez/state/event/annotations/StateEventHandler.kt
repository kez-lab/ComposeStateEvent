package io.github.kez.state.event.annotations

/**
 * Interface that ViewModels must implement to handle state events safely.
 * This provides a type-safe way to update UI state without relying on reflection.
 * 
 * Example usage:
 * ```kotlin
 * class MyViewModel : ViewModel(), StateEventHandler<MyUiState> {
 *     override fun updateUiState(transform: MyUiState.() -> MyUiState) {
 *         _uiState.update(transform)
 *     }
 * }
 * ```
 * 
 * @param T The type of UI state this handler manages
 */
interface StateEventHandler<T> {
    /**
     * Updates the UI state using the provided transform function.
     * This method should call update() on your MutableStateFlow.
     * 
     * @param transform A function that transforms the current state to a new state
     */
    fun updateUiState(transform: T.() -> T)
}