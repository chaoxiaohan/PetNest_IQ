package com.example.petnestiq.data

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.*
import kotlin.random.Random

/**
 * 统一的模拟数据生成器
 * 确保设备页面和详细页面的数据一致性
 */
object MockDataGenerator {

    // 数据缓存，确保同一时间点的数据一致
    private val dataCache = mutableMapOf<String, Float>()

    /**
     * 生成温度数据
     * 模拟真实的温度变化规律：
     * - 白天（6:00-18:00）温度较高
     * - 夜晚（18:00-6:00）温度较低
     * - 有合理的随机波动
     * - 温度范围在24-30度之间
     */
    fun generateTemperatureValue(hour: Int, minute: Int = 0): Float {
        val timeKey = "temp_${hour}_${minute}"

        return dataCache.getOrPut(timeKey) {
            // 基础温度曲线：使用正弦函数模拟日夜温差，调整为24-30度范围
            val baseTemp = 27f + 3f * sin((hour - 6) * PI / 12).toFloat()

            // 添加小幅随机波动 (±0.8°C)
            val noise = (Random.nextFloat() - 0.5f) * 1.6f

            // 确保温度在合理范围内24-30度
            (baseTemp + noise).coerceIn(24f, 30f)
        }
    }

    /**
     * 生成湿度数据
     * 模拟真实的湿度变化规律：
     * - 早晨湿度较高
     * - 中午湿度较低
     * - 傍晚湿度回升
     */
    fun generateHumidityValue(hour: Int, minute: Int = 0): Float {
        val timeKey = "humidity_${hour}_${minute}"

        return dataCache.getOrPut(timeKey) {
            // 基础湿度曲线：早晚高，中午低
            val baseHumidity = 65f + 12f * cos((hour - 2) * PI / 12).toFloat()

            // 添加随机波动 (±3%)
            val noise = (Random.nextFloat() - 0.5f) * 6f

            // 确保湿度在合理范围内
            (baseHumidity + noise).coerceIn(45f, 80f)
        }
    }

    /**
     * 生成食物量数据
     * 模拟真实的宠物饮食规律：
     * - 三餐时间：7:00, 13:00, 19:00 食物量高
     * - 凌晨和深夜（0:00-6:00, 22:00-23:59）饮食量非常小
     * - 其他时间饮食量适中
     */
    fun generateFoodValue(hour: Int, minute: Int = 0): Float {
        val timeKey = "food_${hour}_${minute}"

        return dataCache.getOrPut(timeKey) {
            val currentTime = hour * 60 + minute

            // 三餐时间点：7:00, 13:00, 19:00
            val mealTimes = listOf(7 * 60, 13 * 60, 19 * 60)

            // 找到最近的用餐时间
            val lastMealTime = mealTimes.lastOrNull { it <= currentTime }
                ?: (mealTimes.last() - 24 * 60) // 如果是早上7点前，则是昨天19点

            // 计算距离上次用餐的时间（分钟）
            val timeSinceMeal = if (lastMealTime < 0) {
                currentTime + (24 * 60 + lastMealTime)
            } else {
                currentTime - lastMealTime
            }

            val maxAmount = 400f
            val timeHours = timeSinceMeal / 60f

            // 根据时间段调整食物基础量和消耗率
            val (baseMultiplier, consumptionRate) = when (hour) {
                // 凌晨和深夜：饮食量非常小
                in 0..5, in 22..23 -> Pair(0.1f, 0.05f)
                // 三餐时间前后1小时：饮食量高
                in 6..8, in 12..14, in 18..20 -> Pair(1.0f, 0.3f)
                // 其他时间：饮食量适中
                else -> Pair(0.4f, 0.2f)
            }

            // 使用指数衰减模拟食物消耗
            val adjustedConsumptionRate = consumptionRate + Random.nextFloat() * 0.1f
            val baseRemaining = maxAmount * baseMultiplier * exp(-adjustedConsumptionRate * timeHours)

            // 在用餐时间点附近，食物量会增加
            val mealBonus = if (mealTimes.any { abs(currentTime - it) <= 30 }) { // 用餐前后30分钟
                Random.nextFloat() * 100f + 50f
            } else 0f

            // 添加随机波动
            val randomVariation = (Random.nextFloat() - 0.5f) * 20f

            val currentAmount = baseRemaining + mealBonus + randomVariation

            currentAmount.coerceIn(0f, maxAmount)
        }
    }

