package com.autobook.lingxi.ai

// 发送给 AI 的请求结构
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.1, // 温度越低，回答越严谨固定
    val stream: Boolean = false
)

// 消息体结构
data class Message(
    val role: String, // "user" 或 "system" 或 "assistant"
    val content: String
)

// AI 返回的响应结构
data class ChatResponse(
    val id: String,
    val choices: List<Choice>
)

data class Choice(
    val index: Int,
    val message: Message
)