package com.example.petnestiq.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.petnestiq.R
import com.example.petnestiq.data.Message
import com.example.petnestiq.data.MessagePriority
import com.example.petnestiq.data.MessageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MessageManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: MessageManager? = null

        fun getInstance(): MessageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MessageManager().also { INSTANCE = it }
            }
        }

        // 通知渠道ID
        const val DEVICE_CHANNEL_ID = "device_messages"
        const val ALARM_CHANNEL_ID = "alarm_messages"
    }

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // 添加消息
    fun addMessage(message: Message) {
        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(0, message) // 添加到列表开头
        _messages.value = currentMessages
        updateUnreadCount()
    }

    // 标记消息为已读
    fun markAsRead(messageId: String) {
        val currentMessages = _messages.value.toMutableList()
        val index = currentMessages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            currentMessages[index] = currentMessages[index].copy(isRead = true)
            _messages.value = currentMessages
            updateUnreadCount()
        }
    }

    // 标记所有消息为已读
    fun markAllAsRead() {
        val currentMessages = _messages.value.map { it.copy(isRead = true) }
        _messages.value = currentMessages
        updateUnreadCount()
    }

    // 删除消息
    fun deleteMessage(messageId: String) {
        val currentMessages = _messages.value.toMutableList()
        currentMessages.removeAll { it.id == messageId }
        _messages.value = currentMessages
        updateUnreadCount()
    }

    // 清空所有消息
    fun clearAllMessages() {
        _messages.value = emptyList()
        updateUnreadCount()
    }

    // 根据类型获取消息
    fun getMessagesByType(type: MessageType): List<Message> {
        return _messages.value.filter { it.type == type }
    }

    // 获取未读消息
    fun getUnreadMessages(): List<Message> {
        return _messages.value.filter { !it.isRead }
    }

    private fun updateUnreadCount() {
        _unreadCount.value = _messages.value.count { !it.isRead }
    }

    // 创建并发送测试消息
    fun sendTestMessage(
        type: MessageType,
        content: String,
        priority: MessagePriority = MessagePriority.NORMAL,
        context: Context? = null
    ) {
        val message = Message(
            id = UUID.randomUUID().toString(),
            type = type,
            title = when (type) {
                MessageType.DEVICE -> "智能宠物窝：设备测试消息"
                MessageType.ALARM -> "智能宠物窝：报警测试消息"
            },
            content = content,
            timestamp = java.time.LocalDateTime.now(),
            priority = priority
        )

        addMessage(message)

        // 发送系统通知
        context?.let {
            NotificationService.sendNotification(it, message)
        }
    }

    // 发送正常的设备消息
    fun sendDeviceMessage(
        content: String,
        priority: MessagePriority = MessagePriority.NORMAL,
        context: Context? = null,
        deviceId: String? = null
    ) {
        val message = Message(
            id = UUID.randomUUID().toString(),
            type = MessageType.DEVICE,
            title = "智能宠物窝：设备通知",
            content = content,
            timestamp = java.time.LocalDateTime.now(),
            priority = priority,
            deviceId = deviceId
        )

        addMessage(message)

        // 发送系统通知
        context?.let {
            NotificationService.sendNotification(it, message)
        }
    }

    // 发送正常的报警消息
    fun sendAlarmMessage(
        content: String,
        priority: MessagePriority = MessagePriority.HIGH,
        context: Context? = null,
        deviceId: String? = null
    ) {
        val message = Message(
            id = UUID.randomUUID().toString(),
            type = MessageType.ALARM,
            title = "智能宠物窝：紧急报警",
            content = content,
            timestamp = java.time.LocalDateTime.now(),
            priority = priority,
            deviceId = deviceId
        )

        addMessage(message)

        // 发送系统通知
        context?.let {
            NotificationService.sendNotification(it, message)
        }
    }

    // 使用预定义模板发送设备消息
    fun sendDeviceMessageFromTemplate(
        templateKey: String,
        params: Map<String, String> = emptyMap(),
        priority: MessagePriority = MessagePriority.NORMAL,
        context: Context? = null,
        deviceId: String? = null
    ) {
        val templateMessage = com.example.petnestiq.data.MessageTemplates.createDeviceMessage(
            id = UUID.randomUUID().toString(),
            templateKey = templateKey,
            params = params,
            priority = priority,
            deviceId = deviceId
        )

        templateMessage?.let { message ->
            // 更新标题格式
            val updatedMessage = message.copy(title = "智能宠物窝：设备通知")
            addMessage(updatedMessage)

            // 发送系统通知
            context?.let {
                NotificationService.sendNotification(it, updatedMessage)
            }
        }
    }

    // 使用预定义模板发送报警消息
    fun sendAlarmMessageFromTemplate(
        templateKey: String,
        params: Map<String, String> = emptyMap(),
        priority: MessagePriority = MessagePriority.HIGH,
        context: Context? = null,
        deviceId: String? = null
    ) {
        val templateMessage = com.example.petnestiq.data.MessageTemplates.createAlarmMessage(
            id = UUID.randomUUID().toString(),
            templateKey = templateKey,
            params = params,
            priority = priority,
            deviceId = deviceId
        )

        templateMessage?.let { message ->
            // 更新标题格式
            val updatedMessage = message.copy(title = "智能宠物窝：紧急报警")
            addMessage(updatedMessage)

            // 发送系统通知
            context?.let {
                NotificationService.sendNotification(it, updatedMessage)
            }
        }
    }
}

