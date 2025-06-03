package io.github.kez.state.event.annotations

/**
 * Annotation to mark a property in a UI state class as a state event.
 * When this annotation is applied to a property, the KSP processor will automatically
 * generate a consume function for that event in the ViewModel and optionally a Compose handler.
 *
 * @param consumeFunctionName Optional custom name for the generated consume function.
 *                           If not provided, it will be generated based on the property name.
 * @param eventType The type of event that determines the consumption behavior.
 *                  STANDARD: action first, then consume (for messages, toasts, etc.)
 *                  NAVIGATION: consume first, then action (for navigation events)
 *
 * Usage example:
 * ```
 * data class MyUiState(
 *     @StateEvent(eventType = EventType.STANDARD)
 *     val showSuccessMessage: String? = null,
 *
 *     @StateEvent(consumeFunctionName = "clearError", eventType = EventType.STANDARD)
 *     val errorMessage: String? = null,
 *
 *     @StateEvent(eventType = EventType.NAVIGATION)
 *     val navigateToDetail: String? = null
 * )
 * ```
 *
 * This will generate different consumption orders based on eventType:
 * - STANDARD: LaunchedEffect { action(); consume() }
 * - NAVIGATION: LaunchedEffect { consume(); action() }
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class StateEvent(
    val consumeFunctionName: String = "",
    val eventType: EventType = EventType.STANDARD
)