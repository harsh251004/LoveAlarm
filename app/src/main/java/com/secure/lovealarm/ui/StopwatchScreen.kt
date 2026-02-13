package com.secure.lovealarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LapTime(
    val id: Int,
    val time: Long,
    val lapNumber: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopwatchScreen() {
    var elapsedTime by remember { mutableStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var lapTimes by remember { mutableStateOf<List<LapTime>>(emptyList()) }
    var lapCounter by remember { mutableStateOf(1) }
    val scope = rememberCoroutineScope()
    
    // Use start time + base elapsed so we don't rely on stale state reads in the coroutine
    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        val baseElapsed = elapsedTime
        val startTime = System.currentTimeMillis()
        while (isRunning) {
            delay(50) // Update every 50ms (20/sec) to reduce jank while keeping display smooth
            elapsedTime = baseElapsed + (System.currentTimeMillis() - startTime)
        }
    }
    
    val hours = (elapsedTime / 3600000).toInt()
    val minutes = ((elapsedTime % 3600000) / 60000).toInt()
    val seconds = ((elapsedTime % 60000) / 1000).toInt()
    val milliseconds = ((elapsedTime % 1000) / 10).toInt()
    val isDark = isSystemInDarkTheme()
    val gradientColors = if (isDark) {
        listOf(Color(0xFF1A0E14), Color(0xFF2D1520), Color(0xFF3D1F2E))
    } else {
        listOf(Color(0xFFFFF0F5), Color(0xFFFFE4EC), Color(0xFFFFB6C1))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = gradientColors))
    ) {
        HeartsOverlay()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Stopwatch",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 60.dp, end = 16.dp, bottom = 0.dp)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            // Time display: circle only when no laps; when laps exist ("lap page") no circle, just time + lap list
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (lapTimes.isEmpty()) {
                    // No laps: show circle with time centered
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.size(300.dp),
                                strokeWidth = 8.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Scale font so full time "00:00:00.00" always fits in circle on any screen (cached to avoid glitch)
                                val fontSize = remember(maxWidth) { (maxWidth.value / 11f * 1.5f).coerceIn(32f, 56f).sp }
                                Text(
                                    text = String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, milliseconds),
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontSize = fontSize,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }
                } else {
                    // Lap page: no circle – current time at top, then lap list
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, milliseconds),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = lapTimes.reversed(),
                                key = { it.id }
                            ) { lap ->
                                LapTimeItem(lap = lap)
                            }
                        }
                    }
                }
            }
            
            // Control buttons - at bottom like timer page
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isRunning) {
                    // Lap button
                    Button(
                        onClick = {
                            lapTimes = lapTimes + LapTime(
                                id = lapCounter,
                                time = elapsedTime,
                                lapNumber = lapCounter
                            )
                            lapCounter++
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lap")
                    }
                    
                    // Stop button
                    Button(
                        onClick = {
                            isRunning = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop")
                    }
                } else {
                    if (elapsedTime > 0) {
                        // Reset button
                        Button(
                            onClick = {
                                elapsedTime = 0
                                lapTimes = emptyList()
                                lapCounter = 1
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset")
                        }
                        
                        // Start button
                        Button(
                            onClick = {
                                isRunning = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start")
                        }
                    } else {
                        // Start button (full width when no time elapsed)
                        Button(
                            onClick = {
                                isRunning = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start")
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun LapTimeItem(lap: LapTime) {
    val hours = (lap.time / 3600000).toInt()
    val minutes = ((lap.time % 3600000) / 60000).toInt()
    val seconds = ((lap.time % 60000) / 1000).toInt()
    val milliseconds = ((lap.time % 1000) / 10).toInt()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Lap ${lap.lapNumber}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false)
        )
        Text(
            text = String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, milliseconds),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFeatureSettings = "tnum"
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}



