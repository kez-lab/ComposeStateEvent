package io.github.kez.state.event.annotations

/**
 * Annotation to mark a data class as a UI state class.
 * This helps the state-event processor to identify which MutableStateFlow
 * should be used for state event consumption.
 *
 * Usage example:
 * ```
 * @UIState
 * data class MyUiState(
 *     val isLoading: Boolean = false,
 *     @StateEvent
 *     val showMessage: String? = null
 * )
 *
 * class MyViewModel : ViewModel() {
 *     private val someRandomName = MutableStateFlow(MyUiState())
 *     // The processor will find this field based on @UIState annotation
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class UIState