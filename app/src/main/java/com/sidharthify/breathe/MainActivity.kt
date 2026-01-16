package com.sidharthify.breathe

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sidharthify.breathe.navigation.AppScreen
import com.sidharthify.breathe.ui.screens.ExploreScreen
import com.sidharthify.breathe.ui.screens.HomeScreen
import com.sidharthify.breathe.ui.screens.MapScreen
import com.sidharthify.breathe.ui.screens.SettingsScreen
import com.sidharthify.breathe.viewmodel.BreatheViewModel
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val ctx = applicationContext
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

        val prefs = getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)

        setContent {
            val systemDark = isSystemInDarkTheme()
            val isDarkThemeState = remember {
                val savedValue = if (prefs.contains("is_dark_theme")) {
                    prefs.getBoolean("is_dark_theme", false)
                } else {
                    systemDark
                }
                mutableStateOf(savedValue)
            }

            val toggleTheme: () -> Unit = {
                val newValue = !isDarkThemeState.value
                isDarkThemeState.value = newValue
                prefs.edit().putBoolean("is_dark_theme", newValue).apply()
            }

            // Internal Theme Wrapper
            BreatheTheme(darkTheme = isDarkThemeState.value) {
                BreatheApp(
                    isDarkTheme = isDarkThemeState.value,
                    onThemeToggle = toggleTheme
                )
            }
        }
    }
}

@Composable
fun BreatheApp(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    viewModel: BreatheViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var currentScreen by remember { mutableStateOf(AppScreen.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppScreen.entries.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (isSelected) screen.iconFilled else screen.iconOutlined,
                                contentDescription = screen.label
                            )
                        },
                        label = { 
                            Text(
                                screen.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        selected = isSelected,
                        onClick = { currentScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AnimatedContent(
                targetState = currentScreen,
                label = "ScreenTransition",
                transitionSpec = {
                    (fadeIn(animationSpec = tween(150)) + 
                     scaleIn(initialScale = 0.95f, animationSpec = tween(150))) 
                    .togetherWith(
                        fadeOut(animationSpec = tween(150))
                    )
                }
            ) { screen ->
                when (screen) {
                    AppScreen.Home -> HomeScreen(
                        isLoading = uiState.isLoading,
                        isDarkTheme = isDarkTheme,
                        error = uiState.error,
                        pinnedZones = uiState.pinnedZones,
                        zones = uiState.zones,
                        onGoToExplore = { currentScreen = AppScreen.Explore },
                        onRetry = { viewModel.refreshData(context) }
                    )

                    AppScreen.Map -> MapScreen(
                        zones = uiState.zones,
                        allAqiData = uiState.allAqiData,
                        pinnedIds = uiState.pinnedIds,
                        isDarkTheme = isDarkTheme,
                        onPinToggle = { id -> viewModel.togglePin(context, id) }
                    )

                    AppScreen.Explore -> ExploreScreen(
                        isLoading = uiState.isLoading,
                        isDarkTheme = isDarkTheme,
                        error = uiState.error,
                        zones = uiState.zones,
                        pinnedIds = uiState.pinnedIds,
                        query = searchQuery,
                        onSearchChange = viewModel::onSearchQueryChanged,
                        onPinToggle = { id -> viewModel.togglePin(context, id) },
                        onRetry = { viewModel.refreshData(context) }
                    )

                    AppScreen.Settings -> SettingsScreen(
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = onThemeToggle,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun BreatheTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}