package com.secure.lovealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.secure.lovealarm.data.AlarmRepository
import com.secure.lovealarm.utils.AlarmScheduler

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val alarmId = intent.getLongExtra("alarm_id", -1)
            val audioUri = intent.getStringExtra("audio_uri")
            val isSnooze = intent.getBooleanExtra("is_snooze", false)
            
            if (alarmId == -1L) {
                Log.w(TAG, "Invalid alarm ID received")
                return
            }
            
            // Get alarm from repository to ensure we have the latest audioUri
            val repository = try {
                AlarmRepository(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create AlarmRepository", e)
                // Continue with provided audioUri
                null
            }
            
            val alarm = try {
                repository?.getAlarmById(alarmId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get alarm from repository", e)
                null
            }
            
            val finalAudioUri = alarm?.audioUri ?: audioUri
            
            // Start full-screen alarm activity
            try {
                val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
                    putExtra("alarm_id", alarmId)
                    putExtra("audio_uri", finalAudioUri)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                }
                context.startActivity(alarmIntent)
                Log.d(TAG, "AlarmActivity started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AlarmActivity", e)
                // Try to at least start the service
            }
            
            // Also start service for background playback
            try {
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra("alarm_id", alarmId)
                    putExtra("audio_uri", finalAudioUri)
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    @Suppress("DEPRECATION")
                    context.startService(serviceIntent)
                }
                Log.d(TAG, "AlarmService started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AlarmService", e)
                // Continue - at least the activity might work
            }
            
            // If not a snooze and alarm is recurring, reschedule it
            if (!isSnooze && alarm != null && alarm.isRepeating() && alarm.isEnabled) {
                try {
                    AlarmScheduler.scheduleAlarm(context, alarm)
                    Log.d(TAG, "Recurring alarm rescheduled")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule recurring alarm", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in AlarmReceiver", e)
            // Don't crash - at least try to show something
            try {
                val fallbackIntent = Intent(context, AlarmActivity::class.java).apply {
                    putExtra("alarm_id", -1)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to start fallback activity", ex)
            }
        }
    }
}

