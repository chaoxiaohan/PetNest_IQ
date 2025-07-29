package com.example.petnestiq

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.petnestiq.navigation.MainNavigation
import com.example.petnestiq.service.NotificationService
import com.example.petnestiq.service.HuaweiIoTDAMqttService
import com.example.petnestiq.ui.theme.PetNestIQTheme

class MainActivity : ComponentActivity() {

    // MQTT服务实例
    private val mqttService = HuaweiIoTDAMqttService.getInstance()

    // 通知权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 权限被授予，可以发送通知
        } else {
            // 权限被拒绝，可以显示说明或引导用户到设置
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化通知渠道
        NotificationService.createNotificationChannels(this)

        // 请求通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 初始化并连接MQTT服务
        initializeMqttService()

        enableEdgeToEdge()
        setContent {
            PetNestIQTheme(
                // 在Android 12+设备上启用动态颜色（Material You）
                dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }

    // 初始化并连接MQTT服务,HuaweiioT文件222行更改service_id
    private fun initializeMqttService() {
        // 配置华为云IOTDA MQTT连接参数
        val mqttConfig = HuaweiIoTDAMqttService.MqttConfig(
            serverUri = "ssl://e35491cb0c.st1.iotda-device.cn-north-4.myhuaweicloud.com:8883",
            deviceId = "688879e2d582f20018403921_text1",
            deviceSecret = "",  // 不需要密钥，直接使用提供的密码
            clientId = "688879e2d582f20018403921_text1_0_0_2025072907",
            username = "688879e2d582f20018403921_text1",
            password = "bdd6a2f87eab3e9dd81325957547fa3b5b566f1abfbdb9850249b3b4984f277e",
            port = 8883
        )

        // 配置MQTT服务
        mqttService.configure(mqttConfig)

        // 尝试连接MQTT服务
        mqttService.connect(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理MQTT服务资源
        mqttService.cleanup()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    PetNestIQTheme(
        dynamicColor = true // Preview中启用动态颜色预览
    ) {
        MainNavigation()
    }
}