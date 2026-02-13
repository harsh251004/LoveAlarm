package com.secure.lovealarm.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.secure.lovealarm.data.Alarm
import com.secure.lovealarm.data.AlarmRepository
import com.secure.lovealarm.utils.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AlarmRepository(application)

    val alarms = repository.alarms.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        rescheduleAllAlarms()
    }

    private fun rescheduleAllAlarms() {
        viewModelScope.launch {
            try {
                val currentAlarms = repository.alarms.value
                currentAlarms.forEach { alarm ->
                    if (alarm.isEnabled) {
                        try {
                            AlarmScheduler.scheduleAlarm(getApplication(), alarm)
                        } catch (e: Exception) {
                            Log.e("AlarmViewModel", "Failed to schedule alarm ${alarm.id}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AlarmViewModel", "Failed to reschedule alarms", e)
            }
        }
    }
    
    fun addAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.addAlarm(alarm)
            if (alarm.isEnabled) {
                AlarmScheduler.scheduleAlarm(getApplication(), alarm)
            }
        }
    }
    
    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
            repository.updateAlarm(alarm)
            if (alarm.isEnabled) {
                AlarmScheduler.scheduleAlarm(getApplication(), alarm)
            }
        }
    }
    
    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
            repository.deleteAlarm(alarm.id)
        }
    }
    
    fun toggleAlarm(alarm: Alarm) {
        val updatedAlarm = alarm.copy(isEnabled = !alarm.isEnabled)
        updateAlarm(updatedAlarm)
    }
}



