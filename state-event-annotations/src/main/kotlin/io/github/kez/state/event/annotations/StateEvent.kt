package io.github.kez.state.event.annotations

/**
 * Annotation to mark a property in a UI state class as a state event.
 * When this annotation is applied to a property, the KSP processor will automatically
 * generate a consume function for that event in the ViewModel and optionally a Compose handler.
 *
 * @param consumeFunctionName Optional custom name for the generated consume function.
 *                           If not provided, it will be generated based on the property name.
 * @param handlerFunctionName Optional custom name for the generated Compose handler function.
 *                           If not provided, it will be generated based on the property name.
 *
 * Usage example:
 * ```
 * data class MyUiState(
 *     @StateEvent()
 *     val showSuccessMessage: String? = null,
 *
 *     @StateEvent(consumeFunctionName = "clearError")
 *     val errorMessage: String? = null,
 *
 *     @StateEvent()
 *     val navigateToDetail: String? = null
 * )
 * ```
 *
 * This will generate:
 * ```
 * // ViewModel extensions
 * private fun ViewModel.consumeShowSuccessMessage(_uiState: MutableStateFlow<MyUiState>) { ... }
 * private fun ViewModel.clearError(_uiState: MutableStateFlow<MyUiState>) { ... }
 *
 * // Public ViewModel consume functions
 * fun consumeShowSuccessMessage() { consumeShowSuccessMessage(_uiState) }
 * fun clearError() { clearError(_uiState) }
 *
 * // Compose handlers
 * @Composable
 * fun HandleShowSuccessMessage(uiState: MyUiState, onConsumed: () -> Unit) { ... }
 * ```
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class StateEvent(
    val consumeFunctionName: String = "",
    val handlerFunctionName: String = ""
)