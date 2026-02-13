package com.secure.lovealarm.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secure.lovealarm.MainActivity
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen() {
    var hours by remember { mutableStateOf(0) }
    var minutes by remember { mutableStateOf(0) }
    var seconds by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var totalSeconds by remember { mutableStateOf(0L) }
    var remainingSeconds by remember { mutableStateOf(0L) }
    val context = LocalContext.current
    
    // Calculate total time
    val totalTime = remember(hours, minutes, seconds) {
        (hours * 3600L + minutes * 60L + seconds).toLong()
    }
    
    val progress = if (totalSeconds > 0) {
        (totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "timer_progress"
    )
    
    // Timer countdown logic
    LaunchedEffect(isRunning) {
        while (isRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
            if (remainingSeconds <= 0) {
                // Show notification first (before state update) so LaunchedEffect
                // cancellation doesn't prevent it from running
                showTimerNotification(context, "Timer finished!")
                isRunning = false
                remainingSeconds = 0
            }
        }
    }
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
                        text = "Timer",
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
            // Timer display - wrap in full-width Box so circle stays centered when layout changes
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Progress circle
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(300.dp),
                        strokeWidth = 8.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    
                    // Time display - centered in circle, larger text, padding so it never touches the circle
                    val timerTextStyle = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 52.sp,
                        lineHeight = 60.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp), // margin from circle edge so text never touches it
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.wrapContentSize()
                        ) {
                            if (isRunning || totalSeconds > 0) {
                                val displayHours = (remainingSeconds / 3600).toInt()
                                val displayMinutes = ((remainingSeconds % 3600) / 60).toInt()
                                val displaySeconds = (remainingSeconds % 60).toInt()
                                
                                Text(
                                    text = String.format("%02d:%02d:%02d", displayHours, displayMinutes, displaySeconds),
                                    style = timerTextStyle,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            } else {
                                Text(
                                    text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                                    style = timerTextStyle,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            
            // Time picker (only when not running and timer hasn't started)
            if (!isRunning && totalSeconds == 0L) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TimePickerUnit(
                        value = hours,
                        label = "Hours",
                        onIncrement = { if (hours < 23) hours++ },
                        onDecrement = { if (hours > 0) hours-- }
                    )
                    TimePickerUnit(
                        value = minutes,
                        label = "Minutes",
                        onIncrement = { 
                            if (minutes < 59) {
                                minutes++
                            } else {
                                minutes = 0
                                if (hours < 23) hours++
                            }
                        },
                        onDecrement = { 
                            if (minutes > 0) {
                                minutes--
                            } else {
                                minutes = 59
                                if (hours > 0) hours--
                            }
                        }
                    )
                    TimePickerUnit(
                        value = seconds,
                        label = "Seconds",
                        onIncrement = { 
                            if (seconds < 59) {
                                seconds++
                            } else {
                                seconds = 0
                                if (minutes < 59) {
                                    minutes++
                                } else {
                                    minutes = 0
                                    if (hours < 23) hours++
                                }
                            }
                        },
                        onDecrement = { 
                            if (seconds > 0) {
                                seconds--
                            } else {
                                seconds = 59
                                if (minutes > 0) {
                                    minutes--
                                } else {
                                    minutes = 59
                                    if (hours > 0) hours--
                                }
                            }
                        }
                    )
                }
            }
            }
            
            // Control buttons - pinned to bottom so always fully visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isRunning || totalSeconds > 0) {
                    Button(
                        onClick = {
                            isRunning = false
                            remainingSeconds = 0
                            totalSeconds = 0
                            hours = 0
                            minutes = 0
                            seconds = 0
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
                        Text("Reset")
                    }
                    
                    Button(
                        onClick = {
                            isRunning = !isRunning
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isRunning) "Pause" else "Resume")
                    }
                } else {
                    val canStart = totalTime > 0
                    val backgroundColor = if (canStart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
                    val contentColor = if (canStart) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    val startButtonShape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(backgroundColor, startButtonShape)
                            .then(
                                if (canStart) Modifier.clickable {
                                    totalSeconds = totalTime
                                    remainingSeconds = totalTime
                                    isRunning = true
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .wrapContentSize()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start",
                                modifier = Modifier.size(20.dp),
                                tint = contentColor
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Start",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun TimePickerUnit(
    value: Int,
    label: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onIncrement) {
            Icon(Icons.Default.ArrowDropUp, contentDescription = "Increment")
        }
        Text(
            text = String.format("%02d", value),
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onDecrement) {
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Decrement")
        }
    }
}

fun showTimerNotification(context: Context, message: String) {
    try {
        // Use applicationContext so notification works even if activity is destroyed
        val ctx = context.applicationContext
        val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "timer_channel",
                "Timer Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = androidx.core.app.NotificationCompat.Builder(ctx, "timer_channel")
            .setContentTitle("Timer Finished")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
            .setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
            .setVibrate(longArrayOf(0, 500, 500, 500))
            .build()

        notificationManager.notify(2, notification)

        // Vibrate (wrap in try-catch; can throw if permission denied on some devices)
        try {
            val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 500, 500, 500, 500, 500),
                        0
                    ))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(longArrayOf(0, 500, 500, 500, 500, 500), 0)
                }
            }
        } catch (_: Exception) { /* ignore vibrate failures */ }
    } catch (_: Exception) { /* prevent timer end from crashing the app */ }
}
