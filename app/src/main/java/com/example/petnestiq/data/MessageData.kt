package com.example.petnestiq.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    val timestamp: LocalDateTime,
    val priority: MessagePriority = MessagePriority.NORMAL,
    val isRead: Boolean = false,
    val deviceId: String? = null
) {
    fun getFormattedTime(): String {
        val now = LocalDateTime.now()
        val formatter = when {
            timestamp.toLocalDate() == now.toLocalDate() -> {
                DateTimeFormatter.ofPattern("HH:mm")
            }
            timestamp.toLocalDate() == now.toLocalDate().minusDays(1) -> {
                DateTimeFormatter.ofPattern("昨天 HH:mm")
            }
            timestamp.year == now.year -> {
                DateTimeFormatter.ofPattern("MM-dd HH:mm")
            }
            else -> {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            }
        }
        return timestamp.format(formatter)
    }

    fun getTypeDisplayName(): String {
        return when (type) {
            MessageType.DEVICE -> "设备消息"
            MessageType.ALARM -> "报警消息"
        }
    }

    fun getPriorityDisplayName(): String {
        return when (priority) {
            MessagePriority.LOW -> "低"
            MessagePriority.NORMAL -> "普通"
            MessagePriority.HIGH -> "高"
            MessagePriority.URGENT -> "紧急"
        }
    }
}

// 预定义的消息模板
object MessageTemplates {

    // 设备消息模板
    val deviceTemplates = mapOf(
        "connection_lost" to "设备连接断开，请检查网络连接",
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
            title = "设备通知",
            content = content,
            timestamp = LocalDateTime.now(),
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
            title = "报警通知",
            content = content,
            timestamp = LocalDateTime.now(),
            priority = priority,
            deviceId = deviceId
        )
    }
}
