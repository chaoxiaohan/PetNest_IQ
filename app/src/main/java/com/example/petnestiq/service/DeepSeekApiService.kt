package com.example.petnestiq.service

import com.example.petnestiq.data.ApiMessage
import com.example.petnestiq.data.DeepSeekRequest
import com.example.petnestiq.data.DeepSeekResponse
import com.example.petnestiq.data.StreamResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.ResponseBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * DeepSeek API 服务接口
 */
interface DeepSeekApiService {

    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: DeepSeekRequest
    ): Response<DeepSeekResponse>

    @Streaming
    @POST("chat/completions")
    suspend fun chatCompletionStream(
        @Header("Authorization") authorization: String,
        @Body request: DeepSeekRequest
    ): Response<ResponseBody>

    companion object {
        private const val BASE_URL = "https://api.deepseek.com/v1/"
        private const val API_KEY = "sk-038c7eeafbe74e24849fbc2998dd22d5"

        private val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        private val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        private val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val instance: DeepSeekApiService = retrofit.create(DeepSeekApiService::class.java)

        /**
         * 发送聊天消息到DeepSeek API（流式响应）
         */
        fun sendMessageStream(message: String): Flow<String> = flow {
            try {
                val messages = listOf(
                    ApiMessage(role = "system", content = "你是一个专业的宠物护理助手，专门为宠物主人提供关于宠物健康、营养、行为和护理的建议。请用友好、专业的语气回答用户的问题。"),
                    ApiMessage(role = "user", content = message)
                )

                val request = DeepSeekRequest(
                    model = "deepseek-chat",
                    messages = messages,
                    temperature = 0.7,
                    max_tokens = 1000,
                    stream = true
                )

                val response = instance.chatCompletionStream(
                    authorization = "Bearer $API_KEY",
                    request = request
                )

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
                        val gson = Gson()

                        reader.useLines { lines ->
                            lines.forEach { line ->
                                if (line.startsWith("data: ") && !line.contains("[DONE]")) {
                                    try {
                                        val jsonData = line.removePrefix("data: ")
                                        val streamResponse = gson.fromJson(jsonData, StreamResponse::class.java)
                                        val content = streamResponse.choices.firstOrNull()?.delta?.content
                                        if (!content.isNullOrEmpty()) {
                                            emit(content)
                                        }
                                    } catch (e: Exception) {
                                        // 忽略解析错误，继续处理下一行
                                    }
                                }
                            }
                        }
                    }
                } else {
                    emit("网络请求失败：${response.code()}")
                }
            } catch (e: Exception) {
                emit("发生错误：${e.message}")
            }
        }

        /**
         * 发送聊天消息到DeepSeek API
         */
        suspend fun sendMessage(message: String): String {
            return try {
                val messages = listOf(
                    ApiMessage(role = "system", content = "你是一个专业的宠物护理助手，专门为宠物主人提供关于宠物健康、营养、行为和护理的建议。请用友好、专业的语气回答用户的问题。"),
                    ApiMessage(role = "user", content = message)
                )

                val request = DeepSeekRequest(
                    model = "deepseek-chat",
                    messages = messages,
                    temperature = 0.7,
                    max_tokens = 1000,
                    stream = false
                )

                val response = instance.chatCompletion(
                    authorization = "Bearer $API_KEY",
                    request = request
                )

                if (response.isSuccessful) {
                    response.body()?.choices?.firstOrNull()?.message?.content
                        ?: "抱歉，我现在无法回答您的问题。"
                } else {
                    "网络请求失败：${response.code()}"
                }
            } catch (e: Exception) {
                "发生错误：${e.message}"
            }
        }
    }
}
