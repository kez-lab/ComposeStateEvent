package io.github.kez.state.event.annotations

/**
 * Enum defining different types of state events and their consumption behavior.
 * Based on Android's official documentation for handling ViewModel one-off events.
 * 
 * @see [Handle ViewModel events](https://developer.android.com/topic/architecture/ui-layer/events#handle-viewmodel-events)
 */
enum class EventType {
    /**
     * Standard state events where the action is performed first, then the event is consumed.
     * This is suitable for showing messages, toasts, snackbars, etc.
     * 
     * Execution order:
     * 1. Execute action (e.g., show snackbar)
     * 2. Consume event (set to null)
     */
    STANDARD,
    
    /**
     * Navigation events where the event is consumed first, then the action is performed.
     * This prevents multiple navigation calls and ensures navigation happens only once.
     * Recommended by Android's official documentation for navigation events.
     * 
     * Execution order:
     * 1. Consume event (set to null) 
     * 2. Execute action (e.g., navigate to screen)
     */
    NAVIGATION
}