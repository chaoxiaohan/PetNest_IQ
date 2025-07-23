package com.example.petnestiq.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavigationItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Device : NavigationItem("device", "设备", Icons.Default.Devices)
    object Control : NavigationItem("control", "控制", Icons.Default.Settings)
    object Data : NavigationItem("data", "数据", Icons.Default.Analytics)
    object Message : NavigationItem("message", "消息", Icons.Default.Message)
    object Profile : NavigationItem("profile", "我的", Icons.Default.Person)
}

val navigationItems = listOf(
    NavigationItem.Device,
    NavigationItem.Control,
    NavigationItem.Data,
    NavigationItem.Message,
    NavigationItem.Profile
)