    /**
     * 生成水量数据
     * 模拟真实的宠物饮水规律：
     * - 活跃时间（6:00-9:00, 16:00-20:00）饮水量高
     * - 凌晨和深夜（0:00-5:00, 21:00-23:59）饮水量非常小
     * - 其他时间饮水量适中
     * - 受温度影响
     */
    fun generateWaterValue(hour: Int, minute: Int = 0): Float {
        val timeKey = "water_${hour}_${minute}"

        return dataCache.getOrPut(timeKey) {
            val currentTime = hour * 60 + minute

            // 主要饮水时间点：8:00, 16:00
            val drinkTimes = listOf(8 * 60, 16 * 60)

            // 找到最近的饮水时间
            val lastDrinkTime = drinkTimes.lastOrNull { it <= currentTime }
                ?: (drinkTimes.last() - 24 * 60) // 如果是早上8点前，则是昨天16点

            // 计算距离上次主要饮水的时间（分钟）
            val timeSinceDrink = if (lastDrinkTime < 0) {
                currentTime + (24 * 60 + lastDrinkTime)
            } else {
                currentTime - lastDrinkTime
            }

            val maxAmount = 350f
            val timeHours = timeSinceDrink / 60f

            // 获取当前温度影响
            val currentTemp = generateTemperatureValue(hour, minute)
            val tempFactor = 1f + (currentTemp - 27f) * 0.08f // 温度每高1度，需水量增加8%

            // 根据时间段调整水量基础量和消耗率
            val (baseMultiplier, consumptionRate) = when (hour) {
                // 凌晨和深夜：饮水量非常小
                in 0..5, in 21..23 -> Pair(0.15f, 0.02f)
                // 活跃时间：饮水量高
                in 6..9, in 16..20 -> Pair(1.0f, 0.4f)
                // 中午高温时间：饮水量较高
                in 10..15 -> Pair(0.8f, 0.5f)
                else -> Pair(0.5f, 0.3f)
            }

            // 使用指数衰减模拟水量消耗，考虑温度因素
            val adjustedConsumptionRate = (consumptionRate * tempFactor) + Random.nextFloat() * 0.15f
            val baseRemaining = maxAmount * baseMultiplier * exp(-adjustedConsumptionRate * timeHours)

            // 在主要饮水时间点附近，水量会增加
            val drinkBonus = if (drinkTimes.any { abs(currentTime - it) <= 45 }) { // 饮水前后45分钟
                Random.nextFloat() * 80f + 40f
            } else 0f

            // 温度相关的额外消耗/补充
            val tempAdjustment = when {
                currentTemp > 28f -> -(Random.nextFloat() * 15f) // 高温时额外消耗
                currentTemp < 25f -> Random.nextFloat() * 8f    // 低温时消耗减少
                else -> 0f
            }

            // 添加随机波动
            val randomVariation = (Random.nextFloat() - 0.5f) * 25f

            val currentAmount = baseRemaining + drinkBonus + tempAdjustment + randomVariation

            currentAmount.coerceIn(0f, maxAmount)
        }
    }

    /**
     * 清除数据缓存（用于刷新数据）
     */
    fun clearCache() {
        dataCache.clear()
    }

    /**
     * 获取当前时间的实时数据
     */
    fun getCurrentValue(dataType: DataType): Float {
        val now = LocalDateTime.now()
        return when (dataType) {
            DataType.TEMPERATURE -> generateTemperatureValue(now.hour, now.minute)
            DataType.HUMIDITY -> generateHumidityValue(now.hour, now.minute)
            DataType.FOOD -> generateFoodValue(now.hour, now.minute)
            DataType.WATER -> generateWaterValue(now.hour, now.minute)
        }
    }

    /**
     * 生成24小时的图表数据（设备页面用）
     */
    fun generate24HourChartData(dataType: DataType): List<ChartDataPoint> {
        return (0..23).map { hour ->
            val value = when (dataType) {
                DataType.TEMPERATURE -> generateTemperatureValue(hour)
                DataType.HUMIDITY -> generateHumidityValue(hour)
                DataType.FOOD -> generateFoodValue(hour)
                DataType.WATER -> generateWaterValue(hour)
            }
            ChartDataPoint(hour, value)
        }
    }

    /**
     * 生成详细数据（详细页面用）
     */
    fun generateDetailData(dataType: DataType, date: LocalDate): List<DetailDataPoint> {
        val data = mutableListOf<DetailDataPoint>()

        // 每15分钟生成一个数据点
        for (hour in 0..23) {
            for (minute in 0 until 60 step 15) {
                val value = when (dataType) {
                    DataType.TEMPERATURE -> generateTemperatureValue(hour, minute)
                    DataType.HUMIDITY -> generateHumidityValue(hour, minute)
                    DataType.FOOD -> generateFoodValue(hour, minute)
                    DataType.WATER -> generateWaterValue(hour, minute)
                }

                data.add(
                    DetailDataPoint(
                        hour = hour,
                        minute = minute,
                        value = value,
                        timestamp = String.format("%02d:%02d", hour, minute)
                    )
                )
            }
        }

        return data
    }
}

// 数据点类（从DeviceScreen.kt移动到这里）
data class ChartDataPoint(
    val hour: Int,
    val value: Float
)

// 详细数据点类（从DetailScreen.kt移动到这里）
data class DetailDataPoint(
    val hour: Int,
    val minute: Int,
    val value: Float,
    val timestamp: String
)
