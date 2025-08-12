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
    object Message : NavigationItem("message", "消息", Icons.Default.Message)
    object Profile : NavigationItem("profile", "我的", Icons.Default.Person)

    // 详细数据界面路由
    object TemperatureDetail : NavigationItem("temperature_detail", "温度详情", Icons.Default.Thermostat)
    object HumidityDetail : NavigationItem("humidity_detail", "湿度详情", Icons.Default.WaterDrop)
    object FoodDetail : NavigationItem("food_detail", "食物详情", Icons.Default.Restaurant)
    object WaterDetail : NavigationItem("water_detail", "水量详情", Icons.Default.LocalDrink)

    // 用户资料编辑页面
    object UserProfileEdit : NavigationItem("user_profile_edit", "编辑资料", Icons.Default.Edit)

    // 设备管理页面
    object DeviceManagement : NavigationItem("device_management", "设备管理", Icons.Default.Settings)
}

val navigationItems = listOf(
    NavigationItem.Device,
    NavigationItem.Message,
    NavigationItem.Profile
)
