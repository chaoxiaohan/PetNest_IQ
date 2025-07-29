package com.example.petnestiq.service

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.petnestiq.data.DeviceData
import com.example.petnestiq.data.DeviceDataManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * 华为云IOTDA平台MQTT服务
 */
class HuaweiIoTDAMqttService private constructor() {

    companion object {
        private const val TAG = "HuaweiIoTDAMqttService"

        @Volatile
        private var INSTANCE: HuaweiIoTDAMqttService? = null

        fun getInstance(): HuaweiIoTDAMqttService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HuaweiIoTDAMqttService().also { INSTANCE = it }
            }
        }
    }

    // MQTT连接配置
    data class MqttConfig(
        val serverUri: String,          // MQTT服务器地址
        val deviceId: String,           // 设备ID
        val deviceSecret: String = "",  // 设备密钥（如果使用密钥生成密码）
        val clientId: String,           // 客户端ID
        val username: String,           // 用户名
        val password: String,           // 密码
        val port: Int = 8883           // 端口号
    )

    // MQTT客户端
    private var mqttClient: MqttClient? = null
    private var currentConfig: MqttConfig? = null
    private val deviceDataManager = DeviceDataManager.getInstance()
    private val gson = Gson()

    // 连接状态
    private var isConnected = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 调试数据收集
    private val _debugMessages = MutableStateFlow<List<String>>(emptyList())
    val debugMessages: StateFlow<List<String>> = _debugMessages.asStateFlow()

    private val _lastReceivedData = MutableStateFlow<String?>(null)
    val lastReceivedData: StateFlow<String?> = _lastReceivedData.asStateFlow()

    private val _lastSentCommand = MutableStateFlow<String?>(null)
    val lastSentCommand: StateFlow<String?> = _lastSentCommand.asStateFlow()

    // 上一次发送的设备状态，用于判断是否需要下发指令
    private val lastSentStates = AtomicReference<DeviceData?>(null)

    // 添加上下文引用用于显示Toast
    private var appContext: Context? = null

    // 命令响应回调映射
    private val commandCallbacks = mutableMapOf<String, (Boolean, String?) -> Unit>()

    /**
     * 配置MQTT连接信息
     */
    fun configure(config: MqttConfig) {
        this.currentConfig = config
    }

    /**
     * 连接到华为云IOTDA平台
     */
    fun connect(context: Context) {
        val config = currentConfig ?: run {
            Log.e(TAG, "MQTT配置未设置")
            deviceDataManager.updateConnectionStatus("配置错误")
            return
        }

        appContext = context // 保存上下文引用

        serviceScope.launch {
            try {
                disconnect() // 先断开已有连接

                Log.i(TAG, "开始连接MQTT服务器: ${config.serverUri}")
                deviceDataManager.updateConnectionStatus("连接中...")

                // 使用配置中的密码，如果为空则生成密码
                val password = if (config.password.isNotEmpty()) {
                    config.password
                } else {
                    generatePassword(config.deviceSecret)
                }

                // 创建MQTT客户端
                mqttClient = MqttClient(config.serverUri, config.clientId, MemoryPersistence())

                // 配置连接选项
                val options = MqttConnectOptions().apply {
                    userName = config.username
                    this.password = password.toCharArray()
                    isCleanSession = true
                    connectionTimeout = 30
                    keepAliveInterval = 60
                    isAutomaticReconnect = true
                }

                // 设置回调
                mqttClient?.setCallback(createMqttCallback())

                // 连接
                mqttClient?.connect(options)

                // 订阅多个topic来接收设备数据
                subscribeToTopics(config.deviceId)

                isConnected = true
                Log.i(TAG, "MQTT连接成功")
                deviceDataManager.updateConnectionStatus("MQTT连接")

                // 开始定期获取设备状态
                startDataPolling()

                // 移除监控设备状态变化并下发指令的功能，因为不需要上报数据给MQTT平台
                // startCommandMonitoring()

            } catch (e: Exception) {
                Log.e(TAG, "MQTT连接失败", e)
                isConnected = false
                deviceDataManager.updateConnectionStatus("连接失败")
            }
        }
    }

    /**
     * 订阅所有相关的MQTT Topic
     */
    private fun subscribeToTopics(deviceId: String) {
        try {
            val topics = listOf(
                // 设备影子相关
                "\$oc/devices/$deviceId/sys/shadow/get/response",
                "\$oc/devices/$deviceId/sys/shadow/update/response",

                // 设备属性上报
                "\$oc/devices/$deviceId/sys/properties/report",

                // 设备消息上报
                "\$oc/devices/$deviceId/sys/messages/up",

                // 设备事件上报
                "\$oc/devices/$deviceId/sys/events/up",

                // 命令响应
                "\$oc/devices/$deviceId/sys/commands/response",

                // 通用数据上报topic（根据华为云IoTDA的实际配置）
                "devices/$deviceId/data",
                "data/$deviceId",
                "$deviceId/data",

                // 可能的自定义topic
                "topic/$deviceId/data",
                "iot/$deviceId/data"
            )

            topics.forEach { topic ->
                try {
                    mqttClient?.subscribe(topic, 1)
                    Log.i(TAG, "订阅Topic: $topic")
                    addDebugMessage("订阅Topic: $topic")
                } catch (e: Exception) {
                    Log.w(TAG, "订阅Topic失败: $topic", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "批量订阅Topic失败", e)
        }
    }

    /**
     * 断开MQTT连接
     */
    fun disconnect() {
        try {
            if (mqttClient?.isConnected == true) {
                mqttClient?.disconnect()
            }
            mqttClient?.close()
            mqttClient = null
            isConnected = false
            Log.i(TAG, "MQTT连接已断开")
            deviceDataManager.updateConnectionStatus(null)
        } catch (e: Exception) {
            Log.e(TAG, "断开MQTT连接时出错", e)
        }
    }

    /**
     * 获取设备影子状态
     */
    fun getDeviceShadow() {
        val config = currentConfig ?: return
        if (!isConnected || mqttClient?.isConnected != true) return

        try {
            val requestId = generateRequestId()
            val topic = "\$oc/devices/${config.deviceId}/sys/shadow/get/request_id=$requestId"

            val message = JsonObject().apply {
                addProperty("object_device_id", config.deviceId)
                addProperty("service_id", "dataText")
            }

            val mqttMessage = MqttMessage(message.toString().toByteArray()).apply {
                qos = 1
            }

            mqttClient?.publish(topic, mqttMessage)
            Log.d(TAG, "发送设备影子获取请求: $topic")
            Log.d(TAG, "请求消息内容: ${message.toString()}")
            addDebugMessage("发送�������备影子请求: ${message.toString()}")

        } catch (e: Exception) {
            Log.e(TAG, "获取设备影子失败", e)
            addDebugMessage("设备影子请求失败: ${e.message}")
        }
    }

    /**
     * 发送设备指令
     */
    private fun sendDeviceCommand(commandType: String, commandValue: Any) {
        val config = currentConfig ?: return
        if (!isConnected || mqttClient?.isConnected != true) return

        try {
            val requestId = generateRequestId()
            val topic = "\$oc/devices/${config.deviceId}/sys/commands/request_id=$requestId"

            val command = JsonObject().apply {
                addProperty("object_device_id", config.deviceId)
                addProperty("service_id", "ControlService")

                val paras = JsonObject().apply {
                    addProperty(commandType, commandValue.toString())
                }
                add("paras", paras)
            }

            val commandJson = command.toString()
            val mqttMessage = MqttMessage(commandJson.toByteArray()).apply {
                qos = 1
            }

            mqttClient?.publish(topic, mqttMessage)
            Log.d(TAG, "发送设备指令: $commandType = $commandValue")

            // 记录调试数据
            addDebugMessage("发送指令: $commandType = $commandValue")
            _lastSentCommand.value = commandJson

        } catch (e: Exception) {
            Log.e(TAG, "发送设备指令失败", e)
            addDebugMessage("发送指令失败: ${e.message}")
        }
    }

    /**
     * 开始数据轮询
     */
    private fun startDataPolling() {
        serviceScope.launch {
            while (isConnected) {
                try {
                    getDeviceShadow()
                    delay(10000) // 每10秒获取一次数据
                } catch (e: Exception) {
                    Log.e(TAG, "数据轮询出错", e)
                    delay(5000)
                }
            }
        }
    }

    /**
     * 创建MQTT回调
     */
    private fun createMqttCallback(): MqttCallback {
        return object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Log.w(TAG, "MQTT连接丢失", cause)
                isConnected = false
                deviceDataManager.updateConnectionStatus("连接断开")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                try {
                    val payload = message?.payload?.let { String(it) } ?: return
                    Log.d(TAG, "收到消息 topic: $topic, payload: $payload")

                    // 记录调试数据
                    addDebugMessage("收到消息: $topic")
                    addDebugMessage("数据内容: $payload")
                    _lastReceivedData.value = payload

                    // 根据topic类型解析不同的消息
                    when {
                        topic?.contains("shadow/get/response") == true -> {
                            parseDeviceShadowResponse(payload)
                        }
                        topic?.contains("properties/report") == true -> {
                            parsePropertyReport(payload)
                        }
                        topic?.contains("messages/up") == true -> {
                            parseDeviceMessage(payload)
                        }
                        topic?.contains("events/up") == true -> {
                            parseDeviceEvent(payload)
                        }
                        topic?.contains("commands/response") == true -> {
                            parseCommandResponse(payload)
                        }
                        topic?.contains("/data") == true -> {
                            parseGenericData(payload)
                        }
                        else -> {
                            // 尝试通用解析
                            parseGenericData(payload)
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "处理MQTT消息失败", e)
                    addDebugMessage("处理消息失败: ${e.message}")
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(TAG, "消息发送完成")
            }
        }
    }

    /**
     * 解析设备影子响应
     */
    private fun parseDeviceShadowResponse(payload: String) {
        try {
            Log.d(TAG, "开始解析设备影子响应: $payload")
            addDebugMessage("解析设备影子数据: $payload")
            val json = gson.fromJson(payload, JsonObject::class.java)

            // 支持多种JSON格式解析
            var properties: JsonObject? = null

            // 1. 解析您的平台格式: shadow[0].reported.properties
            json.getAsJsonArray("shadow")?.let { shadowArray ->
                if (shadowArray.size() > 0) {
                    val shadowObj = shadowArray.get(0).asJsonObject
                    val reportedObj = shadowObj.getAsJsonObject("reported")
                    if (reportedObj != null) {
                        properties = reportedObj.getAsJsonObject("properties")
                        if (properties != null) {
                            Log.d(TAG, "使用平台格式解析: shadow[0].reported.properties")
                            addDebugMessage("使用平台格式解析数据")
                        }
                    }
                }
            }

            // 2. 如果上述格式不存在，尝试华为云标准格式: shadow.reported[0].properties
            if (properties == null) {
                json.getAsJsonObject("shadow")?.getAsJsonArray("reported")?.let { reportedArray ->
                    if (reportedArray.size() > 0) {
                        val reportedObj = reportedArray.get(0).asJsonObject
                        properties = reportedObj.getAsJsonObject("properties")
                        if (properties != null) {
                            Log.d(TAG, "使用华为云标准格式解析: shadow.reported[0].properties")
                            addDebugMessage("使用华为云标准格式解析数据")
                        }
                    }
                }
            }

            // 3. 如果标准格式不存在，尝试直接从根级别的properties解析
            if (properties == null && json.has("properties")) {
                properties = json.getAsJsonObject("properties")
                Log.d(TAG, "使用根级别properties格式解析")
                addDebugMessage("使用根级别properties格式解析数据")
            }

            // 4. 如果都不存在，尝试直接从根级别解析所有数据
            if (properties == null) {
                properties = json
                Log.d(TAG, "使用根级别直接解析")
                addDebugMessage("使用根级别直接解析数据")
            }

            if (properties != null && properties.size() > 0) {
                var hasUpdatedData = false

                // 解析温度
                properties.get("temperature")?.let { element ->
                    try {
                        val temperature = when {
                            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asFloat
                            element.isJsonPrimitive && element.asJsonPrimitive.isString -> element.asString.toFloat()
                            else -> null
                        }
                        temperature?.let {
                            deviceDataManager.updateTemperature(it)
                            Log.d(TAG, "✅ 更新温度: ${it}°C")
                            addDebugMessage("✅ 更新温度: ${it}°C")
                            hasUpdatedData = true
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "解析温度失败: ${element.asString}")
                        addDebugMessage("❌ 解析温度失败: ${element.asString}")
                    }
                }

                // 解析湿度
                properties.get("humidity")?.let { element ->
                    try {
                        val humidity = when {
                            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asFloat
                            element.isJsonPrimitive && element.asJsonPrimitive.isString -> element.asString.toFloat()
                            else -> null
                        }
                        humidity?.let {
                            deviceDataManager.updateHumidity(it)
                            Log.d(TAG, "✅ 更新湿度: ${it}%")
                            addDebugMessage("✅ 更新湿度: ${it}%")
                            hasUpdatedData = true
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "解析湿度失败: ${element.asString}")
                        addDebugMessage("❌ 解析湿度失败: ${element.asString}")
                    }
                }

                // 解析食物量
                properties.get("food_amount")?.let { element ->
                    try {
                        val foodAmount = when {
                            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asFloat
                            element.isJsonPrimitive && element.asJsonPrimitive.isString -> element.asString.toFloat()
                            else -> null
                        }
                        foodAmount?.let {
                            deviceDataManager.updateFoodAmount(it)
                            Log.d(TAG, "✅ 更新食物量: ${it}g")
                            addDebugMessage("✅ 更新食物量: ${it}g")
                            hasUpdatedData = true
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "解析食物量失败: ${element.asString}")
                        addDebugMessage("❌ 解析食物量失败: ${element.asString}")
                    }
                }

                // 解析水量
                properties.get("water_amount")?.let { element ->
                    try {
                        val waterAmount = when {
                            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asFloat
                            element.isJsonPrimitive && element.asJsonPrimitive.isString -> element.asString.toFloat()
                            else -> null
                        }
                        waterAmount?.let {
                            deviceDataManager.updateWaterAmount(it)
                            Log.d(TAG, "✅ 更新水量: ${it}ml")
                            addDebugMessage("✅ 更新水量: ${it}ml")
                            hasUpdatedData = true
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "解析水量失败: ${element.asString}")
                        addDebugMessage("❌ 解析水量失败: ${element.asString}")
                    }
                }

                // 解析通风状态
                properties.get("ventilation_status")?.let { element ->
                    try {
                        val ventilation = when {
                            element.isJsonPrimitive && element.asJsonPrimitive.isBoolean -> element.asBoolean
                            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asInt == 1
                            element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                                val str = element.asString.lowercase()
                                str == "true" || str == "1" || str == "on"
                            }
                            else -> false
                        }
                        deviceDataManager.updateVentilationStatus(ventilation)
                        Log.d(TAG, "✅ 更新通风状态: $ventilation")
                        addDebugMessage("✅ 更新通风状态: $ventilation")
                        hasUpdatedData = true
                    } catch (e: Exception) {
                        Log.w(TAG, "解析通风状态失败: ${element.asString}")
                        addDebugMessage("❌ 解析通风状态失败: ${element.asString}")
                    }
                }

                // 解析消毒状态
                properties.get("disinfection_status")?.let { element ->
                    try {
                        val disinfection = when {
                            element.isJsonPrimitive && element.asJsonPrimitive.isBoolean -> element.asBoolean
                            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asInt == 1
                            element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                                val str = element.asString.lowercase()
                                str == "true" || str == "1" || str == "on"
                            }
                            else -> false
                        }
                        deviceDataManager.updateDisinfectionStatus(disinfection)
                        Log.d(TAG, "✅ 更新消毒状态: $disinfection")
                        addDebugMessage("✅ 更新消毒状态: $disinfection")
                        hasUpdatedData = true
                    } catch (e: Exception) {
                        Log.w(TAG, "解析消毒状态失败: ${element.asString}")
                        addDebugMessage("❌ 解析消毒状态失败: ${element.asString}")
                    }
                }

                // 解析加热状态
                properties.get("heating_status")?.let { element ->
                    try {
                        val heating = when {
                            element.isJsonPrimitive && element.asJsonPrimitive.isBoolean -> element.asBoolean
                            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asInt == 1
                            element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                                val str = element.asString.lowercase()
                                str == "true" || str == "1" || str == "on"
                            }
                            else -> false
                        }
                        deviceDataManager.updateHeatingStatus(heating)
                        Log.d(TAG, "✅ 更新加热状态: $heating")
                        addDebugMessage("✅ 更新加热状态: $heating")
                        hasUpdatedData = true
                    } catch (e: Exception) {
                        Log.w(TAG, "解析加热状态失败: ${element.asString}")
                        addDebugMessage("❌ 解析加热状态失败: ${element.asString}")
                    }
                }

                if (hasUpdatedData) {
                    Log.d(TAG, "🎉 设备数据更新成功")
                    addDebugMessage("🎉 设备数据更新成功")
                } else {
                    Log.w(TAG, "⚠️ 未能解析出任何有效数据")
                    addDebugMessage("⚠️ 未能解析出有效数据")

                    // 输出所有可用的字段名以便调试
                    val availableKeys = properties.keySet().toList()
                    Log.d(TAG, "可用字段: $availableKeys")
                    addDebugMessage("可用字段: $availableKeys")
                }
            } else {
                Log.w(TAG, "❌ 未找到有效的属性数据")
                addDebugMessage("❌ 未找到有效的属性数据")

                // 输出整个JSON结构以便调试
                Log.d(TAG, "完整JSON结构: ${json.toString()}")
                addDebugMessage("完整JSON: ${json.toString()}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ 解析设备影子响应失败", e)
            addDebugMessage("❌ 解析失败: ${e.message}")
        }
    }

    /**
     * 解析属性报告
     */
    private fun parsePropertyReport(payload: String) {
        try {
            Log.d(TAG, "开始解析属性报告: $payload")
            val json = gson.fromJson(payload, JsonObject::class.java)

            // 直接从根级别解析数据
            val properties = json

            if (properties != null) {
                // 解析温度
                properties.get("temperature")?.let { element ->
                    try {
                        val temperature = element.asFloat
                        deviceDataManager.updateTemperature(temperature)
                        Log.d(TAG, "更新温度: $temperature°C")
                    } catch (e: Exception) {
                        Log.w(TAG, "解析温度失败: ${element.asString}")
                    }
                }

                // 解析湿度
                properties.get("humidity")?.let { element ->
                    try {
                        val humidity = element.asFloat
                        deviceDataManager.updateHumidity(humidity)
                        Log.d(TAG, "更新湿度: $humidity%")
                    } catch (e: Exception) {
                        Log.w(TAG, "解析湿度失败: ${element.asString}")
                    }
                }

                // 解析食物量
                properties.get("food_amount")?.let { element ->
                    try {
                        val foodAmount = element.asFloat
                        deviceDataManager.updateFoodAmount(foodAmount)
                        Log.d(TAG, "更新食物量: ${foodAmount}g")
                    } catch (e: Exception) {
                        Log.w(TAG, "解析食物量失败: ${element.asString}")
                    }
                }

                // 解析水量
                properties.get("water_amount")?.let { element ->
                    try {
                        val waterAmount = element.asFloat
                        deviceDataManager.updateWaterAmount(waterAmount)
                        Log.d(TAG, "更新水量: ${waterAmount}ml")
                    } catch (e: Exception) {
                        Log.w(TAG, "解析水量失败: ${element.asString}")
                    }
                }

                // 解析通风状态
                properties.get("ventilation_status")?.let { element ->
                    try {
                        val ventilation = when {
                            element.isJsonPrimitive && element.asJsonPrimitive.isBoolean -> element.asBoolean
                            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asInt == 1
                            element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                                val str = element.asString.lowercase()
                                str == "true" || str == "1" || str == "on"
                            }
                            else -> false
                        }
                        deviceDataManager.updateVentilationStatus(ventilation)
                        Log.d(TAG, "更新通风状态: $ventilation")
                    } catch (e: Exception) {
                        Log.w(TAG, "��析通风状态失败: ${element.asString}")
                    }
                }

                // 解析消毒状态
                properties.get("disinfection_status")?.let { element ->
                    try {
                        val disinfection = when {
                            element.isJsonPrimitive && element.asJsonPrimitive.isBoolean -> element.asBoolean
                            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asInt == 1
                            element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                                val str = element.asString.lowercase()
                                str == "true" || str == "1" || str == "on"
                            }
                            else -> false
                        }
                        deviceDataManager.updateDisinfectionStatus(disinfection)
                        Log.d(TAG, "更新消毒状态: $disinfection")
                    } catch (e: Exception) {
                        Log.w(TAG, "解析消毒状态失败: ${element.asString}")
                    }
                }

                // 解析加热状态
                properties.get("heating_status")?.let { element ->
                    try {
                        val heating = when {
                            element.isJsonPrimitive && element.asJsonPrimitive.isBoolean -> element.asBoolean
                            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asInt == 1
                            element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                                val str = element.asString.lowercase()
                                str == "true" || str == "1" || str == "on"
                            }
                            else -> false
                        }
                        deviceDataManager.updateHeatingStatus(heating)
                        Log.d(TAG, "更新加热状态: $heating")
                    } catch (e: Exception) {
                        Log.w(TAG, "解析加热状态失败: ${element.asString}")
                    }
                }

                // 解析目标温度
                properties.get("target_temperature")?.let { element ->
                    try {
                        val targetTemp = element.asFloat
                        deviceDataManager.updateTargetTemperature(targetTemp)
                        Log.d(TAG, "更新目标温度: $targetTemp°C")
                    } catch (e: Exception) {
                        Log.w(TAG, "解析目标温度失败: ${element.asString}")
                    }
                }

                Log.d(TAG, "设备状态更新成功")
            } else {
                Log.w(TAG, "未找到有效的属性数据")
            }

        } catch (e: Exception) {
            Log.e(TAG, "解析属性报告失败", e)
        }
    }

    /**
     * 解析设备消息
     */
    private fun parseDeviceMessage(payload: String) {
        try {
            Log.d(TAG, "开始解析设备消息: $payload")
            val json = gson.fromJson(payload, JsonObject::class.java)

            // 直接从根级别解析数据
            val messageData = json

            if (messageData != null) {
                // TODO: 根据实际消息内容解析并处理

                Log.d(TAG, "设备消息解析成功: $messageData")
            } else {
                Log.w(TAG, "未找到有效的消息数据")
            }

        } catch (e: Exception) {
            Log.e(TAG, "解析设备消息失败", e)
        }
    }

    /**
     * 解析设备事件
     */
    private fun parseDeviceEvent(payload: String) {
        try {
            Log.d(TAG, "开始解析设备事件: $payload")
            val json = gson.fromJson(payload, JsonObject::class.java)

            // 直接从根级别解析数据
            val eventData = json

            if (eventData != null) {
                // TODO: 根据实际事件内容解析并处理

                Log.d(TAG, "设备事件解析成功: $eventData")
            } else {
                Log.w(TAG, "未找到有效的事件数据")
            }

        } catch (e: Exception) {
            Log.e(TAG, "解析设备事件失败", e)
        }
    }

    /**
     * 解析通用数据
     */
    private fun parseGenericData(payload: String) {
        try {
            Log.d(TAG, "开始解析通用数据: $payload")
            val json = gson.fromJson(payload, JsonObject::class.java)

            // 直接从根级别解析数据
            val data = json

            if (data != null) {
                // TODO: 根据实际数据内容解析并处理

                Log.d(TAG, "通用数据解析成功: $data")
            } else {
                Log.w(TAG, "未找到有效的数据")
            }

        } catch (e: Exception) {
            Log.e(TAG, "解析通用数据失败", e)
        }
    }

    /**
     * 生成HMAC-SHA256密码
     */
    private fun generatePassword(deviceSecret: String): String {
        try {
            val timestamp = SimpleDateFormat("yyyyMMddHH", Locale.getDefault()).format(Date())
            val mac = Mac.getInstance("HmacSHA256")
            val secretKeySpec = SecretKeySpec(timestamp.toByteArray(), "HmacSHA256")
            mac.init(secretKeySpec)
            val result = mac.doFinal(deviceSecret.toByteArray())
            return result.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "生成密码失败", e)
            return deviceSecret
        }
    }

    /**
     * 生成请求ID
     */
    private fun generateRequestId(): String {
        return "req_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"
    }

    /**
     * 获取连接状态
     */
    fun isConnected(): Boolean = isConnected

    /**
     * 添加调试消息
     */
    private fun addDebugMessage(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val debugMessage = "[$timestamp] $message"

        val currentMessages = _debugMessages.value.toMutableList()
        currentMessages.add(0, debugMessage) // 添加到列表开头

        // 保持最多50条调试消息
        if (currentMessages.size > 50) {
            currentMessages.removeAt(currentMessages.size - 1)
        }

        _debugMessages.value = currentMessages
    }

    /**
     * 清������试消息
     */
    fun clearDebugMessages() {
        _debugMessages.value = emptyList()
    }

    /**
     * 获取MQTT配置信息
     */
    fun getMqttConfig(): MqttConfig? = currentConfig

    /**
     * 清理资源
     */
    fun cleanup() {
        serviceScope.cancel()
        disconnect()
    }

    /**
     * 下发设备消息 - 根据华为云IoTDA平台标准
     * 参考文档: https://support.huaweicloud.com/api-iothub/iot_06_v5_0059.html
     */
    fun sendDeviceMessage(
        ventilation: Int? = null,
        disinfection: Int? = null,
        heating: Int? = null,
        targetTemperature: Int? = null,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        val config = currentConfig ?: run {
            val errorMsg = "MQTT配置未设置"
            Log.e(TAG, errorMsg)
            onResult?.invoke(false, errorMsg)
            showToast(errorMsg)
            return
        }

        if (!isConnected || mqttClient?.isConnected != true) {
            val errorMsg = "MQTT未连接，无法发送消息"
            Log.w(TAG, errorMsg)
            onResult?.invoke(false, errorMsg)
            showToast(errorMsg)
            return
        }

        // 检查参数有效性 - 至少要有一个控制参数
        if (ventilation == null && disinfection == null && heating == null && targetTemperature == null) {
            val errorMsg = "至少需要设置一个控制参数"
            Log.w(TAG, errorMsg)
            onResult?.invoke(false, errorMsg)
            return
        }

        // 检查状态冲突
        val hasConflict = checkDeviceStateConflict(
            ventilation == 1,
            disinfection == 1,
            heating == 1
        )
        if (hasConflict) {
            val errorMsg = "设备状态冲突，无法执行操作"
            Log.w(TAG, "⚠️ $errorMsg")
            onResult?.invoke(false, errorMsg)
            showToast(errorMsg)
            return
        }

        try {
            val requestId = generateRequestId()
            val topic = "\$oc/devices/${config.deviceId}/sys/commands/request_id=$requestId"

            // 构建控制指令 - 只包含控制参数，不包含状态数据
            val paras = JsonObject()

            // 只添加需要控制的参数
            ventilation?.let { paras.addProperty("ventilation", it) }
            disinfection?.let { paras.addProperty("disinfection", it) }
            heating?.let { paras.addProperty("heating", it) }
            targetTemperature?.let { paras.addProperty("target_temperature", it) }

            val command = JsonObject().apply {
                addProperty("object_device_id", config.deviceId)
                addProperty("service_id", "ControlService")
                add("paras", paras)
            }

            val commandJson = command.toString()
            val mqttMessage = MqttMessage(commandJson.toByteArray()).apply {
                qos = 1
            }

            // 注册回调
            onResult?.let { callback ->
                commandCallbacks[requestId] = callback

                // 设置超时处理
                serviceScope.launch {
                    delay(10000) // 10秒超时
                    if (commandCallbacks.containsKey(requestId)) {
                        commandCallbacks.remove(requestId)
                        val timeoutMsg = "指���发送超时"
                        callback(false, timeoutMsg)
                        showToast(timeoutMsg)
                    }
                }
            }

            mqttClient?.publish(topic, mqttMessage)

            // 记录日志
            val commandParams = mutableListOf<String>()
            ventilation?.let { commandParams.add("通风开关=${if (it == 1) "开" else "关"}") }
            disinfection?.let { commandParams.add("消毒开关=${if (it == 1) "开" else "关"}") }
            heating?.let { commandParams.add("加热开关=${if (it == 1) "开" else "关"}") }
            targetTemperature?.let { commandParams.add("设定温度=${it}°C") }

            val commandDescription = commandParams.joinToString(", ")
            Log.d(TAG, "✅ 发送控制指令: $commandDescription")
            addDebugMessage("✅ 发送控制指令: $commandDescription")
            addDebugMessage("📤 指令内容: $commandJson")

            _lastSentCommand.value = commandJson

        } catch (e: Exception) {
            val errorMsg = "发送控制指令失败: ${e.message}"
            Log.e(TAG, "❌ $errorMsg", e)
            addDebugMessage("❌ $errorMsg")
            onResult?.invoke(false, errorMsg)
            showToast(errorMsg)
        }
    }

    /**
     * 发送通风控制指令
     */
    fun sendVentilationCommand(enabled: Boolean, callback: (Boolean, String?) -> Unit) {
        try {
            sendDeviceCommand("ventilation", if (enabled) 1 else 0)
            callback(true, null)
            Log.d(TAG, "发送通风控制指令: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "发送通风控制指令失败", e)
            callback(false, e.message)
        }
    }

    /**
     * 发送消毒控制指令
     */
    fun sendDisinfectionCommand(enabled: Boolean, callback: (Boolean, String?) -> Unit) {
        try {
            sendDeviceCommand("disinfection", if (enabled) 1 else 0)
            callback(true, null)
            Log.d(TAG, "发送消毒控制指令: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "发送消毒控制指令失败", e)
            callback(false, e.message)
        }
    }

    /**
     * 发送加热控制指令
     */
    fun sendHeatingCommand(enabled: Boolean, callback: (Boolean, String?) -> Unit) {
        try {
            sendDeviceCommand("heating", if (enabled) 1 else 0)
            callback(true, null)
            Log.d(TAG, "发送加热控制指令: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "发送加热控制指令失败", e)
            callback(false, e.message)
        }
    }

    /**
     * 发送目标温度设置指令
     */
    fun sendTargetTemperatureCommand(temperature: Int, callback: (Boolean, String?) -> Unit) {
        try {
            sendDeviceCommand("target_temperature", temperature)
            callback(true, null)
            Log.d(TAG, "发送目标温度设置指令: $temperature")
        } catch (e: Exception) {
            Log.e(TAG, "发送目标温度设置指令失败", e)
            callback(false, e.message)
        }
    }

    /**
     * 批量控制设备状态 - 带结果回调
     */
    fun controlDeviceState(
        ventilation: Boolean? = null,
        disinfection: Boolean? = null,
        heating: Boolean? = null,
        targetTemperature: Int? = null,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        // 转换为数值参数
        val ventilationValue = ventilation?.let { if (it) 1 else 0 }
        val disinfectionValue = disinfection?.let { if (it) 1 else 0 }
        val heatingValue = heating?.let { if (it) 1 else 0 }

        Log.d(TAG, "批量控制设备状态")
        sendDeviceMessage(
            ventilation = ventilationValue,
            disinfection = disinfectionValue,
            heating = heatingValue,
            targetTemperature = targetTemperature,
            onResult = onResult
        )
    }

    /**
     * 上报设备控制状态到MQTT服务器
     * Topic: $oc/devices/{device_id}/sys/messages/up
     */
    fun reportControlStatus(
        ventilation: Boolean,
        disinfection: Boolean,
        heating: Boolean,
        targetTemperature: Int,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        val config = currentConfig ?: run {
            val errorMsg = "MQTT配置未设置"
            Log.e(TAG, errorMsg)
            onResult?.invoke(false, errorMsg)
            showToast(errorMsg)
            return
        }

        if (!isConnected || mqttClient?.isConnected != true) {
            val errorMsg = "MQTT未连��，无法上报数据"
            Log.w(TAG, errorMsg)
            onResult?.invoke(false, errorMsg)
            showToast(errorMsg)
            return
        }

        try {
            val topic = "\$oc/devices/${config.deviceId}/sys/messages/up"

            // 获取当前设备数据以包含完整的状态信息
            val currentData = deviceDataManager.deviceData.value

            // 构建符合华为云IoTDA标准的消息格式
            val properties = JsonObject().apply {
                // 包含传感器数据
                addProperty("temperature", currentData.temperature)
                addProperty("humidity", currentData.humidity)
                addProperty("food_amount", currentData.foodAmount)
                addProperty("water_amount", currentData.waterAmount)

                // 添加控制状态
                addProperty("ventilation_status", ventilation)
                addProperty("disinfection_status", disinfection)
                addProperty("heating_status", heating)
                addProperty("target_temperature", targetTemperature)
            }

            val service = JsonObject().apply {
                addProperty("service_id", "dataText")
                add("properties", properties)
                addProperty("eventTime", SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.getDefault()).format(Date()))
            }

            val services = com.google.gson.JsonArray().apply {
                add(service)
            }

            val message = JsonObject().apply {
                add("services", services)
            }

            val messageJson = message.toString()
            val mqttMessage = MqttMessage(messageJson.toByteArray()).apply {
                qos = 1
            }

            mqttClient?.publish(topic, mqttMessage)

            // 记录日志
            val statusDescription = "通风=${if (ventilation) "开" else "关"}, " +
                    "消毒=${if (disinfection) "开" else "关"}, " +
                    "加热=${if (heating) "开" else "关"}, " +
                    "目标温度=${targetTemperature}°C"

            Log.d(TAG, "✅ 上报控制状态: $statusDescription")
            addDebugMessage("✅ 上报控制状态: $statusDescription")
            addDebugMessage("📤 上报数据: $messageJson")

            _lastSentCommand.value = messageJson

            // 立即调用成功回调（因为上报不需要设备响应）
            onResult?.invoke(true, "控制状态上报成功")
            showToast("控制状态上报成功")

        } catch (e: Exception) {
            val errorMsg = "上报控制状态失败: ${e.message}"
            Log.e(TAG, "❌ $errorMsg", e)
            addDebugMessage("�� $errorMsg")
            onResult?.invoke(false, errorMsg)
            showToast(errorMsg)
        }
    }

    /**
     * 上报通风开关状态
     */
    fun reportVentilationStatus(enabled: Boolean, onResult: ((Boolean, String?) -> Unit)? = null) {
        Log.d(TAG, "上报通风开关状态: ${if (enabled) "开启" else "关闭"}")

        // 获取当前其他控制状态
        val currentData = deviceDataManager.deviceData.value
        val currentDisinfection = currentData.disinfectionStatus ?: false
        val currentHeating = currentData.heatingStatus ?: false
        val currentTargetTemp = currentData.targetTemperature?.toInt() ?: 25

        reportControlStatus(
            ventilation = enabled,
            disinfection = currentDisinfection,
            heating = currentHeating,
            targetTemperature = currentTargetTemp,
            onResult = onResult
        )
    }

    /**
     * 上报消毒开关状态
     */
    fun reportDisinfectionStatus(enabled: Boolean, onResult: ((Boolean, String?) -> Unit)? = null) {
        Log.d(TAG, "上报消毒开关状态: ${if (enabled) "开启" else "关闭"}")

        // 获取当前其他控制状态
        val currentData = deviceDataManager.deviceData.value
        val currentVentilation = currentData.ventilationStatus ?: false
        val currentHeating = currentData.heatingStatus ?: false
        val currentTargetTemp = currentData.targetTemperature?.toInt() ?: 25

        reportControlStatus(
            ventilation = currentVentilation,
            disinfection = enabled,
            heating = currentHeating,
            targetTemperature = currentTargetTemp,
            onResult = onResult
        )
    }

    /**
     * 上报加热开关状态
     */
    fun reportHeatingStatus(enabled: Boolean, onResult: ((Boolean, String?) -> Unit)? = null) {
        Log.d(TAG, "上报加热开关状态: ${if (enabled) "开启" else "关闭"}")

        // 获取当前其他控制状态
        val currentData = deviceDataManager.deviceData.value
        val currentVentilation = currentData.ventilationStatus ?: false
        val currentDisinfection = currentData.disinfectionStatus ?: false
        val currentTargetTemp = currentData.targetTemperature?.toInt() ?: 25

        reportControlStatus(
            ventilation = currentVentilation,
            disinfection = currentDisinfection,
            heating = enabled,
            targetTemperature = currentTargetTemp,
            onResult = onResult
        )
    }

    /**
     * 上报目标温度设置
     */
    fun reportTargetTemperature(temperature: Int, onResult: ((Boolean, String?) -> Unit)? = null) {
        Log.d(TAG, "上报目标温度设置: ${temperature}°C")

        // 获取当前其他控制状态
        val currentData = deviceDataManager.deviceData.value
        val currentVentilation = currentData.ventilationStatus ?: false
        val currentDisinfection = currentData.disinfectionStatus ?: false
        val currentHeating = currentData.heatingStatus ?: false

        reportControlStatus(
            ventilation = currentVentilation,
            disinfection = currentDisinfection,
            heating = currentHeating,
            targetTemperature = temperature,
            onResult = onResult
        )
    }

    /**
     * 批量上报设备控制状态（带状态冲突检查）
     */
    fun reportDeviceControlState(
        ventilation: Boolean? = null,
        disinfection: Boolean? = null,
        heating: Boolean? = null,
        targetTemperature: Int? = null,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        // 获取当前状态作为默认值
        val currentData = deviceDataManager.deviceData.value
        val finalVentilation = ventilation ?: currentData.ventilationStatus ?: false
        val finalDisinfection = disinfection ?: currentData.disinfectionStatus ?: false
        val finalHeating = heating ?: currentData.heatingStatus ?: false
        val finalTargetTemp = targetTemperature ?: currentData.targetTemperature?.toInt() ?: 25

        // 检查状态冲突
        val hasConflict = checkDeviceStateConflict(finalVentilation, finalDisinfection, finalHeating)
        if (hasConflict) {
            val errorMsg = "设备状态冲突，无法上报"
            Log.w(TAG, "⚠️ $errorMsg")
            onResult?.invoke(false, errorMsg)
            showToast(errorMsg)
            return
        }

        Log.d(TAG, "批量上报设备控制状态")
        reportControlStatus(
            ventilation = finalVentilation,
            disinfection = finalDisinfection,
            heating = finalHeating,
            targetTemperature = finalTargetTemp,
            onResult = onResult
        )
    }

    /**
     * 解析命令响应
     */
    private fun parseCommandResponse(payload: String) {
        try {
            Log.d(TAG, "开始解析命令响应: $payload")
            addDebugMessage("解析命令响应: $payload")

            val json = gson.fromJson(payload, JsonObject::class.java)

            // 提取请求ID和响应结果
            val requestId = json.get("id")?.asString
            val resultCode = json.get("result_code")?.asInt ?: -1
            val responseMsg = json.get("response_detail")?.asString

            Log.d(TAG, "命令响应 - requestId: $requestId, resultCode: $resultCode, response: $responseMsg")

            requestId?.let { id ->
                commandCallbacks[id]?.let { callback ->
                    commandCallbacks.remove(id)

                    when (resultCode) {
                        0 -> {
                            // 命令执行成功
                            val successMsg = "设备控制成功"
                            Log.d(TAG, "✅ $successMsg")
                            addDebugMessage("✅ $successMsg")
                            callback(true, successMsg)
                            showToast(successMsg)
                        }
                        else -> {
                            // 命令执行失败
                            val errorMsg = responseMsg ?: "设备控制失败，错误码: $resultCode"
                            Log.w(TAG, "❌ $errorMsg")
                            addDebugMessage("❌ $errorMsg")
                            callback(false, errorMsg)
                            showToast(errorMsg)
                        }
                    }
                }
            }

            // 如果没有找到对应的回调，说明可能是其他类型的响应
            if (requestId == null || !commandCallbacks.containsKey(requestId)) {
                Log.d(TAG, "收到未注册的命令响应或其他类型响应")
                addDebugMessage("收到未注册的命令响应: $payload")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ 解析命令响应失败", e)
            addDebugMessage("❌ 解析命令响应失败: ${e.message}")
        }
    }

    /**
     * 检查设备状态冲突
     * 避免三个开关同时开启可能造成的问题
     */
    private fun checkDeviceStateConflict(
        ventilation: Boolean?,
        disinfection: Boolean?,
        heating: Boolean?
    ): Boolean {
        // 统计要开启的功能数量
        var enabledCount = 0
        if (ventilation == true) enabledCount++
        if (disinfection == true) enabledCount++
        if (heating == true) enabledCount++

        // 如果同时开启超过2个功能，视为冲突
        if (enabledCount > 2) {
            Log.w(TAG, "⚠️ 状态冲突：不建议同时开启超过2个功能")
            addDebugMessage("⚠️ 状态冲突：同时开启功能过多")
            return true
        }

        // 特殊冲突检查：通风和加热同时开启可能影响效果
        if (ventilation == true && heating == true) {
            Log.w(TAG, "⚠�� 状态冲突：通��和加热同时开启可能影响加热效果")
            addDebugMessage("⚠️ 状态冲突：通风和加热功能冲突")
            return true
        }

        return false
    }

    /**
     * 显示Toast提示
     */
    private fun showToast(message: String) {
        appContext?.let { context ->
            serviceScope.launch(Dispatchers.Main) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
