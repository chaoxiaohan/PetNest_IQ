package com.example.petnestiq.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petnestiq.data.ChatMessage
import com.example.petnestiq.service.DeepSeekApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * AI聊天界面的ViewModel
 */
class AiChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 发送消息给AI
     */
    fun sendMessage(content: String) {
        if (content.isBlank() || _isLoading.value) return

        // 添加用户消息
        val userMessage = ChatMessage(
            content = content,
            isFromUser = true
        )
        _messages.value = _messages.value + userMessage

        // 开始加载
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // 调用DeepSeek API
                val aiResponse = DeepSeekApiService.sendMessage(content)

                // 添加AI回复
                val aiMessage = ChatMessage(
                    content = aiResponse,
                    isFromUser = false
                )
                _messages.value = _messages.value + aiMessage

            } catch (e: Exception) {
                // 添加错误消息
                val errorMessage = ChatMessage(
                    content = "抱歉，我现在遇到了一些问题，请稍后重试。",
                    isFromUser = false
                )
                _messages.value = _messages.value + errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 发送消息给AI（逐字符显示）
     */
    fun sendMessageStream(content: String) {
        if (content.isBlank() || _isLoading.value) return

        // 添加用户消息
        val userMessage = ChatMessage(
            content = content,
            isFromUser = true
        )
        _messages.value = _messages.value + userMessage

        // 开始加载
        _isLoading.value = true

        // 创建一个空的AI消息，用于逐字符更新
        val aiMessageId = UUID.randomUUID().toString()
        val initialAiMessage = ChatMessage(
            id = aiMessageId,
            content = "",
            isFromUser = false,
            isStreaming = true
        )
        _messages.value = _messages.value + initialAiMessage

        viewModelScope.launch {
            try {
                // 首先获取完整的AI回复
                val fullResponse = DeepSeekApiService.sendMessage(content)

                // 然后逐字符显示
                displayTextCharByChar(aiMessageId, fullResponse)

            } catch (e: Exception) {
                // 添加错误消息
                val errorMessage = ChatMessage(
                    content = "抱歉，我现在遇到了一些问题，请稍后重试。",
                    isFromUser = false
                )

                // 移除之前的空AI消息，添加错误消息
                _messages.value = _messages.value.filter { it.id != aiMessageId } + errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 逐字符显示文本
     */
    private suspend fun displayTextCharByChar(messageId: String, fullText: String) {
        var displayedText = ""

        for (char in fullText) {
            displayedText += char

            // 更新消息内容
            _messages.value = _messages.value.map { message ->
                if (message.id == messageId) {
                    message.copy(
                        content = displayedText,
                        isStreaming = true
                    )
                } else {
                    message
                }
            }

            // 根据字符类型调整显示速度
            val delayTime = when {
                char == '\n' -> 100L // 换行符稍微长一点
                char.isWhitespace() -> 20L // 空格较快
                isPunctuation(char) -> 80L // 标点符号
                char.code > 127 -> 50L // 中文字符
                else -> 30L // 英文字符
            }

            delay(delayTime)
        }

        // 显示完成，更新最终状态
        _messages.value = _messages.value.map { message ->
            if (message.id == messageId) {
                message.copy(isStreaming = false)
            } else {
                message
            }
        }
    }

    /**
     * 清空聊天记录
     */
    fun clearChat() {
        _messages.value = emptyList()
    }

    /**
     * 判断字符是否为标点符号
     */
    private fun isPunctuation(char: Char): Boolean {
        // 简单判断常见标点符号
        return char in "，。！？；：,.!?"
    }
}
