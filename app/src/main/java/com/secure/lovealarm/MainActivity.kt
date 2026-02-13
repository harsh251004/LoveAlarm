package com.secure.lovealarm

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.secure.lovealarm.data.amoledDarkMode
import com.secure.lovealarm.ui.MainTabsScreen
import com.secure.lovealarm.ui.theme.LoveAlarmTheme
import com.secure.lovealarm.viewmodel.AlarmViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            enableEdgeToEdge()
        } catch (e: Exception) {
            Log.w("MainActivity", "Edge-to-edge setup failed", e)
        }
        val alarmViewModel = try {
            ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            ).get(AlarmViewModel::class.java)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to create ViewModel", e)
            null
        }
        if (alarmViewModel == null) {
            setContent {
                LoveAlarmTheme(darkTheme = isSystemInDarkTheme(), amoledDark = false) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Something went wrong. Please restart the app.", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            return
        }
        try {
            setContent {
                val context = LocalContext.current
                var amoledDark by remember { mutableStateOf(false) }
                LaunchedEffect(context) {
                    try {
                        context.amoledDarkMode.collect { amoledDark = it }
                    } catch (_: Exception) { }
                }
                LoveAlarmTheme(
                    darkTheme = isSystemInDarkTheme(),
                    amoledDark = amoledDark,
                    dynamicColor = true
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainTabsScreen(alarmViewModel = alarmViewModel)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "setContent failed", e)
            setContent {
                LoveAlarmTheme(darkTheme = isSystemInDarkTheme(), amoledDark = false) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Something went wrong. Please restart the app.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

