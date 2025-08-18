package com.example.petnestiq.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.petnestiq.screens.*
import com.example.petnestiq.data.DataType
/*
* 通过底部导航栏切换主页面，通过路由跳转到详细页面，所有页面都由 Compose 组件实现
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavigationItem.Device.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 300 },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -300 },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable(NavigationItem.Device.route) {
                DeviceScreen(navController = navController)
            }
            composable(NavigationItem.Message.route) {
                MessageScreen(navController = navController)
            }
            composable(NavigationItem.Profile.route) {
                ProfileScreen(navController = navController)
            }
            // AI聊天页面
            composable(NavigationItem.AiChat.route) {
                AiChatScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            // 详细数据界面
            composable(NavigationItem.TemperatureDetail.route) {
                DetailScreen(
                    dataType = DataType.TEMPERATURE,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(NavigationItem.HumidityDetail.route) {
                DetailScreen(
                    dataType = DataType.HUMIDITY,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(NavigationItem.FoodDetail.route) {
                DetailScreen(
                    dataType = DataType.FOOD,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(NavigationItem.WaterDetail.route) {
                DetailScreen(
                    dataType = DataType.WATER,
                    onBackClick = { navController.popBackStack() }
                )
            }
            // 用户资料编辑页面
            composable(NavigationItem.UserProfileEdit.route) {
                UserProfileEditScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            // 设备管理页面
            composable(NavigationItem.DeviceManagement.route) {
                DeviceManagementScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            // 设置中心页面
            composable(NavigationItem.SettingsCenter.route) {
                SettingsCenterScreen(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToFeedback = {
                        navController.navigate(NavigationItem.UsageFeedback.route)
                    }
                )
            }
            // 使用与反馈页面
            composable(NavigationItem.UsageFeedback.route) {
                UsageFeedbackScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 只在主要界面显示底部导航栏
    val showBottomBar = currentRoute in listOf(
        NavigationItem.Device.route,
        NavigationItem.Message.route,
        NavigationItem.Profile.route
    )

    if (showBottomBar) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            navigationItems.forEach { item ->
                val isSelected = currentRoute == item.route

                NavigationBarItem(
                    icon = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(
                                top = if (isSelected) 6.dp else 12.dp,
                                bottom = 6.dp
                            )
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )

                            // 只在选中状态下显示文字
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    selected = isSelected,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(NavigationItem.Device.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}
