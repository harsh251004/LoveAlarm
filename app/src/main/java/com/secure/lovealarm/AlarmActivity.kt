package com.secure.lovealarm

import android.content.Intent
import android.media.MediaPlayer
import android.text.format.DateFormat
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secure.lovealarm.data.AlarmRepository
import kotlinx.coroutines.delay
import com.secure.lovealarm.ui.theme.LoveAlarmTheme
import com.secure.lovealarm.utils.AlarmScheduler
import java.util.Calendar

class AlarmActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var alarmId: Long = -1
    private var audioUri: String? = null
    private var alarmLabel: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Get alarm data from intent
            alarmId = intent?.getLongExtra("alarm_id", -1) ?: -1
            audioUri = intent?.getStringExtra("audio_uri")
            
            // Get alarm label safely
            try {
                val repository = AlarmRepository(this)
                val alarm = repository.getAlarmById(alarmId)
                alarmLabel = alarm?.label ?: ""
            } catch (e: Exception) {
                android.util.Log.e("AlarmActivity", "Failed to get alarm label", e)
                alarmLabel = ""
            }
            
            // Set up window flags to show over lockscreen and turn on screen
            try {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
                
                // For Android 10+ (API 29+), also set these flags
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                    try {
                        setShowWhenLocked(true)
                        setTurnScreenOn(true)
                    } catch (e: Exception) {
                        android.util.Log.e("AlarmActivity", "Failed to set show when locked", e)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AlarmActivity", "Failed to set window flags", e)
            }
            
            // Acquire wake lock safely
            try {
                val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
                if (powerManager != null) {
                    wakeLock = powerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        "LoveAlarm::WakeLock"
                    )
                    wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes
                }
            } catch (e: Exception) {
                android.util.Log.e("AlarmActivity", "Failed to acquire wake lock", e)
            }
            
            // Start vibration safely
            try {
                vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null) {
                    startVibration()
                }
            } catch (e: Exception) {
                android.util.Log.e("AlarmActivity", "Failed to start vibration", e)
            }
            
            // Play alarm sound safely
            try {
                playAlarmSound()
            } catch (e: Exception) {
                android.util.Log.e("AlarmActivity", "Failed to play alarm sound", e)
                // Try default sound as fallback
                try {
                    mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                    mediaPlayer?.apply {
                        isLooping = true
                        setVolume(1.0f, 1.0f)
                        start()
                    }
                } catch (ex: Exception) {
                    android.util.Log.e("AlarmActivity", "Failed to play default sound", ex)
                }
            }
            
            // Set up UI - this should always work
            val is24Hour = DateFormat.is24HourFormat(this)
            setContent {
                LoveAlarmTheme {
                    AlarmScreen(
                        alarmLabel = alarmLabel,
                        is24Hour = is24Hour,
                        onDismiss = { dismissAlarm() },
                        onSnooze = { snoozeAlarm() }
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Critical error in onCreate", e)
            // Even if everything fails, try to show the UI
            val is24Hour = DateFormat.is24HourFormat(this)
            setContent {
                LoveAlarmTheme {
                    AlarmScreen(
                        alarmLabel = "Alarm",
                        is24Hour = is24Hour,
                        onDismiss = { finish() },
                        onSnooze = { finish() }
                    )
                }
            }
        }
    }
    
    private fun startVibration() {
        try {
            if (vibrator == null) return
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (vibrator!!.hasVibrator()) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 500, 500),
                            0
                        )
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 500, 500), 0)
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Vibration error", e)
        }
    }
    
    private fun playAlarmSound() {
        try {
            mediaPlayer = if (audioUri != null && audioUri!!.isNotEmpty()) {
                val uri = try {
                    // Try parsing as content URI first (from file picker)
                    if (audioUri!!.startsWith("content://")) {
                        Uri.parse(audioUri)
                    } else if (audioUri!!.startsWith("/")) {
                        // File path
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            Uri.parse("file://$audioUri")
                        } else {
                            Uri.fromFile(java.io.File(audioUri!!))
                        }
                    } else {
                        Uri.parse(audioUri!!)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                
                // Try to create MediaPlayer with the URI
                val player = if (uri != null) {
                    try {
                        MediaPlayer.create(this, uri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } else {
                    null
                }
                
                if (player == null) {
                    // Fallback to default alarm sound
                    MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                } else {
                    player
                }
            } else {
                // Default system alarm sound
                MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
            }
            
            mediaPlayer?.apply {
                isLooping = true
                setVolume(1.0f, 1.0f)
                setOnErrorListener { _, what, extra ->
                    android.util.Log.e("AlarmActivity", "MediaPlayer error: what=$what, extra=$extra")
                    // Try fallback to default sound
                    try {
                        stop()
                        release()
                        mediaPlayer = MediaPlayer.create(this@AlarmActivity, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                        mediaPlayer?.apply {
                            isLooping = true
                            setVolume(1.0f, 1.0f)
                            start()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to default sound
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                mediaPlayer?.apply {
                    isLooping = true
                    setVolume(1.0f, 1.0f)
                    start()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
    
    private fun dismissAlarm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Error stopping media player", e)
        }
        
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Error canceling vibration", e)
        }
        
        try {
            wakeLock?.release()
            wakeLock = null
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Error releasing wake lock", e)
        }
        
        // Stop the service
        try {
            val serviceIntent = Intent(this, AlarmService::class.java).apply {
                action = "STOP_ALARM"
            }
            startService(serviceIntent)
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Error stopping service", e)
        }
        
        finish()
    }
    
    private fun snoozeAlarm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Error stopping media player", e)
        }
        
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Error canceling vibration", e)
        }
        
        try {
            wakeLock?.release()
            wakeLock = null
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Error releasing wake lock", e)
        }
        
        // Schedule snooze
        try {
            val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000) // 10 minutes
            val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarmId)
                putExtra("audio_uri", audioUri)
                putExtra("is_snooze", true)
            }
            val snoozePendingIntent = android.app.PendingIntent.getBroadcast(
                this,
                (alarmId + 1000000).toInt(),
                snoozeIntent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            val alarmManager = getSystemService(ALARM_SERVICE) as? android.app.AlarmManager
            if (alarmManager != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        snoozeTime,
                        snoozePendingIntent
                    )
                } else {
                    @Suppress("DEPRECATION")
                    alarmManager.setExact(
                        android.app.AlarmManager.RTC_WAKEUP,
                        snoozeTime,
                        snoozePendingIntent
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Error scheduling snooze", e)
        }
        
        // Stop the service
        try {
            val serviceIntent = Intent(this, AlarmService::class.java).apply {
                action = "SNOOZE_ALARM"
                putExtra("alarm_id", alarmId)
                putExtra("audio_uri", audioUri)
            }
            startService(serviceIntent)
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Error stopping service", e)
        }
        
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Error releasing media player in onDestroy", e)
        }
        
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Error canceling vibration in onDestroy", e)
        }
        
        try {
            wakeLock?.release()
            wakeLock = null
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Error releasing wake lock in onDestroy", e)
        }
    }
}

@Composable
fun AlarmScreen(
    alarmLabel: String,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val currentTime = remember { mutableStateOf(Calendar.getInstance()) }
    
    // Pulsing animation for the alarm icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime.value = Calendar.getInstance()
        }
    }
    
    val hour = currentTime.value.get(Calendar.HOUR_OF_DAY)
    val minute = currentTime.value.get(Calendar.MINUTE)
    val second = currentTime.value.get(Calendar.SECOND)
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val timeDisplayText = if (is24Hour) {
        String.format("%02d:%02d:%02d", hour, minute, second)
    } else {
        String.format("%02d:%02d:%02d %s", hour12, minute, second, amPm)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Animated alarm icon
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⏰",
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // "ALARM" label
                Text(
                    text = "ALARM",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
                
                // Time display - large and prominent (matches device 12/24h setting)
                Text(
                    text = timeDisplayText,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                
                // Alarm label if present
                if (alarmLabel.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = alarmLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Control buttons - modern design
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Snooze button
                    Button(
                        onClick = onSnooze,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Snooze,
                            contentDescription = "Snooze",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Snooze (10 min)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Stop button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Stop",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Stop Alarm",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

