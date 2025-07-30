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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petnestiq.R
import com.example.petnestiq.navigation.NavigationItem
import com.example.petnestiq.data.DeviceDataManager
import com.example.petnestiq.data.DataType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

// 数据类定义
data class ChartDataPoint(
    val hour: Int,  // 0-24小时
    val value: Float
)

// 生成模拟数据的函数
fun generateTemperatureData(): List<ChartDataPoint> {
    return (0..23).map { hour ->
        // 模拟温度变化
        val baseTemp = 20f + 8f * sin((hour - 6) * Math.PI / 12).toFloat() + Random.nextFloat() * 3f
        ChartDataPoint(hour, baseTemp.coerceIn(15f, 35f))
    }
}

fun generateHumidityData(): List<ChartDataPoint> {
    return (0..23).map { hour ->
        // 模拟湿度变化：相对稳定，有小幅波动
        val baseHumidity = 60f + 15f * sin((hour - 3) * Math.PI / 12).toFloat() + Random.nextFloat() * 10f
        ChartDataPoint(hour, baseHumidity.coerceIn(40f, 85f))
    }
}

fun generateFoodData(): List<ChartDataPoint> {
    return (0..23).map { hour ->
        // 模拟食物量变化：逐渐减少，定时补充
        val baseFood = if (hour % 8 == 0) 500f else 500f - (hour % 8) * 50f + Random.nextFloat() * 20f
        ChartDataPoint(hour, baseFood.coerceIn(0f, 500f))
    }
}

fun generateWaterData(): List<ChartDataPoint> {
    return (0..23).map { hour ->
        // 模拟水量变化：逐渐减少，定时补充
        val baseWater = if (hour % 6 == 0) 500f else 500f - (hour % 6) * 70f + Random.nextFloat() * 30f
        ChartDataPoint(hour, baseWater.coerceIn(0f, 500f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(navController: NavController? = null) {
    // 获取数据管理器实例
    val deviceDataManager = remember { DeviceDataManager.getInstance() }
    val deviceData by deviceDataManager.deviceData.collectAsStateWithLifecycle()
    val connectionStatus by deviceDataManager.connectionStatus.collectAsStateWithLifecycle()

    // 获取Context
    val context = LocalContext.current

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
                    value = "${deviceData.temperature.toInt()}°C",
                    chartData = generateTemperatureData(),
                    chartColor = Color.Red,
                    modifier = Modifier.weight(1f),
                    onClick = { navController?.navigate(NavigationItem.TemperatureDetail.route) }
                )
                EnvironmentCard(
                    label = "湿度",
                    value = "${deviceData.humidity.toInt()}%",
                    chartData = generateHumidityData(),
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
                    value = "${deviceData.foodAmount.toInt()}g",
                    chartData = generateFoodData(),
                    chartColor = Color.Green,
                    modifier = Modifier.weight(1f),
                    onClick = { navController?.navigate(NavigationItem.FoodDetail.route) }
                )
                EnvironmentCard(
                    label = "水量",
                    value = "${deviceData.waterAmount.toInt()}ml",
                    chartData = generateWaterData(),
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
                modifier = Modifier.fillMaxWidth()
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
    modifier: Modifier = Modifier
) {
    var videoStatus by remember { mutableStateOf(VideoStreamStatus.OFFLINE) }
    var isPlaying by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.height(300.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "云端监控",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // 实时状态指示器
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (videoStatus) {
                                    VideoStreamStatus.PLAYING -> Color(0xFF4CAF50)
                                    VideoStreamStatus.LOADING -> Color(0xFFFF9800)
                                    VideoStreamStatus.ERROR -> Color(0xFFF44336)
                                    else -> Color.Gray
                                }
                            )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = when (videoStatus) {
                            VideoStreamStatus.PLAYING -> "直播中"
                            VideoStreamStatus.LOADING -> "连接中"
                            VideoStreamStatus.PAUSED -> "已暂停"
                            VideoStreamStatus.ERROR -> "连接失败"
                            VideoStreamStatus.OFFLINE -> "离线"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // 设置按钮
                IconButton(
                    onClick = { showSettings = !showSettings },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 视频播放区域
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (videoStatus) {
                        VideoStreamStatus.OFFLINE, VideoStreamStatus.ERROR -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "重新连接",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (videoStatus == VideoStreamStatus.ERROR) "连接失败" else "点击连接",
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        VideoStreamStatus.LOADING -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "正��连接...",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        VideoStreamStatus.PLAYING, VideoStreamStatus.PAUSED -> {
                            // TODO: 这里将来集成实际的视频播放器
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                                    .clickable {
                                        isPlaying = !isPlaying
                                        videoStatus = if (isPlaying) VideoStreamStatus.PLAYING else VideoStreamStatus.PAUSED
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (videoStatus == VideoStreamStatus.PAUSED) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "播放",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }

                                // 模拟视频内容（占位符）
                                Text(
                                    text = "实时监控画面",
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 控制按钮区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 播放/暂停按钮
                IconButton(
                    onClick = {
                        when (videoStatus) {
                            VideoStreamStatus.OFFLINE, VideoStreamStatus.ERROR -> {
                                videoStatus = VideoStreamStatus.LOADING
                                // TODO: 启动视频流连接
                                // 模拟连接延迟
                                kotlin.concurrent.thread {
                                    Thread.sleep(2000)
                                    videoStatus = VideoStreamStatus.PLAYING
                                    isPlaying = true
                                }
                            }
                            VideoStreamStatus.PLAYING -> {
                                videoStatus = VideoStreamStatus.PAUSED
                                isPlaying = false
                            }
                            VideoStreamStatus.PAUSED -> {
                                videoStatus = VideoStreamStatus.PLAYING
                                isPlaying = true
                            }
                            else -> {}
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        when (videoStatus) {
                            VideoStreamStatus.PLAYING -> Icons.Default.Pause
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 刷新按钮
                IconButton(
                    onClick = {
                        videoStatus = VideoStreamStatus.LOADING
                        // TODO: 重新连接视频流
                        kotlin.concurrent.thread {
                            Thread.sleep(1500)
                            videoStatus = VideoStreamStatus.PLAYING
                            isPlaying = true
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "刷新",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 视频信息显示
            if (videoStatus == VideoStreamStatus.PLAYING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "1080P",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "延迟: 200ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
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