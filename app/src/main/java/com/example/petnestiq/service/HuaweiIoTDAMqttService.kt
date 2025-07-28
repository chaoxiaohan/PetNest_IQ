package com.example.petnestiq.service

import android.content.Context
import android.util.Log
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
        val deviceSecret: String,       // 设备密钥
        val clientId: String = deviceId,
        val username: String = deviceId,
        val password: String = ""       // 将通过密钥生成
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

        serviceScope.launch {
            try {
                disconnect() // 先断开已有连接

                Log.i(TAG, "开始连接MQTT服务器: ${config.serverUri}")
                deviceDataManager.updateConnectionStatus("连接中...")

                // 生成密码
                val password = generatePassword(config.deviceSecret)

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

                // 订阅影子设备状态获取topic
                val shadowTopic = "\$oc/devices/${config.deviceId}/sys/shadow/get/response"
                mqttClient?.subscribe(shadowTopic, 1)
                Log.i(TAG, "订阅影子设备状态topic: $shadowTopic")

                isConnected = true
                Log.i(TAG, "MQTT连接成功")
                deviceDataManager.updateConnectionStatus("MQTT连接")

                // 开始定期获取设备状态
                startDataPolling()

                // 开始监控设备状态变化并下发指令
                startCommandMonitoring()

            } catch (e: Exception) {
                Log.e(TAG, "MQTT连接失败", e)
                isConnected = false
                deviceDataManager.updateConnectionStatus("连接失败")
            }
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
                addProperty("service_id", "BasicData")
            }

            val mqttMessage = MqttMessage(message.toString().toByteArray()).apply {
                qos = 1
            }

            mqttClient?.publish(topic, mqttMessage)
            Log.d(TAG, "发送设备影子获取请求: $topic")

        } catch (e: Exception) {
            Log.e(TAG, "获取设备影子失败", e)
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
     * 开始监控设备状态变化
     */
    private fun startCommandMonitoring() {
        serviceScope.launch {
            deviceDataManager.deviceData.collect { currentData ->
                val lastData = lastSentStates.get()

                // 检查是否有状态变化需要下发指令
                if (lastData != null) {
                    // 检查通风状态变化
                    if (currentData.ventilationStatus != lastData.ventilationStatus) {
                        sendDeviceCommand("ventilation", if (currentData.ventilationStatus) 1 else 0)
                    }

                    // 检查消毒状态变化
                    if (currentData.disinfectionStatus != lastData.disinfectionStatus) {
                        sendDeviceCommand("disinfection", if (currentData.disinfectionStatus) 1 else 0)
                    }

                    // 检查加热状态变化
                    if (currentData.heatingStatus != lastData.heatingStatus) {
                        sendDeviceCommand("heating", if (currentData.heatingStatus) 1 else 0)
                    }

                    // 检查目标温度变化
                    if (currentData.targetTemperature != lastData.targetTemperature) {
                        sendDeviceCommand("target_temperature", currentData.targetTemperature)
                    }
                }

                // 更新上次发送的状态
                lastSentStates.set(currentData)
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
                    _lastReceivedData.value = payload

                    // 解析设备影子响应
                    if (topic?.contains("shadow/get/response") == true) {
                        parseDeviceShadowResponse(payload)
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
            val json = gson.fromJson(payload, JsonObject::class.java)

            // 支持多种JSON格式
            var properties: JsonObject? = null

            // 尝试解析华为云标准格式
            val shadow = json.getAsJsonObject("shadow")?.getAsJsonArray("reported")?.get(0)?.asJsonObject
            if (shadow != null) {
                properties = shadow.getAsJsonObject("properties")
            }

            // 如果标准格式不存在，尝试直接从根级别解析
            if (properties == null && json.has("properties")) {
                properties = json.getAsJsonObject("properties")
            }

            // 如果properties不存在，尝试直接从根级别解析数据
            if (properties == null) {
                properties = json
            }

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
                        Log.w(TAG, "解析通风状态失败: ${element.asString}")
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
            Log.e(TAG, "解析设备影子响应失败", e)
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
     * 清除调试消息
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
}
