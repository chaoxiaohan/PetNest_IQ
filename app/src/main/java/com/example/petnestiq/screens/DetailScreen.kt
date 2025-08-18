package com.example.petnestiq.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petnestiq.data.DeviceDataManager
import com.example.petnestiq.data.DataType
import com.example.petnestiq.data.MockDataGenerator
import com.example.petnestiq.data.DetailDataPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// 今日概览数据
data class DailySummary(
    val maxValue: Float,
    val minValue: Float,
    val avgValue: Float,
    val maxTime: String,
    val minTime: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    dataType: DataType,
    onBackClick: () -> Unit
) {
    // 获取数据管理器实例
    val deviceDataManager = remember { DeviceDataManager.getInstance() }
    val deviceData by deviceDataManager.deviceData.collectAsStateWithLifecycle()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // 使用统一的模拟数据生成器
    val detailData = remember(selectedDate, dataType) {
        MockDataGenerator.generateDetailData(dataType, selectedDate)
    }

    // 计算今日概览
    val dailySummary = remember(detailData) {
        calculateDailySummary(detailData)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 顶部导航栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${dataType.title}详情",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 日期选择器
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "选择日期: ${selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "选择日期",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 当前数据显示
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = when (dataType) {
                        DataType.TEMPERATURE -> "${deviceData.temperature.toInt()}${dataType.unit}"
                        DataType.HUMIDITY -> "${deviceData.humidity.toInt()}${dataType.unit}"
                        DataType.FOOD -> "${deviceData.foodAmount.toInt()}${dataType.unit}"
                        DataType.WATER -> "${deviceData.waterAmount.toInt()}${dataType.unit}"
                    },
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = dataType.color
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "更新时间: ${deviceData.lastUpdateTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 折线图
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "24小时趋势图",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                DetailChart(
                    data = detailData,
                    lineColor = dataType.color,
                    unit = dataType.unit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 今日概览
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "今日概览",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryItem(
                        label = "最高",
                        value = "${dailySummary.maxValue.toInt()}${dataType.unit}",
                        time = dailySummary.maxTime,
                        color = Color(0xFFE57373)
                    )

                    SummaryItem(
                        label = "最低",
                        value = "${dailySummary.minValue.toInt()}${dataType.unit}",
                        time = dailySummary.minTime,
                        color = Color(0xFF64B5F6)
                    )

                    SummaryItem(
                        label = "平均",
                        value = "${dailySummary.avgValue.toInt()}${dataType.unit}",
                        time = "全天",
                        color = Color(0xFF81C784)
                    )
                }
            }
        }
    }

    // 日期选择器弹窗
    if (showDatePicker) {
        CustomDatePickerDialog(
            onDateSelected = { date ->
                selectedDate = date
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun SummaryItem(
    label: String,
    value: String,
    time: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )

        Text(
            text = time,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun DetailChart(
    data: List<DetailDataPoint>,
    lineColor: Color,
    unit: String,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val padding = 32.dp.toPx()

        // 计算数据范围
        val minValue = data.minOf { it.value }
        val maxValue = data.maxOf { it.value }
        val valueRange = if (maxValue > minValue) maxValue - minValue else 1f

        // 计算图表区域
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding

        // 创建路径
        val path = Path()

        data.forEachIndexed { index, point ->
            val x = padding + chartWidth * (point.hour + point.minute / 60f) / 24f
            val y = padding + chartHeight * (1 - (point.value - minValue) / valueRange)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // 绘制网格线
        drawIntoCanvas { canvas ->
            val gridPaint = android.graphics.Paint().apply {
                color = Color.Gray.copy(alpha = 0.3f).toArgb()
                strokeWidth = 1.dp.toPx()
            }

            // 垂直网格线 (时间)
            for (hour in 0..24 step 6) {
                val x = padding + chartWidth * hour / 24f
                canvas.nativeCanvas.drawLine(x, padding, x, height - padding, gridPaint)
            }

            // 水平网格线 (数值)
            for (i in 0..4) {
                val y = padding + chartHeight * i / 4f
                canvas.nativeCanvas.drawLine(padding, y, width - padding, y, gridPaint)
            }
        }

        // 绘制曲线
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 绘制数据点
        data.forEach { point ->
            val x = padding + chartWidth * (point.hour + point.minute / 60f) / 24f
            val y = padding + chartHeight * (1 - (point.value - minValue) / valueRange)

            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }

        // 绘制坐标轴标签
        drawIntoCanvas { canvas ->
            val textPaint = android.graphics.Paint().apply {
                color = textColor.toArgb()
                textSize = 10.sp.toPx()
                isAntiAlias = true
            }

            // X轴时间标签
            for (hour in 0..24 step 6) {
                val x = padding + chartWidth * hour / 24f
                val y = height - 8.dp.toPx()
                textPaint.textAlign = android.graphics.Paint.Align.CENTER
                canvas.nativeCanvas.drawText("${hour}:00", x, y, textPaint)
            }

            // Y轴数值标签
            for (i in 0..4) {
                val value = minValue + (maxValue - minValue) * (4 - i) / 4f
                val x = 8.dp.toPx()
                val y = padding + chartHeight * i / 4f + 4.dp.toPx()
                textPaint.textAlign = android.graphics.Paint.Align.LEFT
                canvas.nativeCanvas.drawText("${value.toInt()}$unit", x, y, textPaint)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(date)
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

// 计算今日概览
fun calculateDailySummary(data: List<DetailDataPoint>): DailySummary {
    if (data.isEmpty()) {
        return DailySummary(0f, 0f, 0f, "", "")
    }

    val maxPoint = data.maxByOrNull { it.value }!!
    val minPoint = data.minByOrNull { it.value }!!
    val avgValue = data.map { it.value }.average().toFloat()

    return DailySummary(
        maxValue = maxPoint.value,
        minValue = minPoint.value,
        avgValue = avgValue,
        maxTime = maxPoint.timestamp,
        minTime = minPoint.timestamp
    )
}
