package com.autobook.lingxi.ai

import android.util.Log
import com.autobook.lingxi.data.BillEntity
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.text.SimpleDateFormat
import java.util.Locale

object SmartBillParser {

    /**
     * 核心方法：将 OCR 文本发送给 AI，并返回解析后的账单对象
     */
    suspend fun parseOcrText(rawText: String): BillEntity? {
        Log.d("SmartBillParser", "正在请求 AI 解析文本: ${rawText.take(20)}...")

        // 1. 🔥 核心 Prompt (提示词) 设计
        // 告诉 AI 它的角色，以及我们强制要求的 JSON 格式
        val systemPrompt = """
            你是一个专业的账单解析助手。请从用户提供的 OCR 识别文本中提取以下关键信息：
            1. 商户名称 (merchant): 消费的店名、品牌名。如果找不到，根据内容推断（如看到"红烧肉"推断为"餐饮"）。
            2. 金额 (amount): 纯数字，保留两位小数。
            3. 时间 (timestamp): 格式为 yyyy-MM-dd HH:mm:ss。如果文本中只有时间没有日期，默认为今天。
            4. 类型 (type): 默认为 "支出"。
            
            ⚠️ 严格要求：
            - 请直接返回标准的 JSON 格式字符串。
            - 不要包含 Markdown 标记（如 ```json ... ```）。
            - 如果完全无法识别为账单，请返回 null。
            
            JSON 示例:
            {
              "merchant": "罗森便利店",
              "amount": 25.50,
              "time": "2023-10-25 14:30:00",
              "type": "支出"
            }
        """.trimIndent()

        // 2. 构造消息链
        val messages = listOf(
            Message(role = "system", content = systemPrompt),
            Message(role = "user", content = rawText)
        )

        // 3. 构造请求体
        val request = ChatRequest(
            model = LLMClient.MODEL_NAME, // 使用我们在 LLMClient 定义的模型 (如 deepseek-chat)
            messages = messages,
            temperature = 0.1 // 温度设低一点，让 AI 回答更严谨、格式更固定
        )

        return try {
            // 4. 发起网络请求 (挂起函数)
            val response = LLMClient.service.chat(request)

            // 5. 获取 AI 回复的内容
            val content = response.choices.firstOrNull()?.message?.content
            if (content.isNullOrBlank()) {
                Log.e("SmartBillParser", "AI 返回内容为空")
                return null
            }

            Log.d("SmartBillParser", "AI 原始回复: $content")

            // 6. 数据清洗 (去除可能存在的 Markdown 代码块标记)
            val jsonString = content
                .replace("```json", "")
                .replace("```", "")
                .trim()

            // 7. 解析 JSON (使用 Gson)
            val jsonObject = Gson().fromJson(jsonString, JsonObject::class.java)

            // 提取字段 (带容错处理)
            val merchant = if (jsonObject.has("merchant")) jsonObject.get("merchant").asString else "未知商户"
            val amount = if (jsonObject.has("amount")) jsonObject.get("amount").asDouble else 0.0
            val type = if (jsonObject.has("type")) jsonObject.get("type").asString else "支出"
            val timeStr = if (jsonObject.has("time")) jsonObject.get("time").asString else ""

            // 8. 处理时间 (转为 Long 时间戳)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val timestamp = try {
                dateFormat.parse(timeStr)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                // 如果 AI 返回的时间格式不对，就用当前时间兜底
                System.currentTimeMillis()
            }

            // 9. 返回最终的账单实体
            // (假设 id 默认为 0，由 Room 自动生成)
            BillEntity(
                merchant = merchant,
                amount = amount,
                timestamp = timestamp,
                type = type,
                category = "AI识别" // 标记一下这是 AI 记的账
            )

        } catch (e: Exception) {
            Log.e("SmartBillParser", "AI 解析失败: ${e.message}", e)
            null
        }
    }
}