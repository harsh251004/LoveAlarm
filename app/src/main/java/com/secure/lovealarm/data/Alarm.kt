package com.secure.lovealarm.data

import java.util.Calendar

data class Alarm(
    val id: Long = System.currentTimeMillis(),
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val audioUri: String? = null, // URI for the alarm sound
    val isCustomRecording: Boolean = false, // true if recorded, false if selected from files
    val label: String = "",
    val repeatDays: Set<Int> = emptySet(), // Days of week (Calendar.SUNDAY=1, MONDAY=2, etc.)
    val snoozeMinutes: Int = 10, // Snooze duration in minutes
    val vibrate: Boolean = true
) {
    fun getTimeString(is24Hour: Boolean): String {
        return if (is24Hour) {
            String.format("%02d:%02d", hour, minute)
        } else {
            val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val amPm = if (hour < 12) "AM" else "PM"
            String.format("%02d:%02d %s", hour12, minute, amPm)
        }
    }
    
    fun getRepeatDaysString(): String {
        if (repeatDays.isEmpty()) {
            return "Once"
        }
        if (repeatDays.size == 7) {
            return "Every day"
        }
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val sortedDays = repeatDays.sorted()
        return sortedDays.joinToString(", ") { dayNames[it - 1] }
    }
    
    fun getCalendarTime(): Calendar {
        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // If repeating, find next occurrence
        if (repeatDays.isNotEmpty()) {
            val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
            val sortedDays = repeatDays.sorted()
            
            // Check if today is a repeat day and time hasn't passed
            if (sortedDays.contains(currentDayOfWeek) && calendar.timeInMillis > now.timeInMillis) {
                // Today is a repeat day and time hasn't passed, use today
                return calendar
            }
            
            // Find next day in the week (including today if time passed)
            val nextDay = sortedDays.find { it > currentDayOfWeek }
            if (nextDay != null) {
                // Next occurrence is later this week
                val daysToAdd = nextDay - currentDayOfWeek
                calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
            } else {
                // Next occurrence is next week (find first day in sorted list)
                val daysToAdd = 7 - currentDayOfWeek + sortedDays.first()
                calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
            }
        } else {
            // For one-time alarms, if the time has already passed today, set it for tomorrow
            if (calendar.timeInMillis <= now.timeInMillis) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        return calendar
    }
    
    fun isRepeating(): Boolean = repeatDays.isNotEmpty()
}

