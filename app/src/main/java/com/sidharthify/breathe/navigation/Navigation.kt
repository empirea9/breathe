package com.sidharthify.breathe.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes.Companion.Puffy
import androidx.compose.material3.MaterialShapes.Companion.Clover4Leaf
import androidx.compose.material3.MaterialShapes.Companion.Slanted
import androidx.compose.material3.MaterialShapes.Companion.SoftBurst
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.graphics.shapes.RoundedPolygon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
enum class AppScreen(
    val label: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector,
    val shape: RoundedPolygon
) {
    Home("Home", Icons.Filled.Home, Icons.Outlined.Home, shape = Puffy),
    Map("Map", Icons.Filled.Map, Icons.Outlined.Map, shape = Slanted),
    Explore("Explore", Icons.Filled.Search, Icons.Outlined.Search, shape = Clover4Leaf),
    Settings("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, shape = SoftBurst)
}