package com.example.petnestiq.data

import java.text.SimpleDateFormat
import java.util.*

// 消息类型枚举
enum class MessageType {
    DEVICE,  // 设备消息
    ALARM    // 报警消息
}

// 消息重要级别
enum class MessagePriority {
    LOW,     // 低
    NORMAL,  // 普通
    HIGH,    // 高
    URGENT   // 紧急
}

// 消息数据类
data class Message(
    val id: String,
    val type: MessageType,
    val title: String,
    val content: String,
    val timestamp: Date,
    val priority: MessagePriority = MessagePriority.NORMAL,
    val isRead: Boolean = false,
    val deviceId: String? = null
) {
    fun getFormattedTime(): String {
        val now = Date()
        val nowCalendar = Calendar.getInstance().apply { time = now }
        val timestampCalendar = Calendar.getInstance().apply { time = timestamp }

        return when {
            // 今天
            isSameDay(timestampCalendar, nowCalendar) -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp)
            }
            // 昨天
            isYesterday(timestampCalendar, nowCalendar) -> {
                "昨天 " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp)
            }
            // 今年
            timestampCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR) -> {
                SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(timestamp)
            }
            // 其他年份
            else -> {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(timestamp)
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(timestampCal: Calendar, nowCal: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            time = nowCal.time
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(timestampCal, yesterday)
    }

    fun getTypeDisplayName(): String {
        return when (type) {
            MessageType.DEVICE -> "设备消息"
            MessageType.ALARM -> "报警消息"
        }
    }

    // 保留但标记为内部使用，避免警告
    @Suppress("unused")
    fun getPriorityDisplayName(): String {
        return when (priority) {
            MessagePriority.LOW -> "低"
            MessagePriority.NORMAL -> "普通"
            MessagePriority.HIGH -> "高"
            MessagePriority.URGENT -> "紧急"
        }
    }
}

// 消息模板
object MessageTemplates {
    // 设备消息模板
    val deviceTemplates = mapOf(
        "connection_lost" to "设备连接丢失，正在尝试重新连接...",
        "connection_restored" to "设备连接已恢复",
        "low_battery" to "设备电量不足，请及时充电",
        "temperature_warning" to "温度异常，当前温度：{temperature}°C",
        "humidity_warning" to "湿度异常，当前湿度：{humidity}%",
        "food_low" to "食物余量不足，剩余：{amount}g",
        "water_low" to "水量不足，剩余：{amount}ml",
        "system_update" to "系统更新完成，版本：{version}",
        "maintenance_reminder" to "设备需要定期维护，已使用{days}天"
    )

    // 报警消息模板
    val alarmTemplates = mapOf(
        "emergency_stop" to "紧急停机！设备出现故障，请立即检查",
        "high_temperature" to "高温报警！当前温度：{temperature}°C，超出安全范围",
        "low_temperature" to "低温报警！当前温度：{temperature}°C，低于安全范围",
        "door_open" to "安全报警：设备门未关闭",
        "motion_detected" to "异常活动检测：在非喂食时间检测到活动",
        "power_failure" to "电源故障：设备失去电源供应",
        "sensor_error" to "传感器错误：{sensor}传感器故障",
        "fire_alarm" to "火灾报警！检测到异常高温或烟雾",
        "intrusion_detected" to "入侵检测：检测到未授权访问"
    )

    fun createDeviceMessage(
        id: String,
        templateKey: String,
        params: Map<String, String> = emptyMap(),
        priority: MessagePriority = MessagePriority.NORMAL,
        deviceId: String? = null
    ): Message? {
        val template = deviceTemplates[templateKey] ?: return null
        var content = template
        params.forEach { (key, value) ->
            content = content.replace("{$key}", value)
        }

        return Message(
            id = id,
            type = MessageType.DEVICE,
            title = "智能宠物窝：设备通知",
            content = content,
            timestamp = Date(),
            priority = priority,
            deviceId = deviceId
        )
    }

    fun createAlarmMessage(
        id: String,
        templateKey: String,
        params: Map<String, String> = emptyMap(),
        priority: MessagePriority = MessagePriority.HIGH,
        deviceId: String? = null
    ): Message? {
        val template = alarmTemplates[templateKey] ?: return null
        var content = template
        params.forEach { (key, value) ->
            content = content.replace("{$key}", value)
        }

        return Message(
            id = id,
            type = MessageType.ALARM,
            title = "智能宠物窝：紧急报警",
            content = content,
            timestamp = Date(),
            priority = priority,
            deviceId = deviceId
        )
    }
}
