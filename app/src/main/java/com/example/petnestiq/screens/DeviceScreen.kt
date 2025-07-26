package com.example.petnestiq.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petnestiq.R
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
        // 模拟温度变化：夜间较低，白天较高
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
fun DeviceScreen() {
    // 连接状态 - 可以是 Connected, Disconnected, null
    var connectionStatus by remember { mutableStateOf<String?>(null) }
    var ventilationEnabled by remember { mutableStateOf(true) }
    var disinfectionEnabled by remember { mutableStateOf(false) }
    var heatingEnabled by remember { mutableStateOf(false) }
    var targetTemperature by remember { mutableStateOf(25) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 上半部分 - 固定高度而不是比例
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 连接状态卡片 - 缩小并居中
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .padding(bottom = 12.dp),
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
                        "Connected" -> Color(0xFF4CAF50)
                        "Disconnected" -> Color(0xFFF44336)
                        else -> Color(0xFFF44336)
                    }

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(indicatorColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "连接状态：${connectionStatus ?: "null"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 设备图片区域
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
                    value = "25°C",
                    chartData = generateTemperatureData(),
                    chartColor = Color.Red,
                    modifier = Modifier.weight(1f)
                )
                EnvironmentCard(
                    label = "湿度",
                    value = "60%",
                    chartData = generateHumidityData(),
                    chartColor = Color.Blue,
                    modifier = Modifier.weight(1f)
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
                    value = "500g",
                    chartData = generateFoodData(),
                    chartColor = Color.Green,
                    modifier = Modifier.weight(1f)
                )
                EnvironmentCard(
                    label = "水量",
                    value = "500ml",
                    chartData = generateWaterData(),
                    chartColor = Color.Cyan,
                    modifier = Modifier.weight(1f)
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
                    checked = ventilationEnabled,
                    onCheckedChange = { ventilationEnabled = it },
                    modifier = Modifier.weight(1f)
                )
                SwitchCard(
                    label = "消毒开关",
                    checked = disinfectionEnabled,
                    onCheckedChange = { disinfectionEnabled = it },
                    modifier = Modifier.weight(1f)
                )
            }

            // 第四行：加热状态（独占一行）
            HeatingCard(
                enabled = heatingEnabled,
                onEnabledChange = { heatingEnabled = it },
                targetTemperature = targetTemperature,
                onTemperatureChange = { targetTemperature = it },
                modifier = Modifier
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(120.dp),
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
            // 标题在左上角，字号较大
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

            // 第三行显示曲线图
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
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
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
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
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

@Preview(showBackground = true)
@Composable
fun DeviceScreenPreview() {
    DeviceScreen()
}