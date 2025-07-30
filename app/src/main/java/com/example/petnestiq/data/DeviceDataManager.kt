package com.example.petnestiq.data

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

// 设备数据类
data class DeviceData(
    val temperature: Float,
    val humidity: Float,
    val foodAmount: Float,
    val waterAmount: Float,
    val ventilationStatus: Boolean = false,
    val disinfectionStatus: Boolean = false,
    val heatingStatus: Boolean = false,
    val targetTemperature: Float = 25f,
    val lastUpdateTime: String = getCurrentTime()
)

// 获取当前时间
private fun getCurrentTime(): String {
    val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return formatter.format(java.util.Date())
}

// 设备数据管理器（单例模式）
class DeviceDataManager private constructor() {

    // 使用StateFlow管理数据状态
    private val _deviceData = MutableStateFlow(
        DeviceData(
            temperature = 25f,
            humidity = 70f,
            foodAmount = 500f,
            waterAmount = 500f
        )
    )
    val deviceData: StateFlow<DeviceData> = _deviceData.asStateFlow()

    // 连接状态
    private val _connectionStatus = MutableStateFlow<String?>(null)
    val connectionStatus: StateFlow<String?> = _connectionStatus.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: DeviceDataManager? = null

        fun getInstance(): DeviceDataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DeviceDataManager().also { INSTANCE = it }
            }
        }
    }

    // 更新设备数据
    fun updateDeviceData(newData: DeviceData) {
        _deviceData.value = newData.copy(lastUpdateTime = getCurrentTime())
    }

    // 更新单个数据项
    fun updateTemperature(temperature: Float) {
        _deviceData.value = _deviceData.value.copy(
            temperature = temperature,
            lastUpdateTime = getCurrentTime()
        )
    }

    fun updateHumidity(humidity: Float) {
        _deviceData.value = _deviceData.value.copy(
            humidity = humidity,
            lastUpdateTime = getCurrentTime()
        )
    }

    fun updateFoodAmount(amount: Float) {
        _deviceData.value = _deviceData.value.copy(
            foodAmount = amount,
            lastUpdateTime = getCurrentTime()
        )
    }

    fun updateWaterAmount(amount: Float) {
        _deviceData.value = _deviceData.value.copy(
            waterAmount = amount,
            lastUpdateTime = getCurrentTime()
        )
    }

    // 更新设备状态
    fun updateVentilationStatus(status: Boolean) {
        _deviceData.value = _deviceData.value.copy(
            ventilationStatus = status,
            lastUpdateTime = getCurrentTime()
        )
    }

    fun updateDisinfectionStatus(status: Boolean) {
        _deviceData.value = _deviceData.value.copy(
            disinfectionStatus = status,
            lastUpdateTime = getCurrentTime()
        )
    }

    fun updateHeatingStatus(status: Boolean) {
        _deviceData.value = _deviceData.value.copy(
            heatingStatus = status,
            lastUpdateTime = getCurrentTime()
        )
    }

    fun updateTargetTemperature(temperature: Float) {
        _deviceData.value = _deviceData.value.copy(
            targetTemperature = temperature,
            lastUpdateTime = getCurrentTime()
        )
    }

    // 更新连接状态
    fun updateConnectionStatus(status: String?) {
        _connectionStatus.value = status
    }

    // 模拟数据更新（用于测试）
//    fun simulateDataUpdate() {
//        val currentData = _deviceData.value
//        val newData = currentData.copy(
//            temperature = (currentData.temperature + (Random.nextFloat() - 0.5f) * 2).coerceIn(15f, 35f),
//            humidity = (currentData.humidity + (Random.nextFloat() - 0.5f) * 5).coerceIn(40f, 85f),
//            foodAmount = maxOf(0f, currentData.foodAmount - Random.nextFloat() * 10),
//            waterAmount = maxOf(0f, currentData.waterAmount - Random.nextFloat() * 15),
//            lastUpdateTime = getCurrentTime()
//        )
//        _deviceData.value = newData
//    }

    // 获取指定数据类型的当前值
    fun getCurrentValue(dataType: DataType): Float {
        return when (dataType) {
            DataType.TEMPERATURE -> _deviceData.value.temperature
            DataType.HUMIDITY -> _deviceData.value.humidity
            DataType.FOOD -> _deviceData.value.foodAmount
            DataType.WATER -> _deviceData.value.waterAmount
        }
    }

//    // 获取格式化的数据值
//    fun getFormattedValue(dataType: DataType): String {
//        val value = getCurrentValue(dataType)
//        return "${value.toInt()}${dataType.unit}"
//    }
}

// 数据类型枚举（移动到这里以便共享）
enum class DataType(val title: String, val unit: String, val color: androidx.compose.ui.graphics.Color) {
    TEMPERATURE("温度", "°C", androidx.compose.ui.graphics.Color.Red),
    HUMIDITY("湿度", "%", androidx.compose.ui.graphics.Color.Blue),
    FOOD("食物量", "g", androidx.compose.ui.graphics.Color.Green),
    WATER("水量", "ml", androidx.compose.ui.graphics.Color.Cyan)
}
