package com.secure.lovealarm.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.secure.lovealarm.data.Alarm
import com.secure.lovealarm.viewmodel.AlarmViewModel
import kotlinx.coroutines.launch

enum class ClockTab {
    Alarm, Clock, Timer, Stopwatch
}

private const val TRANSITION_DURATION = 300

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabsScreen(alarmViewModel: AlarmViewModel) {
    val navController = rememberNavController()
    val tabs = remember { ClockTab.values() }
    val pagerState = rememberPagerState(pageCount = { tabs.size }, initialPage = 0)
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    fun scrollToAlarmPage() {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    val slideInFromRight = {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(TRANSITION_DURATION)
        )
    }
    val slideOutToRight = {
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(TRANSITION_DURATION)
        )
    }
    val slideOutToLeft = {
        slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(TRANSITION_DURATION)
        )
    }
    val slideInFromLeft = {
        slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(TRANSITION_DURATION)
        )
    }

    NavHost(
        navController = navController,
        startDestination = "tabs",
        modifier = Modifier.fillMaxSize()
    ) {
        composable(
            "tabs",
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() }
        ) {
            TabsContent(
                alarmViewModel = alarmViewModel,
                pagerState = pagerState,
                coroutineScope = coroutineScope,
                tabs = tabs,
                onAddAlarm = { navController.navigate("addAlarm") },
                onEditAlarm = { alarm -> navController.navigate("editAlarm/${alarm.id}") },
                onSettings = { navController.navigate("settings") },
                scrollToAlarmPage = { scrollToAlarmPage() }
            )
        }
        composable(
            "addAlarm",
            enterTransition = { slideInFromRight() },
            popExitTransition = { slideOutToRight() }
        ) {
            CreateAlarmScreen(
                alarm = null,
                onSave = { alarm ->
                    alarmViewModel.addAlarm(alarm)
                    scrollToAlarmPage()
                    navController.popBackStack()
                },
                onCancel = {
                    scrollToAlarmPage()
                    navController.popBackStack()
                },
                onDelete = null
            )
        }
        composable(
            route = "editAlarm/{alarmId}",
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType }),
            enterTransition = { slideInFromRight() },
            popExitTransition = { slideOutToRight() }
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: 0L
            val alarms by alarmViewModel.alarms.collectAsState()
            val alarm = remember(alarmId, alarms) { alarms.find { it.id == alarmId } }
            CreateAlarmScreen(
                alarm = alarm,
                onSave = { updated ->
                    alarmViewModel.updateAlarm(updated)
                    scrollToAlarmPage()
                    navController.popBackStack()
                },
                onCancel = {
                    scrollToAlarmPage()
                    navController.popBackStack()
                },
                onDelete = { toDelete ->
                    alarmViewModel.deleteAlarm(toDelete)
                    scrollToAlarmPage()
                    navController.popBackStack()
                }
            )
        }
        composable(
            "settings",
            enterTransition = { slideInFromRight() },
            popExitTransition = { slideOutToRight() }
        ) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabsContent(
    alarmViewModel: AlarmViewModel,
    pagerState: androidx.compose.foundation.pager.PagerState,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    tabs: Array<ClockTab>,
    onAddAlarm: () -> Unit,
    onEditAlarm: (Alarm) -> Unit,
    onSettings: () -> Unit,
    scrollToAlarmPage: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val gradientColors = if (isDark) {
        listOf(
            Color(0xFF1A0E14),
            Color(0xFF2D1520),
            Color(0xFF3D1F2E)
        )
    } else {
        listOf(
            Color(0xFFFFF0F5),
            Color(0xFFFFE4EC),
            Color(0xFFFFB6C1)
        )
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .height(56.dp)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onSettings,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            },
            bottomBar = {
                fun selectionForTab(currentPage: Int, pageOffset: Float, tabIndex: Int): Float {
                    return when {
                        currentPage == tabIndex -> (1f - 2 * kotlin.math.abs(pageOffset)).coerceIn(0f, 1f)
                        currentPage == tabIndex - 1 -> (-2 * pageOffset).coerceIn(0f, 1f)
                        currentPage == tabIndex + 1 -> (2 * pageOffset).coerceIn(0f, 1f)
                        else -> 0f
                    }
                }
                NavigationBar {
                    NavigationBarItem(
                        icon = {
                            val selection by remember {
                                derivedStateOf {
                                    selectionForTab(
                                        pagerState.currentPage,
                                        pagerState.currentPageOffsetFraction,
                                        0
                                    )
                                }
                            }
                            val scale = 1f + 0.3f * selection
                            val rotation = -12f * (1f - selection)
                            Icon(
                                Icons.Default.Alarm,
                                contentDescription = "Alarm",
                                modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    rotationZ = rotation
                                }
                            )
                        },
                        label = { Text("Alarm") },
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            val selection by remember {
                                derivedStateOf {
                                    selectionForTab(
                                        pagerState.currentPage,
                                        pagerState.currentPageOffsetFraction,
                                        1
                                    )
                                }
                            }
                            val scale = 1f + 0.3f * selection
                            val rotation = -12f * (1f - selection)
                            Icon(
                                Icons.Default.Public,
                                contentDescription = "Clock",
                                modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    rotationZ = rotation
                                }
                            )
                        },
                        label = { Text("Clock") },
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            val selection by remember {
                                derivedStateOf {
                                    selectionForTab(
                                        pagerState.currentPage,
                                        pagerState.currentPageOffsetFraction,
                                        2
                                    )
                                }
                            }
                            val scale = 1f + 0.3f * selection
                            val rotation = -12f * (1f - selection)
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = "Timer",
                                modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    rotationZ = rotation
                                }
                            )
                        },
                        label = { Text("Timer") },
                        selected = pagerState.currentPage == 2,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            val selection by remember {
                                derivedStateOf {
                                    selectionForTab(
                                        pagerState.currentPage,
                                        pagerState.currentPageOffsetFraction,
                                        3
                                    )
                                }
                            }
                            val scale = 1f + 0.3f * selection
                            val rotation = -12f * (1f - selection)
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = "Stopwatch",
                                modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    rotationZ = rotation
                                }
                            )
                        },
                        label = { Text("Stopwatch") },
                        selected = pagerState.currentPage == 3,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(3)
                            }
                        }
                    )
                }
            }
        ) { padding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(padding),
                beyondViewportPageCount = 1,
                userScrollEnabled = true
            ) { page ->
                key(tabs[page]) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                clip = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (tabs[page]) {
                            ClockTab.Alarm -> AlarmListScreen(
                                viewModel = alarmViewModel,
                                onAddAlarm = onAddAlarm,
                                onAlarmClick = onEditAlarm
                            )
                            ClockTab.Clock -> WorldClockScreen()
                            ClockTab.Timer -> TimerScreen()
                            ClockTab.Stopwatch -> StopwatchScreen()
                        }
                    }
                }
            }
        }
    }
}
