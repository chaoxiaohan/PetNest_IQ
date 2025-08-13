package com.example.petnestiq.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petnestiq.data.DeviceConfigManager
import com.example.petnestiq.data.MqttConfig
import com.example.petnestiq.data.BluetoothConfig
import com.example.petnestiq.service.BluetoothService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val deviceConfigManager = remember { DeviceConfigManager.getInstance(context) }
    val bluetoothService = remember { BluetoothService.getInstance() }

    // 获取配置状态
    val mqttConfig by deviceConfigManager.mqttConfig.collectAsStateWithLifecycle()
    val bluetoothConfig by deviceConfigManager.bluetoothConfig.collectAsStateWithLifecycle()
    val configList by deviceConfigManager.configList.collectAsStateWithLifecycle()

    // 获取蓝牙服务状态
    val bluetoothConnectionState by bluetoothService.connectionState.collectAsStateWithLifecycle()
    val bluetoothError by bluetoothService.errorMessage.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    var configName by remember { mutableStateOf("") }
    var isTestingBluetooth by remember { mutableStateOf(false) }

    // 初始化蓝牙服务
    LaunchedEffect(Unit) {
        bluetoothService.initialize(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "设备管理",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 保存配置按钮
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.Save, contentDescription = "保存配置")
                    }
                    // 加载配置按钮
                    IconButton(onClick = { showLoadDialog = true }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "加载配置")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // MQTT设置板块
            MqttSettingsSection(
                mqttConfig = mqttConfig,
                onConfigChange = { newConfig ->
                    deviceConfigManager.updateMqttConfig(newConfig)
                }
            )

            // 蓝牙设置板块
            BluetoothSettingsSection(
                bluetoothConfig = bluetoothConfig,
                connectionState = bluetoothConnectionState,
                errorMessage = bluetoothError,
                isTestingConnection = isTestingBluetooth,
                onConfigChange = { newConfig ->
                    deviceConfigManager.updateBluetoothConfig(newConfig)
                    bluetoothService.updateBluetoothConfig(newConfig)
                },
                onTestConnection = {
                    isTestingBluetooth = true
                    bluetoothService.testConnection()
                    // 延迟重置测试状态
                    kotlinx.coroutines.MainScope().launch {
                        delay(3000)
                        isTestingBluetooth = false
                    }
                }
            )
        }
    }

    // 保存配置对话框
    if (showSaveDialog) {
        SaveConfigDialog(
            configName = configName,
            onConfigNameChange = { configName = it },
            onSave = { name ->
                deviceConfigManager.saveConfig(name)
                showSaveDialog = false
                configName = ""
            },
            onDismiss = {
                showSaveDialog = false
                configName = ""
            }
        )
    }

    // 加载配置对话框
    if (showLoadDialog) {
        LoadConfigDialog(
            configList = configList,
            onLoad = { config ->
                deviceConfigManager.loadConfig(config)
                showLoadDialog = false
            },
            onDelete = { config ->
                deviceConfigManager.deleteConfig(config)
            },
            onDismiss = { showLoadDialog = false }
        )
    }
}

@Composable
fun MqttSettingsSection(
    mqttConfig: MqttConfig,
    onConfigChange: (MqttConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MQTT设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 服务器地址
            OutlinedTextField(
                value = mqttConfig.serverUrl,
                onValueChange = { onConfigChange(mqttConfig.copy(serverUrl = it)) },
                label = { Text("服务器地址") },
                placeholder = { Text("mqtt://example.com:1883") },
                modifier = Modifier.fillMaxWidth()
            )

            // 端口
            OutlinedTextField(
                value = mqttConfig.port.toString(),
                onValueChange = {
                    val port = it.toIntOrNull() ?: mqttConfig.port
                    onConfigChange(mqttConfig.copy(port = port))
                },
                label = { Text("端口") },
                placeholder = { Text("1883") },
                modifier = Modifier.fillMaxWidth()
            )

            // 客户端ID
            OutlinedTextField(
                value = mqttConfig.clientId,
                onValueChange = { onConfigChange(mqttConfig.copy(clientId = it)) },
                label = { Text("客户端ID") },
                placeholder = { Text("PetNestIQ_Client") },
                modifier = Modifier.fillMaxWidth()
            )

            // 用户名
            OutlinedTextField(
                value = mqttConfig.username,
                onValueChange = { onConfigChange(mqttConfig.copy(username = it)) },
                label = { Text("用户名") },
                placeholder = { Text("用户名(可选)") },
                modifier = Modifier.fillMaxWidth()
            )

            // 密码
            OutlinedTextField(
                value = mqttConfig.password,
                onValueChange = { onConfigChange(mqttConfig.copy(password = it)) },
                label = { Text("密码") },
                placeholder = { Text("密码(可选)") },
                modifier = Modifier.fillMaxWidth()
            )

            // 订阅主题
            OutlinedTextField(
                value = mqttConfig.subscribeTopic,
                onValueChange = { onConfigChange(mqttConfig.copy(subscribeTopic = it)) },
                label = { Text("订阅主题") },
                placeholder = { Text("/device/data") },
                modifier = Modifier.fillMaxWidth()
            )

            // 发布主题
            OutlinedTextField(
                value = mqttConfig.publishTopic,
                onValueChange = { onConfigChange(mqttConfig.copy(publishTopic = it)) },
                label = { Text("发布主题") },
                placeholder = { Text("/device/control") },
                modifier = Modifier.fillMaxWidth()
            )

            // SSL连接开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SSL连接")
                Switch(
                    checked = mqttConfig.useSSL,
                    onCheckedChange = { onConfigChange(mqttConfig.copy(useSSL = it)) }
                )
            }

            // 自动重连开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("自动重连")
                Switch(
                    checked = mqttConfig.autoReconnect,
                    onCheckedChange = { onConfigChange(mqttConfig.copy(autoReconnect = it)) }
                )
            }
        }
    }
}

