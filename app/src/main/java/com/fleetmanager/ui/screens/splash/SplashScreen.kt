package com.fleetmanager.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleetmanager.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    val scale = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.8f) }
    
    // Animation effects
    LaunchedEffect(Unit) {
        // Logo scale animation
        logoScale.animateTo(
            targetValue = 1.2f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            )
        )
        
        // Text scale animation
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 600,
                delayMillis = 400,
                easing = FastOutSlowInEasing
            )
        )
        
        // Wait for animations to complete, then navigate (reduced to 750ms as requested)
        delay(750)
        onSplashComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Company Logo
            Icon(
                painter = painterResource(id = R.drawable.ic_company_logo),
                contentDescription = "AG Motion Logo",
                modifier = Modifier
                    .size(200.dp)
                    .scale(logoScale.value),
                tint = Color.Unspecified
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Company Name
            Text(
                text = "AG MOTION",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                ),
                color = Color.White,
                modifier = Modifier.scale(scale.value)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tagline
            Text(
                text = "Fleet Management System",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp
                ),
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.scale(scale.value)
            )
        }
    }
}