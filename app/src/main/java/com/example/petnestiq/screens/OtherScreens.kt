package com.example.petnestiq.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petnestiq.data.Message
import com.example.petnestiq.data.MessageType
import com.example.petnestiq.data.MessagePriority
import com.example.petnestiq.service.MessageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen() {
    val messageManager = remember { MessageManager.getInstance() }
    val messages by messageManager.messages.collectAsStateWithLifecycle()
    val unreadCount by messageManager.unreadCount.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("全部", "设备消息", "报警消息")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "消息中心",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row {
                // 未读消息计数
                if (unreadCount > 0) {
                    Badge(
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(text = unreadCount.toString())
                    }
                }

                // 全部已读按钮
                if (unreadCount > 0) {
                    IconButton(
                        onClick = { messageManager.markAllAsRead() }
                    ) {
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = "全部已读",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 选项卡
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 消息列表
        val filteredMessages = when (selectedTab) {
            1 -> messages.filter { it.type == MessageType.DEVICE }
            2 -> messages.filter { it.type == MessageType.ALARM }
            else -> messages
        }

        if (filteredMessages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无消息",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMessages) { message ->
                    MessageItem(
                        message = message,
                        onMessageClick = {
                            if (!message.isRead) {
                                messageManager.markAsRead(message.id)
                            }
                        },
                        onDeleteClick = {
                            messageManager.deleteMessage(message.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    onMessageClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMessageClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isRead)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 消息图标
            Icon(
                imageVector = when (message.type) {
                    MessageType.DEVICE -> Icons.Default.Info
                    MessageType.ALARM -> Icons.Default.Warning
                },
                contentDescription = null,
                tint = when (message.type) {
                    MessageType.DEVICE -> MaterialTheme.colorScheme.primary
                    MessageType.ALARM -> Color(0xFFFF5722)
                },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 消息内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (message.isRead) FontWeight.Normal else FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 优先级指示器
                    if (message.priority != MessagePriority.NORMAL) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (message.priority) {
                                        MessagePriority.LOW -> Color.Gray
                                        MessagePriority.HIGH -> Color(0xFFFF9800)
                                        MessagePriority.URGENT -> Color(0xFFF44336)
                                        else -> Color.Transparent
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (message.isRead) 0.7f else 1f
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.getFormattedTime(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Text(
                        text = message.getTypeDisplayName(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 删除按钮
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ControlScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "控制",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun DataScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "数据",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ProfileScreen() {
    var showDebugOptions by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "我的",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 用户信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "用户昵称",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "智能宠物窝用户",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 功能列表
        ProfileMenuItem(
            title = "设备管理",
            subtitle = "管理您的智能宠物窝设备",
            onClick = { /* TODO: 跳转到设备管理 */ }
        )

        ProfileMenuItem(
            title = "数据统计",
            subtitle = "查看宠物活动和环境数据",
            onClick = { /* TODO: 跳转到数据统计 */ }
        )

        ProfileMenuItem(
            title = "设置中心",
            subtitle = "个人设置和系统偏好",
            onClick = { /* TODO: 跳转到设置 */ }
        )

        ProfileMenuItem(
            title = "调试选项",
            subtitle = "开发者调试和测试功能",
            onClick = { showDebugOptions = true }
        )

        ProfileMenuItem(
            title = "帮助与反馈",
            subtitle = "使用帮助和问题反馈",
            onClick = { /* TODO: 跳转到帮助 */ }
        )

        ProfileMenuItem(
            title = "关于应用",
            subtitle = "应用版本和开发信息",
            onClick = { /* TODO: 跳转到关于 */ }
        )

        // 调试选项对话框
        if (showDebugOptions) {
            DebugOptionsDialog(
                onDismiss = { showDebugOptions = false }
            )
        }
    }
}

@Composable
fun ProfileMenuItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun DebugOptionsDialog(
    onDismiss: () -> Unit
) {
    var selectedDebugTab by remember { mutableStateOf(0) }
    val debugTabs = listOf("消息测试", "系统调试", "网络测试")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "调试选项",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 调试选项卡
                TabRow(
                    selectedTabIndex = selectedDebugTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    debugTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedDebugTab == index,
                            onClick = { selectedDebugTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 调试内容
                when (selectedDebugTab) {
                    0 -> MessageTestPanel()
                    1 -> SystemDebugPanel()
                    2 -> NetworkTestPanel()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageTestPanel() {
    val context = LocalContext.current
    val messageManager = remember { MessageManager.getInstance() }

    var selectedMessageType by remember { mutableStateOf(MessageType.DEVICE) }
    var messageContent by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(MessagePriority.NORMAL) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "消息测试板块",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            // 消息类型选择
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "消息类型",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FilterChip(
                            selected = selectedMessageType == MessageType.DEVICE,
                            onClick = { selectedMessageType = MessageType.DEVICE },
                            label = { Text("设备消息") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        FilterChip(
                            selected = selectedMessageType == MessageType.ALARM,
                            onClick = { selectedMessageType = MessageType.ALARM },
                            label = { Text("报警消息") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        item {
            // 优先级选择
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "消息优先级",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MessagePriority.values().forEach { priority ->
                            FilterChip(
                                selected = selectedPriority == priority,
                                onClick = { selectedPriority = priority },
                                label = {
                                    Text(
                                        text = when (priority) {
                                            MessagePriority.LOW -> "低"
                                            MessagePriority.NORMAL -> "普通"
                                            MessagePriority.HIGH -> "高"
                                            MessagePriority.URGENT -> "紧急"
                                        },
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            // 消息内容输入
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "消息内容",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = messageContent,
                        onValueChange = { messageContent = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("请输入要发送的消息内容...") },
                        minLines = 3,
                        maxLines = 5
                    )
                }
            }
        }

        item {
            // 快速消息模板
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "快速模板",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val templates = if (selectedMessageType == MessageType.DEVICE) {
                        listOf(
                            "设备连接成功",
                            "温度异常，当前温度：30°C",
                            "食物余量不足，剩余：50g",
                            "系统更新完成"
                        )
                    } else {
                        listOf(
                            "紧急停机！设备出现故障",
                            "高温报警！当前温度：45°C",
                            "安全报警：设备门未关闭",
                            "火灾报警！检测到异常高温"
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        templates.forEach { template ->
                            TextButton(
                                onClick = { messageContent = template },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = template,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            // 发送按钮
            Button(
                onClick = {
                    if (messageContent.isNotBlank()) {
                        messageManager.sendTestMessage(
                            type = selectedMessageType,
                            content = messageContent,
                            priority = selectedPriority,
                            context = context
                        )
                        showSuccessMessage = true
                        messageContent = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = messageContent.isNotBlank()
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("发送测试消息")
            }
        }

        // 成功提示
        if (showSuccessMessage) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "✓ 测试消息发送成功！请查看消息界面和系统通知。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(16.dp)
                    )
                }

                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(3000)
                    showSuccessMessage = false
                }
            }
        }
    }
}

@Composable
fun SystemDebugPanel() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "系统调试",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "应用信息",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("版本: 1.0.0", style = MaterialTheme.typography.bodySmall)
                    Text("构建: Debug", style = MaterialTheme.typography.bodySmall)
                    Text("设备: Android", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Button(
                onClick = {
                    MessageManager.getInstance().clearAllMessages()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("清空所有消息")
            }
        }
    }
}

@Composable
fun NetworkTestPanel() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "网络测试",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "网络状态",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("连接状态: 已连接", style = MaterialTheme.typography.bodySmall)
                    Text("网络类型: WiFi", style = MaterialTheme.typography.bodySmall)
                    Text("延迟: 50ms", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Button(
                onClick = { /* TODO: 网络测试 */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("开始网络测试")
            }
        }
    }
}
