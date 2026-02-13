package com.secure.lovealarm.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.secure.lovealarm.data.amoledDarkMode
import com.secure.lovealarm.data.setAmoledDarkMode
import kotlinx.coroutines.launch
import androidx.lifecycle.LifecycleEventObserver

private const val REQUIREMENT_TEXT = "Requirement: Highly recommended"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val amoledDark by context.amoledDarkMode.collectAsState(initial = false)

    var batteryOptimizationIgnored by remember { mutableStateOf(isBatteryOptimizationIgnored(context)) }
    var fullScreenGranted by remember { mutableStateOf(isFullScreenIntentGranted(context)) }
    var microphoneGranted by remember { mutableStateOf(isMicrophoneGranted(context)) }
    var musicAudioGranted by remember { mutableStateOf(isMusicAudioGranted(context)) }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) microphoneGranted = true
        else openAppDetails(context)
    }

    val musicAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) musicAudioGranted = true
        else openAppDetails(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryOptimizationIgnored = isBatteryOptimizationIgnored(context)
                fullScreenGranted = isFullScreenIntentGranted(context)
                microphoneGranted = isMicrophoneGranted(context)
                musicAudioGranted = isMusicAudioGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                        .height(120.dp)
                        .padding(top = 32.dp, start = 4.dp, end = 4.dp, bottom = 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge,
                        fontSize = 32.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(64.dp))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Amoled Dark Mode - at top
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Amoled Dark Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When your system is on dark mode and this feature is enabled, the app will be Amoled pitch black instead of normal dark mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = amoledDark,
                        onCheckedChange = { enabled ->
                            scope.launch { context.setAmoledDarkMode(enabled) }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            PermissionCard(
                title = "IGNORE BATTERY OPTIMIZATIONS",
                requirement = REQUIREMENT_TEXT,
                statusGranted = batteryOptimizationIgnored,
                onClick = {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            PermissionCard(
                title = "ENABLE FULL SCREEN NOTIFICATIONS",
                requirement = REQUIREMENT_TEXT,
                statusGranted = fullScreenGranted,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } else {
                        openAppDetails(context)
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            PermissionCard(
                title = "ENABLE MICROPHONE",
                requirement = REQUIREMENT_TEXT,
                statusGranted = microphoneGranted,
                onClick = {
                    if (microphoneGranted) {
                        openMicrophonePermissionPage(context)
                    } else {
                        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            PermissionCard(
                title = "ENABLE MUSIC AND AUDIO",
                requirement = REQUIREMENT_TEXT,
                statusGranted = musicAudioGranted,
                onClick = {
                    if (musicAudioGranted) {
                        openMusicAudioPermissionPage(context)
                    } else {
                        musicAudioPermissionLauncher.launch(getMusicAudioPermission())
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
                    },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SYSTEM DATE AND TIME SETTINGS",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Open the system date and time settings to change the default time zone of the app or to enable the 24 hour clock in the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    requirement: String,
    statusGranted: Boolean,
    onClick: () -> Unit
) {
    val statusColor = if (statusGranted)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.error

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = requirement,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Status: ${if (statusGranted) "Granted" else "Not granted"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openAppDetails(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun openMicrophonePermissionPage(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        openAppDetails(context)
        return
    }
    val packageName = context.packageName
    val permissionName = Manifest.permission.RECORD_AUDIO
    val permissionGroup = "android.permission-group.MICROPHONE"
    val intentsToTry = listOf(
        Intent("android.settings.action.MANAGE_APP_PERMISSION").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
            putExtra("android.provider.extra.PERMISSION_NAME", permissionName)
        },
        Intent("android.settings.action.MANAGE_APP_PERMISSION").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
            putExtra("android.provider.extra.PERMISSION_GROUP", permissionGroup)
        },
        Intent("android.settings.action.MANAGE_APP_PERMISSIONS").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
        },
        Intent().apply {
            component = ComponentName("com.android.packageinstaller", "com.android.packageinstaller.permission.ui.ManagePermissionsActivity")
            action = "android.settings.action.MANAGE_APP_PERMISSION"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
            putExtra("android.provider.extra.PERMISSION_NAME", permissionName)
        }
    )
    for (intent in intentsToTry) {
        try {
            context.startActivity(intent)
            return
        } catch (_: Exception) { }
    }
    Toast.makeText(context, "Tap Permissions, then Microphone to change access", Toast.LENGTH_LONG).show()
    openAppDetails(context)
}

private fun getMusicAudioPermission(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    Manifest.permission.READ_MEDIA_AUDIO
} else {
    Manifest.permission.READ_EXTERNAL_STORAGE
}

private fun openMusicAudioPermissionPage(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        openAppDetails(context)
        return
    }
    val packageName = context.packageName
    val permissionName = getMusicAudioPermission()
    val permissionGroup = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        "android.permission-group.AUDIO"
    } else {
        "android.permission-group.STORAGE"
    }
    val intentsToTry = listOf(
        Intent("android.settings.action.MANAGE_APP_PERMISSION").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
            putExtra("android.provider.extra.PERMISSION_NAME", permissionName)
        },
        Intent("android.settings.action.MANAGE_APP_PERMISSION").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
            putExtra("android.provider.extra.PERMISSION_GROUP", permissionGroup)
        },
        Intent("android.settings.action.MANAGE_APP_PERMISSIONS").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
        },
        Intent().apply {
            component = ComponentName("com.android.packageinstaller", "com.android.packageinstaller.permission.ui.ManagePermissionsActivity")
            action = "android.settings.action.MANAGE_APP_PERMISSION"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
            putExtra("android.provider.extra.PERMISSION_NAME", permissionName)
        }
    )
    for (intent in intentsToTry) {
        try {
            context.startActivity(intent)
            return
        } catch (_: Exception) { }
    }
    Toast.makeText(context, "Tap Permissions, then Music and audio to change access", Toast.LENGTH_LONG).show()
    openAppDetails(context)
}

private fun isBatteryOptimizationIgnored(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun isFullScreenIntentGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.USE_FULL_SCREEN_INTENT) == PackageManager.PERMISSION_GRANTED
    }
    return true
}

private fun isMicrophoneGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
}

private fun isMusicAudioGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}
