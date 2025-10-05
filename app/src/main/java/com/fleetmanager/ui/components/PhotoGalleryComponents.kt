package com.fleetmanager.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * Photo Gallery Grid
 * Displays photos in a grid with clickable thumbnails
 * 
 * @param photoUrls List of photo URLs to display
 * @param modifier Modifier for the grid
 * @param columns Number of columns in the grid
 */
@Composable
fun PhotoGalleryGrid(
    photoUrls: List<String>,
    modifier: Modifier = Modifier,
    columns: Int = 3
) {
    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }
    
    if (photoUrls.isEmpty()) return
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(photoUrls.withIndex().toList()) { (index, url) ->
            PhotoThumbnail(
                photoUrl = url,
                onClick = { selectedPhotoIndex = index }
            )
        }
    }
    
    // Show fullscreen viewer when a photo is selected
    selectedPhotoIndex?.let { index ->
        FullscreenPhotoViewer(
            photoUrls = photoUrls,
            initialIndex = index,
            onDismiss = { selectedPhotoIndex = null }
        )
    }
}

/**
 * Photo Thumbnail
 * A clickable thumbnail for a single photo
 * 
 * @param photoUrl URL of the photo to display
 * @param onClick Callback when thumbnail is clicked
 * @param modifier Modifier for the thumbnail
 */
@Composable
fun PhotoThumbnail(
    photoUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Photo thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onError = { /* Gracefully handle failed image loads */ }
        )
    }
}

/**
 * Fullscreen Photo Viewer
 * Displays photos in fullscreen with swipe navigation
 * 
 * @param photoUrls List of photo URLs to display
 * @param initialIndex Initial photo index to display
 * @param onDismiss Callback when viewer is dismissed
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullscreenPhotoViewer(
    photoUrls: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { photoUrls.size }
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Photo pager with swipe navigation
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = photoUrls[page],
                        contentDescription = "Photo ${page + 1} of ${photoUrls.size}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        onError = { /* Gracefully handle failed image loads */ }
                    )
                }
            }
            
            // Top bar with close button and counter
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Photo counter
                    Text(
                        text = "${pagerState.currentPage + 1} / ${photoUrls.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    
                    // Close button
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }
            
            // Page indicator dots (optional, for better UX)
            if (photoUrls.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(photoUrls.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (index == pagerState.currentPage)
                                        Color.White
                                    else
                                        Color.White.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }
        }
    }
}
