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
fun UsageFeedbackScreen(
    onBackClick: () -> Unit
) {
    var feedbackText by remember { mutableStateOf("") }
    var selectedFeedbackType by remember { mutableStateOf("功能建议") }
    var showSubmitDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部标题栏
        TopAppBar(
            title = {
                Text(
                    text = "使用与反馈",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 使用帮助部分
            item {
                FeedbackSection(title = "使用帮助") {
                    HelpItem(
                        icon = Icons.Default.DeviceHub,
                        title = "设备连接",
                        description = "如何连接和配置智能宠物窝设备",
                        onClick = { /* TODO: 打开帮助详情 */ }
                    )

                    Divider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    HelpItem(
                        icon = Icons.Default.Notifications,
                        title = "通知设置",
                        description = "如何设置和管理设备通知",
                        onClick = { /* TODO: 打开帮助详情 */ }
                    )

                    Divider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    HelpItem(
                        icon = Icons.Default.DataUsage,
                        title = "数据查看",
                        description = "如何查看和分析宠物数据",
                        onClick = { /* TODO: 打开帮助详情 */ }
                    )

                    Divider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    HelpItem(
                        icon = Icons.Default.SmartToy,
                        title = "AI助手",
                        description = "如何使用AI助手功能",
                        onClick = { /* TODO: 打开帮助详情 */ }
                    )
                }
            }

            // 常见问题部分
            item {
                FeedbackSection(title = "常见问题") {
                    FAQItem(
                        question = "设备无法连接网络怎么办？",
                        answer = "请检查网络设置，确保设备在WiFi覆盖范围内，并重启设备重试"
                    )

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    FAQItem(
                        question = "数据显示不准确怎么办？",
                        answer = "请确保传感器清洁，设备放置位置合适，必要时可以重新校准设备"
                    )

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    FAQItem(
                        question = "如何设置自动喂食？",
                        answer = "在设备管理页面中找到自动喂食选项，设置喂食时间和分量即可"
                    )
                }
            }

            // 意见反馈部分
            item {
                FeedbackSection(title = "意见反馈") {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 反馈类型选择
                        Text(
                            text = "反馈类型",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FeedbackTypeChip(
                                text = "功能建议",
                                selected = selectedFeedbackType == "功能建议",
                                onClick = { selectedFeedbackType = "功能建议" }
                            )
                            FeedbackTypeChip(
                                text = "问题反馈",
                                selected = selectedFeedbackType == "问题反馈",
                                onClick = { selectedFeedbackType = "问题反馈" }
                            )
                            FeedbackTypeChip(
                                text = "其他",
                                selected = selectedFeedbackType == "其他",
                                onClick = { selectedFeedbackType = "其他" }
                            )
                        }

                        // 反馈内容输入
                        Text(
                            text = "反馈内容",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        OutlinedTextField(
                            value = feedbackText,
                            onValueChange = { feedbackText = it },
                            placeholder = { Text("请详细描述您的意见或建议...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5
                        )

                        // 提交按钮
                        Button(
                            onClick = { showSubmitDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = feedbackText.isNotBlank()
                        ) {
                            Text("提交反馈")
                        }
                    }
                }
            }

            // 联系我们部分
            item {
                FeedbackSection(title = "联系我们") {
                    ContactItem(
                        icon = Icons.Default.Email,
                        title = "邮箱联系",
                        content = "support@petnestiq.com",
                        onClick = { /* TODO: 打开邮箱应用 */ }
                    )

                    Divider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    ContactItem(
                        icon = Icons.Default.Phone,
                        title = "客服热线",
                        content = "400-123-4567",
                        onClick = { /* TODO: 拨打电话 */ }
                    )

                    Divider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    ContactItem(
                        icon = Icons.Default.Forum,
                        title = "在线客服",
                        content = "工作日 9:00-18:00",
                        onClick = { /* TODO: 打开客服对话 */ }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // 提交成功对话框
    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("反馈提交成功") },
            text = { Text("感谢您的反馈，我们会认真处理您的意见和建议！") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSubmitDialog = false
                        feedbackText = ""
                    }
                ) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
private fun FeedbackSection(
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
private fun HelpItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
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
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun FAQItem(
    question: String,
    answer: String
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = question,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = answer,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun FeedbackTypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = { Text(text) },
        selected = selected
    )
}

@Composable
private fun ContactItem(
    icon: ImageVector,
    title: String,
    content: String,
    onClick: () -> Unit
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
                text = content,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
