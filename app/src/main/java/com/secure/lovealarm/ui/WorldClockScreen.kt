package com.secure.lovealarm.ui

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.*

data class WorldClock(
    val id: String,
    val timezoneId: String,
    val cityName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockScreen() {
    var clocks by remember { mutableStateOf<List<WorldClock>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    
    // Initialize with local timezone
    LaunchedEffect(Unit) {
        if (clocks.isEmpty()) {
            try {
                val localTimezone = TimeZone.getDefault().id
                clocks = listOf(
                    WorldClock(
                        id = "local",
                        timezoneId = localTimezone,
                        cityName = getCityName(localTimezone)
                    )
                )
            } catch (_: Exception) {
                // Fallback if timezone/default fails
                clocks = listOf(
                    WorldClock(id = "local", timezoneId = "UTC", cityName = "Local")
                )
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
                        text = "World Clock",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 60.dp, end = 16.dp, bottom = 0.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Clock",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Clock")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Local time display at top
            ClockWidget()
            
            Divider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            if (clocks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "No world clocks",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap + to add a world clock",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = clocks,
                        key = { it.id }
                    ) { clock ->
                        WorldClockItem(
                            clock = clock,
                            is24Hour = is24Hour,
                            onDelete = {
                                clocks = clocks.filter { it.id != clock.id }
                            }
                        )
                    }
                }
            }
        }
    }
    }
    
    if (showAddDialog) {
        AddWorldClockDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { timezoneId, cityName ->
                clocks = clocks + WorldClock(
                    id = UUID.randomUUID().toString(),
                    timezoneId = timezoneId,
                    cityName = cityName
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun WorldClockItem(
    clock: WorldClock,
    is24Hour: Boolean,
    onDelete: () -> Unit
) {
    var currentTime by remember { mutableStateOf(getTimeForTimezone(clock.timezoneId, is24Hour)) }
    
    LaunchedEffect(clock.timezoneId, is24Hour) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTime = getTimeForTimezone(clock.timezoneId, is24Hour)
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clock.cityName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = getDateForTimezone(clock.timezoneId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private const val DialogExitDurationMs = 200

@Composable
fun AddWorldClockDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var isExiting by remember { mutableStateOf(false) }
    val popularTimezones = listOf(
        "America/New_York" to "New York",
        "America/Los_Angeles" to "Los Angeles",
        "Europe/London" to "London",
        "Europe/Paris" to "Paris",
        "Asia/Tokyo" to "Tokyo",
        "Asia/Shanghai" to "Shanghai",
        "Asia/Dubai" to "Dubai",
        "Australia/Sydney" to "Sydney",
        "America/Chicago" to "Chicago",
        "America/Denver" to "Denver",
        "America/Phoenix" to "Phoenix",
        "America/Toronto" to "Toronto",
        "Europe/Berlin" to "Berlin",
        "Europe/Madrid" to "Madrid",
        "Europe/Rome" to "Rome",
        "Asia/Hong_Kong" to "Hong Kong",
        "Asia/Singapore" to "Singapore",
        "Asia/Seoul" to "Seoul",
        "Asia/Mumbai" to "Mumbai",
        "America/Mexico_City" to "Mexico City",
        "America/Sao_Paulo" to "São Paulo",
        "Africa/Cairo" to "Cairo",
        "Africa/Johannesburg" to "Johannesburg"
    )

    fun dismissWithAnimation() {
        if (!isExiting) {
            isExiting = true
        }
    }

    LaunchedEffect(isExiting) {
        if (isExiting) {
            kotlinx.coroutines.delay(DialogExitDurationMs.toLong())
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { dismissWithAnimation() },
        properties = DialogProperties()
    ) {
        AnimatedVisibility(
            visible = !isExiting,
            enter = scaleIn(initialScale = 0.92f) + fadeIn(),
            exit = scaleOut(targetScale = 0.92f) + fadeOut()
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Add World Clock",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = popularTimezones,
                            key = { it.first }
                        ) { (timezoneId, cityName) ->
                            TextButton(
                                onClick = { onAdd(timezoneId, cityName) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = cityName,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { dismissWithAnimation() }) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

fun getTimeForTimezone(timezoneId: String, is24Hour: Boolean): String {
    val pattern = if (is24Hour) "HH:mm:ss" else "hh:mm:ss a"
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone(timezoneId)
    return sdf.format(Date())
}

fun getDateForTimezone(timezoneId: String): String {
    val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone(timezoneId)
    return sdf.format(Date())
}

fun getCityName(timezoneId: String): String {
    return timezoneId.split("/").lastOrNull()?.replace("_", " ") ?: timezoneId
}

