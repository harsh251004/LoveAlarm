package com.secure.lovealarm.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun ClockWidget() {
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    val context = LocalContext.current
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // Update every second
            currentTime = Calendar.getInstance()
        }
    }
    
    val hour24 = currentTime.get(Calendar.HOUR_OF_DAY)
    val minute = currentTime.get(Calendar.MINUTE)
    val second = currentTime.get(Calendar.SECOND)
    val amPm = if (hour24 < 12) "AM" else "PM"
    val hour12 = if (hour24 == 0) 12 else if (hour24 > 12) hour24 - 12 else hour24
    
    val timeText = if (is24Hour) {
        String.format("%02d:%02d:%02d", hour24, minute, second)
    } else {
        String.format("%02d:%02d:%02d %s", hour12, minute, second, amPm)
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

