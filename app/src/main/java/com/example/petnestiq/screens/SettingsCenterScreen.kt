package com.example.petnestiq.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCenterScreen(
    onBackClick: () -> Unit,
    onNavigateToFeedback: () -> Unit = {}
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var autoFeedEnabled by remember { mutableStateOf(false) }
    var temperatureUnit by remember { mutableStateOf("摄氏度") }
    var dataSync by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部标题栏
        TopAppBar(
            title = {
                Text(
                    text = "设置中心",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 通知设置分组
            item {
                SettingsGroup(title = "通知设置") {
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = "推送通知",
                        subtitle = "接收设备状态变化通知",
                        trailing = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { notificationsEnabled = it }
                            )
                        }
                    )
                }
            }

            // 设备设置分组
            item {
                SettingsGroup(title = "设备设置") {
                    SettingsItem(
                        icon = Icons.Default.Restaurant,
                        title = "自动喂食",
                        subtitle = "按时间自动投食",
                        trailing = {
                            Switch(
                                checked = autoFeedEnabled,
                                onCheckedChange = { autoFeedEnabled = it }
                            )
                        }
                    )

                    Divider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    SettingsItem(
                        icon = Icons.Default.Thermostat,
                        title = "温度单位",
                        subtitle = temperatureUnit,
                        trailing = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        onClick = {
                            temperatureUnit = if (temperatureUnit == "摄氏度") "华氏度" else "摄氏度"
                        }
                    )
                }
            }

            // 数据管理分组
            item {
                SettingsGroup(title = "数据管理") {
                    SettingsItem(
                        icon = Icons.Default.Sync,
                        title = "数据同步",
                        subtitle = "自动同步设备数据到云端",
                        trailing = {
                            Switch(
                                checked = dataSync,
                                onCheckedChange = { dataSync = it }
                            )
                        }
                    )

                    Divider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    SettingsItem(
                        icon = Icons.Default.Storage,
                        title = "清除缓存",
                        subtitle = "清理本地缓存数据",
                        trailing = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        onClick = {
                            // TODO: 实现清除缓存功能
                        }
                    )
                }
            }

            // 关于应用分组
            item {
                SettingsGroup(title = "关于应用") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "应用版本",
                        subtitle = "V3.1.0",
                        trailing = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    )

                    Divider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    SettingsItem(
                        icon = Icons.Default.Feedback,
                        title = "使用与反馈",
                        subtitle = "意见建议和使用帮助",
                        trailing = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        onClick = onNavigateToFeedback
                    )

                    Divider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "隐私政策",
                        subtitle = "查看隐私保护条款",
                        trailing = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit = {},
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        trailing()
    }
}
