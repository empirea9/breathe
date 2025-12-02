package com.sidharthify.breathe

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

// --- Navigation Enum ---
enum class AppScreen(val label: String, val iconFilled: ImageVector, val iconOutlined: ImageVector) {
    Home("Home", Icons.Filled.Home, Icons.Outlined.Home),
    Explore("Explore", Icons.Filled.Search, Icons.Outlined.Search),
    Settings("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

// --- ViewModel ---
class BreatheViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private var allZones = listOf<Zone>()

    fun init(context: Context) {
        viewModelScope.launch {
            try {
                val zonesResp = RetrofitClient.api.getZones()
                allZones = zonesResp.zones

                val prefs = context.getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)
                val pinnedSet = prefs.getStringSet("pinned_ids", emptySet()) ?: emptySet()

                val pinnedAqiList = mutableListOf<AqiResponse>()
                for (id in pinnedSet) {
                    try {
                        pinnedAqiList.add(RetrofitClient.api.getZoneAqi(id))
                    } catch (e: Exception) {
                        // ignore errors for specific pins
                    }
                }

                _uiState.value = UiState.Success(
                    zones = allZones,
                    pinnedZones = pinnedAqiList,
                    pinnedIds = pinnedSet
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to connect: ${e.localizedMessage}")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun togglePin(context: Context, zoneId: String) {
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            val newPinnedIds = currentState.pinnedIds.toMutableSet()
            if (newPinnedIds.contains(zoneId)) {
                newPinnedIds.remove(zoneId)
            } else {
                newPinnedIds.add(zoneId)
            }

            context.getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE)
                .edit().putStringSet("pinned_ids", newPinnedIds).apply()

            init(context)
        }
    }
}

// --- Main Activity ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = dynamicDarkColorScheme(LocalContext.current)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BreatheApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreatheApp(viewModel: BreatheViewModel = viewModel()) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppScreen.values().forEach { screen ->
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                if (currentScreen == screen) screen.iconFilled else screen.iconOutlined, 
                                contentDescription = screen.label
                            ) 
                        },
                        label = { Text(screen.label) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val s = state) {
                is UiState.Loading -> LoadingScreen()
                is UiState.Error -> ErrorScreen(s.message) { viewModel.init(context) }
                is UiState.Success -> {
                    when (currentScreen) {
                        AppScreen.Home -> HomeScreen(
                            pinnedZones = s.pinnedZones,
                            onGoToExplore = { currentScreen = AppScreen.Explore }
                        )
                        AppScreen.Explore -> ExploreScreen(
                            zones = s.zones,
                            pinnedIds = s.pinnedIds,
                            query = viewModel.searchQuery.collectAsState().value,
                            onSearchChange = viewModel::onSearchQueryChanged,
                            onPinToggle = { id -> viewModel.togglePin(context, id) }
                        )
                        AppScreen.Settings -> SettingsScreen()
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------
// SECTION 1: HOME
// ----------------------------------------------------------------
@Composable
fun HomeScreen(
    pinnedZones: List<AqiResponse>,
    onGoToExplore: () -> Unit
) {
    val homeZone = pinnedZones.firstOrNull()

    if (homeZone == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("No Home Zone Selected", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Pin a location in the Explore tab to see detailed stats here.", textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onGoToExplore) {
                Text("Go to Explore")
            }
        }
    } else {
        // Detailed Dashboard
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            // Header
            Text("Your Location", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            Text(homeZone.zoneName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(24.dp))

            // Big AQI Indicator
            val aqiColor = getAqiColor(homeZone.usAqi)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(aqiColor.copy(alpha = 0.3f), aqiColor.copy(alpha = 0.1f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${homeZone.usAqi}",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = aqiColor
                    )
                    Text("US AQI", style = MaterialTheme.typography.titleMedium, color = aqiColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Main Pollutant: ${homeZone.mainPollutant.uppercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Pollutant Breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))

            val pollutants = homeZone.concentrations ?: emptyMap()
            if (pollutants.isEmpty()) {
                Text("No detailed pollutant data available.")
            } else {
                FlowRowGrid(pollutants)
            }
        }
    }
}

@Composable
fun FlowRowGrid(pollutants: Map<String, Double>) {
    val items = pollutants.entries.toList()
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { (key, value) ->
                    PollutantCard(
                        modifier = Modifier.weight(1f),
                        name = formatPollutantName(key),
                        value = "$value",
                        unit = "µg/m³"
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun PollutantCard(modifier: Modifier = Modifier, name: String, value: String, unit: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ----------------------------------------------------------------
// SECTION 2: EXPLORE
// ----------------------------------------------------------------
@Composable
fun ExploreScreen(
    zones: List<Zone>,
    pinnedIds: Set<String>,
    query: String,
    onSearchChange: (String) -> Unit,
    onPinToggle: (String) -> Unit
) {
    val filteredZones = zones.filter {
        it.name.contains(query, ignoreCase = true) || it.id.contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search zones...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = MaterialTheme.shapes.extraLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (filteredZones.isEmpty()) {
                item { Text("No zones found", modifier = Modifier.padding(8.dp)) }
            }
            items(filteredZones) { zone ->
                ZoneListItem(
                    zone = zone,
                    isPinned = pinnedIds.contains(zone.id),
                    onPinClick = { onPinToggle(zone.id) }
                )
            }
        }
    }
}

@Composable
fun ZoneListItem(zone: Zone, isPinned: Boolean, onPinClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isPinned) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(zone.name, style = MaterialTheme.typography.titleMedium)
                Text(zone.provider ?: "Unknown", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onPinClick) {
                Icon(
                    if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "Pin",
                    tint = if(isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ----------------------------------------------------------------
// SECTION 3: SETTINGS
// ----------------------------------------------------------------
@Composable
fun SettingsScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        SettingsItem(title = "App Theme", subtitle = "Uses system default (Material You)")
        SettingsItem(title = "Data Provider", subtitle = "Central Pollution Control Board (CPCB) & OpenWeatherMap")
        SettingsItem(title = "Version", subtitle = "1.0.0 Alpha")
        
        Spacer(modifier = Modifier.weight(1f))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "This app is for educational purposes. Data may be delayed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

// --- Utils ---

@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(msg: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Error", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
            Text(msg, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

fun formatPollutantName(key: String): String {
    return when(key.lowercase()) {
        "pm2_5", "pm2.5" -> "PM2.5"
        "pm10" -> "PM10"
        "no2" -> "NO₂"
        "so2" -> "SO₂"
        "co" -> "CO"
        "o3" -> "O₃"
        "nh3" -> "NH₃"
        else -> key.uppercase()
    }
}

fun getAqiColor(aqi: Int): Color {
    return when (aqi) {
        in 0..50 -> Color(0xFF00E400) // Green
        in 51..100 -> Color(0xFFFFFF00) // Yellow
        in 101..150 -> Color(0xFFFF7E00) // Orange
        in 151..200 -> Color(0xFFFF0000) // Red
        in 201..300 -> Color(0xFF8F3F97) // Purple
        else -> Color(0xFF7E0023) // Maroon
    }
}