package com.fleetmanager.ui.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

/**
 * Optimized LazyColumn component that implements best practices for performance.
 */
@Composable
fun <T> OptimizedLazyColumn(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    itemContent: @Composable (item: T) -> Unit
) {
    // Use keys to optimize recomposition
    val stableItems = remember(items) { items }
    
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement
    ) {
        items(
            items = stableItems,
            key = key,
            contentType = contentType
        ) { item ->
            // Wrap each item to prevent unnecessary recompositions
            key(key?.invoke(item)) {
                itemContent(item)
            }
        }
    }
}

/**
 * Optimized item component that prevents recompositions when the item hasn't changed.
 */
@Composable
fun <T> OptimizedListItem(
    item: T,
    content: @Composable (T) -> Unit
) {
    // Use remember to prevent recomposition if the item hasn't changed
    val stableItem = remember(item) { item }
    content(stableItem)
}

/**
 * Performance monitoring composable that can be used to detect expensive recompositions.
 */
@Composable
fun PerformanceMonitor(
    tag: String = "Compose",
    content: @Composable () -> Unit
) {
    val recompositionCount = remember { mutableIntStateOf(0) }
    
    SideEffect {
        recompositionCount.intValue++
        if (recompositionCount.intValue > 1) {
            println("AEDtag: Recomposition #AED{recompositionCount.intValue}")
        }
    }
    
    content()
}