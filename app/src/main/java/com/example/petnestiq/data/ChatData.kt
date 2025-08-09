package com.example.petnestiq.data

import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 聊天消息数据类
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isTyping: Boolean = false,
    val isStreaming: Boolean = false
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

/**
 * DeepSeek API 请求数据模型
 */
data class DeepSeekRequest(
    val model: String = "deepseek-chat",
    val messages: List<ApiMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1000,
    val stream: Boolean = false
)

data class ApiMessage(
    val role: String, // "user" 或 "assistant" 或 "system"
    val content: String
)

/**
 * DeepSeek API 响应数据模型
 */
data class DeepSeekResponse(
    val id: String,
    val objectType: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage
)

data class Choice(
    val index: Int,
    val message: ApiMessage,
    val finish_reason: String
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

/**
 * 流式响应数据模型
 */
data class StreamChoice(
    val index: Int,
    val delta: StreamDelta,
    val finish_reason: String?
)

data class StreamDelta(
    val content: String?
)

data class StreamResponse(
    val id: String,
    val objectType: String,
    val created: Long,
    val model: String,
    val choices: List<StreamChoice>
)
