package com.secure.lovealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.secure.lovealarm.data.AlarmRepository
import com.secure.lovealarm.utils.AlarmScheduler

/**
 * Reschedules all enabled alarms after device reboot (alarms are cleared on reboot).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != "android.intent.action.QUICKBOOT_POWERON" &&
            intent?.action != "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }
        try {
            val appContext = context.applicationContext
            val repository = AlarmRepository(appContext)
            val alarms = repository.alarms.value
            var count = 0
            alarms.forEach { alarm ->
                if (alarm.isEnabled) {
                    try {
                        AlarmScheduler.scheduleAlarm(appContext, alarm)
                        count++
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to reschedule alarm ${alarm.id}", e)
                    }
                }
            }
            if (count > 0) {
                Log.d(TAG, "Rescheduled $count alarm(s) after boot")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule alarms after boot", e)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