@Composable
fun BluetoothSettingsSection(
    bluetoothConfig: BluetoothConfig,
    connectionState: Int,
    errorMessage: String?,
    isTestingConnection: Boolean,
    onConfigChange: (BluetoothConfig) -> Unit,
    onTestConnection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "蓝牙设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 设备名称
            OutlinedTextField(
                value = bluetoothConfig.deviceName,
                onValueChange = { onConfigChange(bluetoothConfig.copy(deviceName = it)) },
                label = { Text("设备名称") },
                placeholder = { Text("PetNest Device") },
                modifier = Modifier.fillMaxWidth()
            )

            // MAC地址
            OutlinedTextField(
                value = bluetoothConfig.macAddress,
                onValueChange = { onConfigChange(bluetoothConfig.copy(macAddress = it)) },
                label = { Text("MAC地址") },
                placeholder = { Text("00:11:22:33:44:55") },
                modifier = Modifier.fillMaxWidth()
            )

            // 自动连接开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("启动时自动连接")
                    Text(
                        "APP启动时自动连接到设备",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Switch(
                    checked = bluetoothConfig.autoConnect,
                    onCheckedChange = { onConfigChange(bluetoothConfig.copy(autoConnect = it)) }
                )
            }

            // 连接超时设置
            Column {
                Text("连接超时 (秒): ${bluetoothConfig.connectionTimeout}")
                Slider(
                    value = bluetoothConfig.connectionTimeout.toFloat(),
                    onValueChange = {
                        onConfigChange(bluetoothConfig.copy(connectionTimeout = it.toInt()))
                    },
                    valueRange = 5f..60f,
                    steps = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 连接状态和测试
            Column {
                val statusText = when (connectionState) {
                    BluetoothService.STATE_DISCONNECTED -> "未连接"
                    BluetoothService.STATE_CONNECTING -> "连接中"
                    BluetoothService.STATE_CONNECTED -> "已连接"
                    BluetoothService.STATE_FAILED -> "连接失败"
                    else -> "未知状态"
                }

                Text("连接状态: $statusText")
                errorMessage?.let {
                    Text("错误信息: $it", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onTestConnection,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTestingConnection
                ) {
                    Text(if (isTestingConnection) "测试中..." else "测试连接")
                }
            }
        }
    }
}

@Composable
fun SaveConfigDialog(
    configName: String,
    onConfigNameChange: (String) -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存配置") },
        text = {
            Column {
                Text("请输入配置名称：")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = configName,
                    onValueChange = onConfigNameChange,
                    label = { Text("配置名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (configName.isNotBlank()) {
                        onSave(configName.trim())
                    }
                },
                enabled = configName.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun LoadConfigDialog(
    configList: List<String>,
    onLoad: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加载配置") },
        text = {
            if (configList.isEmpty()) {
                Text("暂无已保存的配置")
            } else {
                Column {
                    Text("选择要加载的配置：")
                    Spacer(modifier = Modifier.height(8.dp))
                    configList.forEach { configName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { onLoad(configName) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = configName,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            IconButton(
                                onClick = { onDelete(configName) }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
