package com.example.petnestiq.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class MqttConfig(
    val serverUrl: String = "ssl://e35491cb0c.st1.iotda-device.cn-north-4.myhuaweicloud.com",
    val port: Int = 8883,
    val clientId: String = "688879e2d582f20018403921_text1_0_0_2025072907",
    val username: String = "688879e2d582f20018403921_text1",
    val password: String = "bdd6a2f87eab3e9dd81325957547fa3b5b566f1abfbdb9850249b3b4984f277e",
    val subscribeTopic: String = "/device/data",
    val publishTopic: String = "/device/control",
    val useSSL: Boolean = true,
    val autoReconnect: Boolean = true
)

data class BluetoothConfig(
    val deviceName: String = "PetNest Device",
    val macAddress: String = "",
    val autoConnect: Boolean = false,
    val connectionTimeout: Int = 30
)

data class DeviceConfig(
    val name: String,
    val mqttConfig: MqttConfig,
    val bluetoothConfig: BluetoothConfig,
    val timestamp: Long = System.currentTimeMillis()
)

class DeviceConfigManager private constructor(private val context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "device_config_prefs", Context.MODE_PRIVATE
    )

    private val gson = Gson()

    // MQTT配置状态
    private val _mqttConfig = MutableStateFlow(loadMqttConfig())
    val mqttConfig: StateFlow<MqttConfig> = _mqttConfig.asStateFlow()

    // 蓝牙配置状态
    private val _bluetoothConfig = MutableStateFlow(loadBluetoothConfig())
    val bluetoothConfig: StateFlow<BluetoothConfig> = _bluetoothConfig.asStateFlow()

    // 已保存的配置列表
    private val _configList = MutableStateFlow(loadConfigList())
    val configList: StateFlow<List<String>> = _configList.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: DeviceConfigManager? = null

        fun getInstance(context: Context): DeviceConfigManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DeviceConfigManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        private const val KEY_MQTT_CONFIG = "mqtt_config"
        private const val KEY_BLUETOOTH_CONFIG = "bluetooth_config"
        private const val KEY_CONFIG_LIST = "config_list"
        private const val KEY_CONFIG_PREFIX = "saved_config_"
    }

    // 加载MQTT配置
    private fun loadMqttConfig(): MqttConfig {
        val configJson = preferences.getString(KEY_MQTT_CONFIG, null)
        return if (configJson != null) {
            try {
                gson.fromJson(configJson, MqttConfig::class.java) ?: MqttConfig()
            } catch (e: Exception) {
                MqttConfig() // 返回默认配置
            }
        } else {
            MqttConfig() // 返回默认配置
        }
    }

    // 加载蓝牙配置
    private fun loadBluetoothConfig(): BluetoothConfig {
        val configJson = preferences.getString(KEY_BLUETOOTH_CONFIG, null)
        return if (configJson != null) {
            try {
                gson.fromJson(configJson, BluetoothConfig::class.java) ?: BluetoothConfig()
            } catch (e: Exception) {
                BluetoothConfig() // 返回默认配置
            }
        } else {
            BluetoothConfig() // 返回默认配置
        }
    }

    // 加载配置列表
    private fun loadConfigList(): List<String> {
        val configListJson = preferences.getString(KEY_CONFIG_LIST, null)
        return if (configListJson != null) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(configListJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    // 更新MQTT配置
    fun updateMqttConfig(config: MqttConfig) {
        _mqttConfig.value = config
        saveMqttConfig(config)
    }

    // 更新蓝牙配置
    fun updateBluetoothConfig(config: BluetoothConfig) {
        _bluetoothConfig.value = config
        saveBluetoothConfig(config)
    }

    // 保存MQTT配置
    private fun saveMqttConfig(config: MqttConfig) {
        val configJson = gson.toJson(config)
        preferences.edit()
            .putString(KEY_MQTT_CONFIG, configJson)
            .apply()
    }

    // 保存蓝牙配置
    private fun saveBluetoothConfig(config: BluetoothConfig) {
        val configJson = gson.toJson(config)
        preferences.edit()
            .putString(KEY_BLUETOOTH_CONFIG, configJson)
            .apply()
    }

    // 保存完整配置组合
    fun saveConfig(name: String) {
        val deviceConfig = DeviceConfig(
            name = name,
            mqttConfig = _mqttConfig.value,
            bluetoothConfig = _bluetoothConfig.value
        )

        // 保存配置到SharedPreferences
        val configJson = gson.toJson(deviceConfig)
        preferences.edit()
            .putString(KEY_CONFIG_PREFIX + name, configJson)
            .apply()

        // 更新配置列表
        val currentList = _configList.value.toMutableList()
        if (!currentList.contains(name)) {
            currentList.add(name)
            _configList.value = currentList
            saveConfigList(currentList)
        }
    }

    // 加载完整配置组合
    fun loadConfig(name: String) {
        val configJson = preferences.getString(KEY_CONFIG_PREFIX + name, null)
        if (configJson != null) {
            try {
                val deviceConfig = gson.fromJson(configJson, DeviceConfig::class.java)
                if (deviceConfig != null) {
                    updateMqttConfig(deviceConfig.mqttConfig)
                    updateBluetoothConfig(deviceConfig.bluetoothConfig)
                }
            } catch (e: Exception) {
                // 加载失败，保持当前配置
            }
        }
    }

    // 删除保存的配置
    fun deleteConfig(name: String) {
        preferences.edit()
            .remove(KEY_CONFIG_PREFIX + name)
            .apply()

        val currentList = _configList.value.toMutableList()
        currentList.remove(name)
        _configList.value = currentList
        saveConfigList(currentList)
    }

    // 保存配置列表
    private fun saveConfigList(list: List<String>) {
        val listJson = gson.toJson(list)
        preferences.edit()
            .putString(KEY_CONFIG_LIST, listJson)
            .apply()
    }

    // 获取当前MQTT配置（用于其他组件访问）
    fun getCurrentMqttConfig(): MqttConfig = _mqttConfig.value

    // 获取当前蓝牙配置（用于其他组件访问）
    fun getCurrentBluetoothConfig(): BluetoothConfig = _bluetoothConfig.value
}
