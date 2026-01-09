package com.autobook.lingxi.ai

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object LLMClient {
    // 🔥【重要】请替换为你申请的 API 地址
    // 例如 DeepSeek: "https://api.deepseek.com/"
    // 例如 Moonshot (Kimi): "https://api.moonshot.cn/"
    private const val BASE_URL = "https://api.deepseek.com/v1"

    // 🔥【重要】请替换为你的 API Key (以 sk- 开头)
    private const val API_KEY = "sk-87ecf15cae754c139d3cc67dfc685240"

    // 我们在这个模型上表现最好
    const val MODEL_NAME = "deepseek-chat"

    private val client = OkHttpClient.Builder()
        // AI 思考有时需要较长时间，超时设长一点 (60秒)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        // 添加日志拦截器，方便在 Logcat 看 AI 回了什么
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    // 创建 Retrofit 实例
    val service: LLMService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(LLMService::class.java)
}