package com.autobook.lingxi.ai

import retrofit2.http.Body
import retrofit2.http.POST

interface LLMService {
    // 标准的 Chat Completion 接口路径
    @POST("v1/chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}