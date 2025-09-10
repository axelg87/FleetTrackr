package com.fleetmanager.ui.utils

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Utility functions for optimizing Compose performance.
 */

/**
 * Collects a StateFlow as state with lifecycle awareness to prevent unnecessary recompositions
 * when the screen is not visible.
 */
@Composable
fun <T> StateFlow<T>.collectAsStateWithLifecycle(): State<T> {
    val lifecycleOwner = LocalLifecycleOwner.current
    return remember(this, lifecycleOwner) {
        this.flowWithLifecycle(
            lifecycleOwner.lifecycle,
            Lifecycle.State.STARTED
        )
    }.collectAsState(initial = this.value)
}

/**
 * Collects a Flow as state with lifecycle awareness.
 */
@Composable
fun <T> Flow<T>.collectAsStateWithLifecycle(
    initialValue: T,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED
): State<T> {
    val lifecycleOwner = LocalLifecycleOwner.current
    return remember(this, lifecycleOwner) {
        this.flowWithLifecycle(lifecycleOwner.lifecycle, minActiveState)
    }.collectAsState(initial = initialValue)
}

/**
 * Creates a stable lambda that won't cause recompositions.
 */
@Composable
fun <T> rememberStableLambda(key: Any? = null, lambda: () -> T): () -> T {
    return remember(key) { lambda }
}

/**
 * Creates a stable lambda with one parameter that won't cause recompositions.
 */
@Composable
fun <P, T> rememberStableLambda(key: Any? = null, lambda: (P) -> T): (P) -> T {
    return remember(key) { lambda }
}

/**
 * Marker interface for stable classes to help with Compose optimization.
 * Classes that implement this interface should be immutable and stable.
 */
@Stable
interface StableData

/**
 * Wrapper for making data classes stable for Compose.
 */
@Stable
data class StableWrapper<T>(val value: T) : StableData

/**
 * Extension function to wrap any value in a stable wrapper.
 */
fun <T> T.asStable(): StableWrapper<T> = StableWrapper(this)