package com.example.petnestiq.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream

/**
 * 音频录制管理器
 * 用于录制音频并通过MQTT发送语音数据
 */
class AudioRecordManager private constructor() {

    companion object {
        private const val TAG = "AudioRecordManager"

        // 音频录制参数
        private const val SAMPLE_RATE = 16000  // 采样率 16kHz
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO  // 单声道
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT  // 16位PCM
        private const val BUFFER_SIZE_FACTOR = 2  // 缓冲区大小倍数

        @Volatile
        private var INSTANCE: AudioRecordManager? = null

        fun getInstance(): AudioRecordManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioRecordManager().also { INSTANCE = it }
            }
        }
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 录制状态
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // 录制音量级别（用于UI显示）
    private val _volumeLevel = MutableStateFlow(0f)
    val volumeLevel: StateFlow<Float> = _volumeLevel.asStateFlow()

    // MQTT服务实例
    private val mqttService = HuaweiIoTDAMqttService.getInstance()

    // 计算缓冲区大小
    private val bufferSize: Int by lazy {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        minBufferSize * BUFFER_SIZE_FACTOR
    }

    /**
     * 检查录音权限
     */
    fun hasRecordPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 开始录音
     */
    fun startRecording(context: Context) {
        if (_isRecording.value) {
            Log.w(TAG, "已经在录音中")
            return
        }

        if (!hasRecordPermission(context)) {
            Log.e(TAG, "没有录音权限")
            return
        }

        try {
            // 创建AudioRecord实例
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord初始化失败")
                releaseAudioRecord()
                return
            }

            // 开始录制
            audioRecord?.startRecording()
            _isRecording.value = true

            // 启动录制协程
            recordingJob = recordingScope.launch {
                recordAudio()
            }

            Log.d(TAG, "开始录音")

        } catch (e: Exception) {
            Log.e(TAG, "开始录音失败", e)
            releaseAudioRecord()
        }
    }

    /**
     * 停止录音
     */
    fun stopRecording() {
        if (!_isRecording.value) {
            Log.w(TAG, "当前没有在录音")
            return
        }

        try {
            _isRecording.value = false
            recordingJob?.cancel()
            audioRecord?.stop()
            releaseAudioRecord()
            _volumeLevel.value = 0f

            Log.d(TAG, "停止录音")

        } catch (e: Exception) {
            Log.e(TAG, "停止录音失败", e)
        }
    }

    /**
     * 录制音频数据
     */
    private suspend fun recordAudio() {
        val audioData = ByteArrayOutputStream()
        val buffer = ByteArray(bufferSize)

        try {
            while (_isRecording.value && !recordingJob?.isCancelled!!) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                if (bytesRead > 0) {
                    // 保存音频数据
                    audioData.write(buffer, 0, bytesRead)

                    // 计算音量级别
                    val volumeLevel = calculateVolumeLevel(buffer, bytesRead)
                    _volumeLevel.value = volumeLevel

                    // 每隔一定时间发送一次数据（流式传输）
                    if (audioData.size() >= SAMPLE_RATE * 2) { // 约1秒的数据
                        val audioBytes = audioData.toByteArray()
                        audioData.reset()

                        // 发送音频数据到MQTT
                        mqttService.sendVoiceData(audioBytes)
                    }
                }

                // 短暂延迟，避免过度消耗CPU
                delay(10)
            }

            // 录制结束后，发送剩余的音频数据
            if (audioData.size() > 0) {
                val audioBytes = audioData.toByteArray()
                mqttService.sendVoiceData(audioBytes)
            }

        } catch (e: Exception) {
            Log.e(TAG, "录制音频数据失败", e)
        } finally {
            audioData.close()
        }
    }

    /**
     * 计算音量级别
     */
    private fun calculateVolumeLevel(buffer: ByteArray, bytesRead: Int): Float {
        var sum = 0.0
        for (i in 0 until bytesRead step 2) {
            if (i + 1 < bytesRead) {
                // 将两个字节组合成一个16位采样
                val sample = (buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)
                sum += (sample * sample).toDouble()
            }
        }

        val rms = kotlin.math.sqrt(sum / (bytesRead / 2))
        // 归一化到0-1范围
        return (rms / 32768.0).toFloat().coerceIn(0f, 1f)
    }

    /**
     * 释放AudioRecord资源
     */
    private fun releaseAudioRecord() {
        try {
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "释放AudioRecord失败", e)
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        stopRecording()
        recordingScope.cancel()
    }
}
