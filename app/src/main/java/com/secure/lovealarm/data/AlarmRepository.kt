package com.secure.lovealarm.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AlarmRepository(context: Context) {
    private val prefs: SharedPreferences? = try {
        context.getSharedPreferences("alarms_prefs", Context.MODE_PRIVATE)
    } catch (_: Exception) {
        null
    }
    private val gson = Gson()
    private val alarmsKey = "alarms_list"

    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    val alarms: StateFlow<List<Alarm>> = _alarms.asStateFlow()

    init {
        try {
            loadAlarms()
        } catch (_: Exception) {
            _alarms.value = emptyList()
        }
    }

    private fun loadAlarms() {
        val p = prefs ?: return
        val alarmsJson = try { p.getString(alarmsKey, null) } catch (_: Exception) { null } ?: return
        try {
            val type = object : TypeToken<List<Alarm>>() {}.type
            val loadedAlarms = gson.fromJson<List<Alarm>>(alarmsJson, type)
            _alarms.value = loadedAlarms ?: emptyList()
        } catch (_: Exception) {
            _alarms.value = emptyList()
        }
    }

    private fun saveAlarms() {
        prefs?.edit()?.putString(alarmsKey, gson.toJson(_alarms.value))?.commit() ?: Unit
    }

    fun addAlarm(alarm: Alarm) {
        _alarms.value = _alarms.value + alarm
        saveAlarms()
    }

    fun updateAlarm(alarm: Alarm) {
        _alarms.value = _alarms.value.map { if (it.id == alarm.id) alarm else it }
        saveAlarms()
    }

    fun deleteAlarm(alarmId: Long) {
        _alarms.value = _alarms.value.filter { it.id != alarmId }
        saveAlarms()
    }

    fun getAlarmById(id: Long): Alarm? {
        return _alarms.value.find { it.id == id }
    }
}