object NotificationService {

    // 创建通知渠道
    fun createNotificationChannels(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 设备消息渠道
        val deviceChannel = NotificationChannel(
            MessageManager.DEVICE_CHANNEL_ID,
            "设备消息",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "来自智能宠物窝的设备状态消息"
        }

        // 报警消息渠道
        val alarmChannel = NotificationChannel(
            MessageManager.ALARM_CHANNEL_ID,
            "报警消息",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "来自智能宠物窝的紧急报警消息"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 1000, 500, 1000)
        }

        notificationManager.createNotificationChannel(deviceChannel)
        notificationManager.createNotificationChannel(alarmChannel)
    }

    // 发送通知
    fun sendNotification(context: Context, message: Message) {
        val channelId = when (message.type) {
            MessageType.DEVICE -> MessageManager.DEVICE_CHANNEL_ID
            MessageType.ALARM -> MessageManager.ALARM_CHANNEL_ID
        }

        val priority = when (message.priority) {
            MessagePriority.LOW -> NotificationCompat.PRIORITY_LOW
            MessagePriority.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
            MessagePriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            MessagePriority.URGENT -> NotificationCompat.PRIORITY_MAX
        }

        // 检测是否为MIUI系统
        val isMIUI = isMIUISystem()

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 统一使用应用前景图标
            .setContentTitle(message.title)
            .setContentText(message.content)
            .setPriority(priority)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.content))
            .apply {
                // 根据消息类型设置不同的行为
                when (message.type) {
                    MessageType.ALARM -> {
                        setDefaults(NotificationCompat.DEFAULT_ALL)
                        setVibrate(longArrayOf(0, 1000, 500, 1000))
                    }
                    MessageType.DEVICE -> {
                        setDefaults(NotificationCompat.DEFAULT_SOUND)
                    }
                }

                // 设置大图标显示cat图标（仅在支持的情况下）
                try {
                    setLargeIcon(getBitmapFromDrawable(context, R.drawable.cat))
                } catch (e: Exception) {
                    // 如果cat图标不存在，则不设置大图标
                    e.printStackTrace()
                }
            }
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            // 检查通知权限
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(message.id.hashCode(), notification)
            }
        } catch (e: SecurityException) {
            // 处理通知权限问题
            e.printStackTrace()
        }
    }

    // 检测是否为MIUI系统
    private fun isMIUISystem(): Boolean {
        return try {
            val prop = Class.forName("android.os.SystemProperties")
            val method = prop.getMethod("get", String::class.java)
            val miui = method.invoke(null, "ro.miui.ui.version.name") as String
            miui.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    // 将Drawable转换为Bitmap
    private fun getBitmapFromDrawable(context: Context, drawableId: Int): android.graphics.Bitmap? {
        return try {
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, drawableId)
            drawable?.let {
                val bitmap = android.graphics.Bitmap.createBitmap(
                    it.intrinsicWidth,
                    it.intrinsicHeight,
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)
                it.setBounds(0, 0, canvas.width, canvas.height)
                it.draw(canvas)
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    // 检查并请求通知权限
    fun checkNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
}
