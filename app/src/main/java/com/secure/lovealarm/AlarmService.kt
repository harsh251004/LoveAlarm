package com.secure.lovealarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val channelId = "alarm_channel"
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                "STOP_ALARM" -> {
                    // Must call startForeground when started from notification action (Android 8+)
                    try {
                        showMinimalForegroundNotification()
                    } catch (e: Exception) {
                        android.util.Log.e("AlarmService", "Error showing minimal notification", e)
                    }
                    try {
                        stopSelf()
                    } catch (e: Exception) {
                        android.util.Log.e("AlarmService", "Error stopping service", e)
                    }
                    return START_NOT_STICKY
                }
                "SNOOZE_ALARM" -> {
                    // Must call startForeground when started from notification action (Android 8+)
                    try {
                        showMinimalForegroundNotification()
                    } catch (e: Exception) {
                        android.util.Log.e("AlarmService", "Error showing minimal notification", e)
                    }
                    try {
                        val snoozeAlarmId = intent.getLongExtra("alarm_id", -1)
                        if (snoozeAlarmId != -1L) {
                            // Stop current alarm
                            try {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                mediaPlayer = null
                            } catch (e: Exception) {
                                android.util.Log.e("AlarmService", "Error stopping media player", e)
                            }
                            
                            try {
                                vibrator?.cancel()
                            } catch (e: Exception) {
                                android.util.Log.e("AlarmService", "Error canceling vibration", e)
                            }
                            
                            // Schedule snooze (10 minutes by default)
                            val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000) // 10 minutes
                            val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
                                putExtra("alarm_id", snoozeAlarmId)
                                putExtra("audio_uri", intent.getStringExtra("audio_uri"))
                                putExtra("is_snooze", true)
                            }
                            val snoozePendingIntent = PendingIntent.getBroadcast(
                                this,
                                (snoozeAlarmId + 1000000).toInt(), // Different request code for snooze
                                snoozeIntent,
                                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                            )
                            val alarmManager = getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager
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
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AlarmService", "Error handling snooze", e)
                    }
                    
                    try {
                        stopSelf()
                    } catch (e: Exception) {
                        android.util.Log.e("AlarmService", "Error stopping service after snooze", e)
                    }
                    return START_NOT_STICKY
                }
            }
            
            val alarmId = intent?.getLongExtra("alarm_id", -1) ?: -1
            val audioUri = intent?.getStringExtra("audio_uri")
            if (intent == null || alarmId == -1L) {
                stopSelf()
                return START_NOT_STICKY
            }
            currentAudioUri = audioUri
            
            try {
                showNotification(alarmId)
            } catch (e: Exception) {
                android.util.Log.e("AlarmService", "Error showing notification", e)
            }
            
            try {
                startVibration()
            } catch (e: Exception) {
                android.util.Log.e("AlarmService", "Error starting vibration", e)
            }
            
            try {
                playAlarmSound(audioUri)
            } catch (e: Exception) {
                android.util.Log.e("AlarmService", "Error playing alarm sound", e)
                // Try default sound as fallback
                try {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                    mediaPlayer?.apply {
                        isLooping = true
                        setVolume(1.0f, 1.0f)
                        start()
                    }
                } catch (ex: Exception) {
                    android.util.Log.e("AlarmService", "Error playing default sound", ex)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmService", "Unexpected error in onStartCommand", e)
        }
        
        return START_NOT_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alarm Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for alarm notifications"
                setBypassDnd(true)
                setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC)
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    /** Call when handling STOP_ALARM/SNOOZE_ALARM so we comply with startForeground within 5s when started from notification. */
    private fun showMinimalForegroundNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Alarm")
            .setContentText("Alarm")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1, notification)
    }
    
    private var alarmId: Long = -1
    private var currentAudioUri: String? = null
    
    private fun showNotification(alarmId: Long) {
        this.alarmId = alarmId
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("audio_uri", currentAudioUri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt() and 0x7FFFFFFF,
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = "STOP_ALARM"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = "SNOOZE_ALARM"
            putExtra("alarm_id", alarmId)
            putExtra("audio_uri", currentAudioUri)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this, 1, snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Alarm")
            .setContentText("Wake up!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_menu_revert, "Snooze", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        startForeground(1, notification)
    }
    
    private fun startVibration() {
        try {
            if (vibrator == null) return
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
            android.util.Log.e("AlarmService", "Vibration error", e)
        }
    }
    
    private fun playAlarmSound(audioUri: String?) {
        try {
            mediaPlayer = if (audioUri != null && audioUri.isNotEmpty()) {
                val uri = try {
                    // Try parsing as content URI first (from file picker)
                    if (audioUri.startsWith("content://")) {
                        Uri.parse(audioUri)
                    } else if (audioUri.startsWith("/")) {
                        // File path
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            Uri.parse("file://$audioUri")
                        } else {
                            Uri.fromFile(java.io.File(audioUri))
                        }
                    } else {
                        Uri.parse(audioUri)
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
                    // If creation failed, fall back to default
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
                    android.util.Log.e("AlarmService", "MediaPlayer error: what=$what, extra=$extra")
                    // Try fallback to default sound
                    try {
                        stop()
                        release()
                        mediaPlayer = MediaPlayer.create(this@AlarmService, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
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
                setOnCompletionListener {
                    // If not looping, restart
                    if (!isLooping) {
                        stopSelf()
                    }
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
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            android.util.Log.e("AlarmService", "Error releasing media player", e)
        }
        
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            android.util.Log.e("AlarmService", "Error canceling vibration", e)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}

