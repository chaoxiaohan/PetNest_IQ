package com.example.petnestiq.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.petnestiq.service.AudioRecordManager
import com.example.petnestiq.R
import com.example.petnestiq.navigation.NavigationItem
import com.example.petnestiq.data.DeviceDataManager
import com.example.petnestiq.data.DataType
import com.example.petnestiq.data.MockDataGenerator
import com.example.petnestiq.data.ChartDataPoint
import com.example.petnestiq.components.VideoStreamPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(navController: NavController? = null) {
    // 获取数据管理器实例
    val deviceDataManager = remember { DeviceDataManager.getInstance() }
    val deviceData by deviceDataManager.deviceData.collectAsStateWithLifecycle()
    val connectionStatus by deviceDataManager.connectionStatus.collectAsStateWithLifecycle()

    // 确保数据同步 - 页面加载时立即同步数据
    LaunchedEffect(Unit) {
        // 清除缓存确保获取最新数据
        MockDataGenerator.clearCache()
        deviceDataManager.updateWithCurrentValues()
    }

    // 每次重组时确保图表数据和当前值使用相同的时间点
    val currentHour = remember { java.time.LocalDateTime.now().hour }
    val currentMinute = remember { java.time.LocalDateTime.now().minute }
    
    // 生成图表数据时使用相同的缓存，确保与详细页面一致
    val temperatureChartData = remember(currentHour) { 
        MockDataGenerator.generate24HourChartData(DataType.TEMPERATURE) 
    }
    val humidityChartData = remember(currentHour) {
        MockDataGenerator.generate24HourChartData(DataType.HUMIDITY)
    }
    val foodChartData = remember(currentHour) {
        MockDataGenerator.generate24HourChartData(DataType.FOOD)
    }
    val waterChartData = remember(currentHour) {
        MockDataGenerator.generate24HourChartData(DataType.WATER)
    }

    // 注意：显示的数值使用MQTT真实数据，图表使用模拟数据保持与详细页面一致

    // 获取当前模拟数据值，确保与图表数据一致
    val currentTemperature = remember(currentHour, currentMinute) {
        MockDataGenerator.getCurrentValue(DataType.TEMPERATURE)
    }
    val currentHumidity = remember(currentHour, currentMinute) {
        MockDataGenerator.getCurrentValue(DataType.HUMIDITY)
    }
    val currentFoodAmount = remember(currentHour, currentMinute) {
        MockDataGenerator.getCurrentValue(DataType.FOOD)
    }
    val currentWaterAmount = remember(currentHour, currentMinute) {
        MockDataGenerator.getCurrentValue(DataType.WATER)
    }

    // 获取Context
    val context = LocalContext.current

    // 添加音频录制管理器
    val audioRecordManager = remember { AudioRecordManager.getInstance() }
    val isRecording by audioRecordManager.isRecording.collectAsStateWithLifecycle()
    val volumeLevel by audioRecordManager.volumeLevel.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current

    // 获取MQTT服务实例
    val mqttService = remember { com.example.petnestiq.service.HuaweiIoTDAMqttService.getInstance() }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 上半部分
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 连接状态卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .padding(bottom = 12.dp)
                    .clickable {
                        // 点击连接状态卡片时尝试重新连接MQTT
                        val mqttService = com.example.petnestiq.service.HuaweiIoTDAMqttService.getInstance()
                        if (connectionStatus == "MQTT连接") {
                            // 如果已连接，先断开再重连
                            mqttService.disconnect()
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(1000) // 等待1秒
                                mqttService.connect(context)
                            }
                        } else {
                            // 如果未连接，直接尝试连接
                            mqttService.connect(context)
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 状态指示点
                    val indicatorColor = when (connectionStatus) {
                        "MQTT连接" -> Color(0xFF4CAF50)  // 绿色表示MQTT连接成功
                        "连接中..." -> Color(0xFFFF9800)  // 橙色表示连接中
                        "连接失败", "连接断开" -> Color(0xFFF44336)  // 红色表示连接失败或断开
                        null -> Color(0xFF9E9E9E)  // 灰色表示未连接
                        else -> Color(0xFF9E9E9E)
                    }

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(indicatorColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = connectionStatus ?: "未连接",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 设备图片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cat),
                        contentDescription = "PetNest 智能猫窝",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 下半部分 - 设备状态
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "设备状态",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 第一行：温度和湿度
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EnvironmentCard(
                    label = "温度",
                    value = "${deviceData.temperature.toInt()}°C", // 使用MQTT真实数据
                    chartData = temperatureChartData, // 使用模拟数据与详细页面保持一致
                    chartColor = Color.Red,
                    modifier = Modifier.weight(1f),
                    onClick = { navController?.navigate(NavigationItem.TemperatureDetail.route) }
                )
                EnvironmentCard(
                    label = "湿度",
                    value = "${deviceData.humidity.toInt()}%", // 使用MQTT真实数据
                    chartData = humidityChartData, // 使用模拟数据与详细页面保持一致
                    chartColor = Color.Blue,
                    modifier = Modifier.weight(1f),
                    onClick = { navController?.navigate(NavigationItem.HumidityDetail.route) }
                )
            }

            // 第二行：食物量和水量
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EnvironmentCard(
                    label = "食物量",
                    value = "${deviceData.foodAmount.toInt()}g", // 使用MQTT真实数据
                    chartData = foodChartData, // 使用模拟数据与详细页面保持一致
                    chartColor = Color.Green,
                    modifier = Modifier.weight(1f),
                    onClick = { navController?.navigate(NavigationItem.FoodDetail.route) }
                )
                EnvironmentCard(
                    label = "水量",
                    value = "${deviceData.waterAmount.toInt()}ml", // 使用MQTT真实数据
                    chartData = waterChartData, // 使用模拟数据与详细页面保持一致
                    chartColor = Color.Cyan,
                    modifier = Modifier.weight(1f),
                    onClick = { navController?.navigate(NavigationItem.WaterDetail.route) }
                )
            }

            // 第三行：通风状态和消毒状态
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SwitchCard(
                    label = "通风开关",
                    checked = deviceData.ventilationStatus,
                    onCheckedChange = { enabled ->
                        // 更新本地状态
                        deviceDataManager.updateVentilationStatus(enabled)
                        // 发送控制指令到设备
                        mqttService.sendVentilationCommand(enabled) { success, message ->
                            if (!success) {
                                // 如果发送失败，回滚本地状态
                                deviceDataManager.updateVentilationStatus(!enabled)
                                android.util.Log.w("DeviceScreen", "通风控制指令发送失败: $message")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                SwitchCard(
                    label = "消毒开关",
                    checked = deviceData.disinfectionStatus,
                    onCheckedChange = { enabled ->
                        // 更新本地状态
                        deviceDataManager.updateDisinfectionStatus(enabled)
                        // 发送控制指令到设备
                        mqttService.sendDisinfectionCommand(enabled) { success, message ->
                            if (!success) {
                                // 如果发送失败，回滚本地状态
                                deviceDataManager.updateDisinfectionStatus(!enabled)
                                android.util.Log.w("DeviceScreen", "消毒控制指令发送失败: $message")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 第四行：加热状态
            HeatingCard(
                enabled = deviceData.heatingStatus,
                onEnabledChange = { enabled ->
                    // 更新本地状态
                    deviceDataManager.updateHeatingStatus(enabled)
                    // 发送控制指令到设备
                    mqttService.sendHeatingCommand(enabled) { success, message ->
                        if (!success) {
                            // 如果发送失败，回滚本地状态
                            deviceDataManager.updateHeatingStatus(!enabled)
                            android.util.Log.w("DeviceScreen", "加热控制指令发送失败: $message")
                        }
                    }
                },
                targetTemperature = deviceData.targetTemperature.toInt(),
                onTemperatureChange = { temperature ->
                    // 更新本地状态
                    deviceDataManager.updateTargetTemperature(temperature.toFloat())
                    // 发送目标温度设置指令到设备
                    mqttService.sendTargetTemperatureCommand(temperature) { success, message ->
                        if (!success) {
                            // 如果发送失败，回滚本地状态
                            deviceDataManager.updateTargetTemperature(deviceData.targetTemperature)
                            android.util.Log.w("DeviceScreen", "目标温度设置指令发送失败: $message")
                        }
                    }
                },
                modifier = Modifier
            )

            // 第五行：云端监控视频模块
            Spacer(modifier = Modifier.height(8.dp))
            CloudVideoMonitorCard(
                modifier = Modifier.fillMaxWidth(),
                context = context,
                hapticFeedback = hapticFeedback,
                audioRecordManager = audioRecordManager,
                isRecording = isRecording,
                volumeLevel = volumeLevel
            )
        }
    }
}

@Composable
fun EnvironmentCard(
    label: String,
    value: String,
    chartData: List<ChartDataPoint>,
    chartColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // 标题
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 数值
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )

            // 显示曲线图
            Spacer(modifier = Modifier.weight(1f))
            MiniChart(
                data = chartData,
                lineColor = chartColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            )
        }
    }
}

@Composable
fun SwitchCard(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)  // 关闭时为灰色状态
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (checked)
                    MaterialTheme.colorScheme.onTertiaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),  // 灰色文字
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
fun HeatingCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    targetTemperature: Int,
    onTemperatureChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 第一行：标题和开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "加热开关：${if (enabled) "开启" else "关闭"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.size(32.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 第二行：温度控制
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (targetTemperature > 10) {
                            onTemperatureChange(targetTemperature - 1)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "降低温度",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = "当前设定温度：${targetTemperature}°C",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )

                IconButton(
                    onClick = {
                        if (targetTemperature < 40) {
                            onTemperatureChange(targetTemperature + 1)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "提高温度",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

// 曲线图组件
@Composable
fun MiniChart(
    data: List<ChartDataPoint>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val padding = 8.dp.toPx()
        val textHeight = 10.dp.toPx()

        // 计算数据范围
        val minValue = data.minOf { it.value }
        val maxValue = data.maxOf { it.value }
        val valueRange = maxValue - minValue

        // 为时间标签留出空间
        val chartHeight = height - textHeight - 4.dp.toPx()

        // 创建路径
        val path = Path()

        data.forEachIndexed { index, point ->
            val x = padding + (width - 2 * padding) * (point.hour / 23f)
            val y = padding + (chartHeight - 2 * padding) * (1 - (point.value - minValue) / valueRange)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // 绘制曲线
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 绘制数据点
        data.forEach { point ->
            val x = padding + (width - 2 * padding) * (point.hour / 23f)
            val y = padding + (chartHeight - 2 * padding) * (1 - (point.value - minValue) / valueRange)

            drawCircle(
                color = lineColor,
                radius = 1.dp.toPx(),
                center = Offset(x, y)
            )
        }

        // 绘制时间轴文字标签
        drawIntoCanvas { canvas ->
            val textPaint = android.graphics.Paint().apply {
                color = Color.Gray.toArgb()
                textSize = 8.sp.toPx()
                isAntiAlias = true
            }

            // 绘制"0:00"标签
            val startX = padding
            val textY = height - 2.dp.toPx()
            textPaint.textAlign = android.graphics.Paint.Align.LEFT
            canvas.nativeCanvas.drawText(
                "0:00",
                startX,
                textY,
                textPaint
            )

            // 绘制"24:00"标签
            val endX = width - padding
            textPaint.textAlign = android.graphics.Paint.Align.RIGHT
            canvas.nativeCanvas.drawText(
                "24:00",
                endX,
                textY,
                textPaint
            )
        }
    }
}

// 云端视频监控状态枚举
enum class VideoStreamStatus {
    LOADING,    // 加载中
    PLAYING,    // 播放中
    PAUSED,     // 暂停
    ERROR,      // 错误
    OFFLINE     // 离线
}

// 视频流数据类
data class VideoStreamInfo(
    val streamUrl: String,
    val resolution: String,
    val quality: String,
    val isLive: Boolean
)

@Composable
fun CloudVideoMonitorCard(
    modifier: Modifier = Modifier,
    context: android.content.Context,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
    audioRecordManager: AudioRecordManager,
    isRecording: Boolean,
    volumeLevel: Float
) {
    var isFullscreen by remember { mutableStateOf(false) }

    // FRP视频流URL
    val streamUrl = remember {
        // 示例FRP代理的RTMP/HTTP视频流地址
        // 请根据你的实际FRP配置修改这个URL
        "http://your-frp-server:port/live/stream"
        // 或者RTSP流: "rtsp://your-frp-server:port/live/stream"
        // 或者HLS流: "http://your-frp-server:port/live/stream.m3u8"
    }

    Column(modifier = modifier) {
        // 实时监控视频标题
        Text(
            text = "实时监控",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 视频播放器组件
        VideoStreamPlayer(
            streamUrl = streamUrl,
            modifier = Modifier.fillMaxWidth(),
            isFullscreen = isFullscreen,
            onFullscreenToggle = { isFullscreen = !isFullscreen }
        )

        // 对讲功能控制区域
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "语音对讲",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 对讲按钮
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isRecording) {
                                audioRecordManager.stopRecording()
                            } else {
                                if (audioRecordManager.hasRecordPermission(context)) {
                                    audioRecordManager.startRecording(context)
                                } else {
                                    android.util.Log.e("DeviceScreen", "没有录音权限")
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isRecording) Color.Red.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isRecording) "停止对讲" else "开始对讲"
                        )
                    }

                    // 音量指示器（录音时显示）
                    if (isRecording) {
                        val animatedVolumeLevel by animateFloatAsState(
                            targetValue = volumeLevel,
                            animationSpec = tween(durationMillis = 100)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "音量:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Canvas(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(16.dp)
                            ) {
                                val barCount = 5
                                val barWidth = size.width / barCount * 0.7f
                                val activeBarCount = (animatedVolumeLevel * barCount).toInt()

                                for (i in 0 until barCount) {
                                    val barColor = if (i < activeBarCount) {
                                        Color.Green
                                    } else {
                                        Color.Gray.copy(alpha = 0.3f)
                                    }
                                    drawRect(
                                        color = barColor,
                                        topLeft = Offset(i * (size.width / barCount), size.height - (i + 1) * (size.height / barCount)),
                                        size = androidx.compose.ui.geometry.Size(barWidth, (i + 1) * (size.height / barCount))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceScreenPreview() {
    DeviceScreen()
}
