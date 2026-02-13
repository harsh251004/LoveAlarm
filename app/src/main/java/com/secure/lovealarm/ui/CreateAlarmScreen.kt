package com.secure.lovealarm.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.secure.lovealarm.data.Alarm
import com.secure.lovealarm.utils.AudioRecorder
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CreateAlarmScreen(
    alarm: Alarm? = null,
    onSave: (Alarm) -> Unit,
    onCancel: () -> Unit,
    onDelete: ((Alarm) -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("alarm_ui_prefs", Context.MODE_PRIVATE) }
    var hour by remember { mutableStateOf(alarm?.hour ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(alarm?.minute ?: Calendar.getInstance().get(Calendar.MINUTE)) }
    var label by remember { mutableStateOf(alarm?.label ?: "") }
    var audioUri by remember { mutableStateOf(alarm?.audioUri) }
    var isCustomRecording by remember { mutableStateOf(alarm?.isCustomRecording ?: false) }
    var selectedAudioDisplayName by remember { mutableStateOf<String?>(null) }
    var repeatDays by remember { mutableStateOf(alarm?.repeatDays ?: emptySet<Int>()) }
    var vibrate by remember { mutableStateOf(alarm?.vibrate ?: true) }
    
    var isRecording by remember { mutableStateOf(false) }
    var recordingPath by remember { mutableStateOf<String?>(null) }
    val audioRecorder = remember { AudioRecorder(context) }
    
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteRecordingDialog by remember { mutableStateOf(false) }
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    
    // Load last selected audio when creating new alarm
    LaunchedEffect(alarm) {
        if (alarm == null) {
            val lastUri = prefs.getString("last_alarm_audio_uri", null)
            val lastName = prefs.getString("last_alarm_audio_name", null)
            val lastIsRecording = prefs.getBoolean("last_alarm_audio_is_recording", false)
            if (lastUri != null) {
                audioUri = lastUri
                isCustomRecording = lastIsRecording
                selectedAudioDisplayName = lastName ?: if (lastIsRecording) "Voice recording" else "Audio file"
            }
        } else if (alarm.audioUri != null) {
            selectedAudioDisplayName = if (alarm.isCustomRecording) {
                "Voice recording"
            } else {
                try {
                    getDisplayNameFromUri(context, alarm.audioUri!!)
                } catch (_: Exception) {
                    "Audio file"
                }
            }
        }
    }
    val timePickerState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = is24Hour
    )
    
    // Permissions
    val recordAudioPermission = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.RECORD_AUDIO)
    )
    
    val readStoragePermission = rememberMultiplePermissionsState(
        permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    )
    
    // Audio file picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            audioUri = it.toString()
            isCustomRecording = false
            selectedAudioDisplayName = getDisplayNameFromUri(context, it.toString()) ?: "Audio file"
        }
    }
    
    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .padding(top = 52.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = if (alarm == null) "New Alarm" else "Edit Alarm",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (alarm != null && onDelete != null) {
                        IconButton(onClick = { onDelete(alarm) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Alarm",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            val newAlarm = Alarm(
                                id = alarm?.id ?: System.currentTimeMillis(),
                                hour = hour,
                                minute = minute,
                                isEnabled = alarm?.isEnabled ?: true,
                                audioUri = audioUri,
                                isCustomRecording = isCustomRecording,
                                label = label,
                                repeatDays = repeatDays,
                                vibrate = vibrate
                            )
                            onSave(newAlarm)
                            prefs.edit()
                                .putString("last_alarm_audio_uri", audioUri)
                                .putString("last_alarm_audio_name", selectedAudioDisplayName)
                                .putBoolean("last_alarm_audio_is_recording", isCustomRecording)
                                .apply()
                        },
                        enabled = audioUri != null
                    ) {
                        Text(
                            "Save",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (audioUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Large time display - sized to fit without overflow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (is24Hour) {
                            String.format("%02d:%02d", hour, minute)
                        } else {
                            val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                            val amPm = if (hour < 12) "AM" else "PM"
                            String.format("%02d:%02d %s", hour12, minute, amPm)
                        },
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    TextButton(
                        onClick = { showTimePicker = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Change time",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            
            Divider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )
            
            // Label input
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { 
                    Text(
                        "Label",
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            
            // Repeat days section
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Repeat",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
                    val dayValues = listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, 
                        Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY)
                    
                    dayLabels.forEachIndexed { index, dayLabel ->
                        val dayOfWeek = dayValues[index]
                        val isSelected = repeatDays.contains(dayOfWeek)
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                repeatDays = if (isSelected) {
                                    repeatDays - dayOfWeek
                                } else {
                                    repeatDays + dayOfWeek
                                }
                            },
                            label = { Text(dayLabel) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            Divider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )
            
            // Audio selection section - Enhanced UI
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Alarm Sound",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Choose how you want to set your alarm sound:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Selected audio display with Replace / Delete
                if (audioUri != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Column {
                                    Text(
                                        text = "Selected",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                    )
                                    Text(
                                        text = selectedAudioDisplayName ?: "Audio file",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        audioUri = null
                                        isCustomRecording = false
                                        selectedAudioDisplayName = null
                                        recordingPath = null
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Replace", style = MaterialTheme.typography.labelMedium)
                                }
                                TextButton(
                                    onClick = {
                                        if (isCustomRecording) {
                                            showDeleteRecordingDialog = true
                                        } else {
                                            audioUri = null
                                            isCustomRecording = false
                                            selectedAudioDisplayName = null
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
                
                // Two prominent options
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option 1: Record your voice
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isRecording) {
                                    recordingPath = audioRecorder.stopRecording()
                                    if (recordingPath != null) {
                                        audioUri = recordingPath
                                        isCustomRecording = true
                                        selectedAudioDisplayName = "Voice recording"
                                    }
                                    isRecording = false
                                } else {
                                    if (recordAudioPermission.allPermissionsGranted) {
                                        recordingPath = audioRecorder.startRecording()
                                        isRecording = recordingPath != null
                                    } else {
                                        recordAudioPermission.launchMultiplePermissionRequest()
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRecording) {
                                MaterialTheme.colorScheme.errorContainer
                            } else if (isCustomRecording && audioUri != null) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = when {
                                    isRecording -> MaterialTheme.colorScheme.onErrorContainer
                                    isCustomRecording && audioUri != null -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                val (titleColor, subtitleColor) = when {
                                    isRecording -> MaterialTheme.colorScheme.onErrorContainer to MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                                    isCustomRecording && audioUri != null -> MaterialTheme.colorScheme.onPrimaryContainer to MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                    else -> MaterialTheme.colorScheme.onSurface to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                }
                                Text(
                                    text = if (isRecording) "Recording..." else "Record Your Voice",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = titleColor
                                )
                                Text(
                                    text = if (isRecording) "Tap to stop recording" else "Record a custom sound for this alarm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subtitleColor
                                )
                            }
                            if (isCustomRecording && audioUri != null && !isRecording) {
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    
                    // Option 2: Choose from device files
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    audioPickerLauncher.launch("audio/*")
                                } else {
                                    if (readStoragePermission.allPermissionsGranted) {
                                        audioPickerLauncher.launch("audio/*")
                                    } else {
                                        readStoragePermission.launchMultiplePermissionRequest()
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isCustomRecording && audioUri != null) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = if (!isCustomRecording && audioUri != null) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                val (titleColor, subtitleColor) = if (!isCustomRecording && audioUri != null) {
                                    MaterialTheme.colorScheme.onPrimaryContainer to MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                }
                                Text(
                                    text = "Choose Audio File",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = titleColor
                                )
                                Text(
                                    text = "Select an audio file from your device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subtitleColor
                                )
                            }
                            if (!isCustomRecording && audioUri != null) {
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                
                // Default sound option
                if (audioUri == null) {
                    Text(
                        text = "No custom sound selected. Default alarm sound will be used.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            Divider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )
            
            // Vibration toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Vibrate",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Vibrate when alarm goes off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = vibrate,
                    onCheckedChange = { vibrate = it }
                )
            }
        }
        
        // Time picker dialog - scale content so digits fit in circles/boxes
        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("Set Time") },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(0.85f),
                        contentAlignment = Alignment.Center
                    ) {
                        TimePicker(state = timePickerState)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            hour = timePickerState.hour
                            minute = timePickerState.minute
                            showTimePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Dialog: save recording to device before deleting? — all 3 actions grouped, clearly visible
        if (showDeleteRecordingDialog) {
            Dialog(onDismissRequest = { showDeleteRecordingDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Text(
                            "Delete recording?",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Do you want to save this recording to your device (Downloads folder) before deleting?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        // All 3 actions grouped together, minimal gap (4.dp), clearly visible
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    recordingPath?.let { path ->
                                        audioRecorder.copyRecordingToDevice(path)
                                    }
                                    audioUri = null
                                    isCustomRecording = false
                                    selectedAudioDisplayName = null
                                    recordingPath = null
                                    showDeleteRecordingDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save to device")
                            }
                            OutlinedButton(
                                onClick = {
                                    audioUri = null
                                    isCustomRecording = false
                                    selectedAudioDisplayName = null
                                    recordingPath = null
                                    showDeleteRecordingDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Delete without saving")
                            }
                            OutlinedButton(
                                onClick = { showDeleteRecordingDialog = false },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getDisplayNameFromUri(context: Context, uriString: String): String? {
    return try {
        val uri = Uri.parse(uriString)
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                cursor.getString(nameIndex)
            } else null
        }
    } catch (e: Exception) {
        null
    }
}
