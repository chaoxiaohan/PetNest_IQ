package com.example.petnestiq.service

import android.content.Context
import android.util.Log
import com.example.petnestiq.data.DeviceConfigManager
import com.example.petnestiq.data.BluetoothConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 蓝牙设备管理服务
 * 负责蓝牙设置信息的存储和管理，为后续蓝牙功能扩展做准备
 */
class BluetoothService private constructor() {

    companion object {
        private const val TAG = "BluetoothService"

        // 连接状态常量
        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
        const val STATE_FAILED = 3

        @Volatile
        private var INSTANCE: BluetoothService? = null

        fun getInstance(): BluetoothService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BluetoothService().also { INSTANCE = it }
            }
        }
    }

    // 设备配置管理器
    private var deviceConfigManager: DeviceConfigManager? = null

    // 连接状态
    private val _connectionState = MutableStateFlow(STATE_DISCONNECTED)
    val connectionState: StateFlow<Int> = _connectionState.asStateFlow()

    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 设备信息
    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    // 应用上下文
    private var appContext: Context? = null

    /**
     * 初始化蓝牙服务
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        deviceConfigManager = DeviceConfigManager.getInstance(context)
        Log.i(TAG, "蓝牙服务初始化完成")
    }

    /**
     * 获取当前蓝牙配置
     */
    fun getCurrentBluetoothConfig(): BluetoothConfig? {
        return deviceConfigManager?.getCurrentBluetoothConfig()
    }

    /**
     * 更新蓝牙配置
     */
    fun updateBluetoothConfig(config: BluetoothConfig) {
        deviceConfigManager?.updateBluetoothConfig(config)
        Log.d(TAG, "蓝牙配置已更新: ${config.deviceName} (${config.macAddress})")
    }

    /**
     * 获取连接状态描述
     */
    fun getConnectionStateDescription(): String {
        return when (_connectionState.value) {
            STATE_DISCONNECTED -> "未连接"
            STATE_CONNECTING -> "连接中"
            STATE_CONNECTED -> "已连接"
            STATE_FAILED -> "连接失败"
            else -> "未知状态"
        }
    }

    /**
     * 模拟连接测试（为后续实际蓝牙功能预留接口）
     */
    fun testConnection(): Boolean {
        val config = getCurrentBluetoothConfig() ?: run {
            _errorMessage.value = "蓝牙配置未设置"
            return false
        }

        if (config.macAddress.isBlank()) {
            _errorMessage.value = "MAC地址未配置"
            return false
        }

        // 模拟连接过程
        _connectionState.value = STATE_CONNECTING
        _errorMessage.value = null

        // 这里为后续实际蓝牙连接功能预留扩展空间
        Log.d(TAG, "模拟测试连接到: ${config.deviceName} (${config.macAddress})")

        // 模拟连接成功
        _connectionState.value = STATE_CONNECTED
        _connectedDeviceName.value = config.deviceName

        return true
    }

    /**
     * 断开连接（为后续功能预留）
     */
    fun disconnect() {
        _connectionState.value = STATE_DISCONNECTED
        _connectedDeviceName.value = null
        _errorMessage.value = null
        Log.d(TAG, "蓝牙连接已断开")
    }

    /**
     * 清理错误消息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 检查是否应该自动连接
     */
    fun shouldAutoConnect(): Boolean {
        val config = getCurrentBluetoothConfig()
        return config?.autoConnect == true && config.macAddress.isNotBlank()
    }

    /**
     * 获取配置的连接超时时间
     */
    fun getConnectionTimeout(): Int {
        return getCurrentBluetoothConfig()?.connectionTimeout ?: 30
    }

    /**
     * 验证MAC地址格式
     */
    fun isValidMacAddress(address: String): Boolean {
        val macPattern = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"
        return address.matches(macPattern.toRegex())
    }

    /**
     * 释放资源
     */
    fun release() {
        disconnect()
        Log.d(TAG, "蓝牙服务已释放")
    }
}
