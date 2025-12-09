package com.sidharthify.breathe.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sidharthify.breathe.data.AqiResponse
import com.sidharthify.breathe.data.Zone
import com.sidharthify.breathe.ui.components.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    isLoading: Boolean,
    isDarkTheme: Boolean,
    error: String?,
    pinnedZones: List<AqiResponse>,
    zones: List<Zone>,
    onGoToExplore: () -> Unit,
    onRetry: () -> Unit
) {
    var selectedZone by remember { mutableStateOf(pinnedZones.firstOrNull()) }

    LaunchedEffect(pinnedZones) {
        if (selectedZone == null && pinnedZones.isNotEmpty()) {
            selectedZone = pinnedZones.first()
        } else if (pinnedZones.isNotEmpty() && !pinnedZones.any { it.zoneId == selectedZone?.zoneId }) {
            selectedZone = pinnedZones.first()
        }
    }

    if (isLoading && pinnedZones.isEmpty()) {
        LoadingScreen()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                "Pinned Locations",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, top = 32.dp, bottom = 16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            if (pinnedZones.isNotEmpty()) {
                val listState = rememberLazyListState()
                
                LazyRow(
                    state = listState,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pinnedZones, key = { it.zoneId }) { zone ->
                        PinnedMiniCard(
                            zone = zone,
                            isSelected = zone.zoneId == (selectedZone?.zoneId),
                            onClick = { selectedZone = zone }
                        )
                    }
                }
            } else if (error != null) {
                ErrorCard(msg = error, onRetry = onRetry)
            } else {
                EmptyStateCard(onGoToExplore)
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (selectedZone != null) {
            item(key = "dashboard_detail") {
                val provider = remember(selectedZone, zones) {
                    zones.find { it.id == selectedZone!!.zoneId }?.provider
                }
                MainDashboardDetail(selectedZone!!, provider, isDarkTheme)
            }
        }
    }
}