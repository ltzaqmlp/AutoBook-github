package com.autobook.lingxi.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.hzkitty.RapidOCR
import io.github.hzkitty.entity.OcrResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class RecognitionWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // 1. 获取传递过来的 Uri 字符串
        val imageUriString = inputData.getString("IMAGE_PATH") ?: return Result.failure()
        val imageUri = Uri.parse(imageUriString)

        Log.d("AutoBook", "✅ WorkManager 启动，准备读取 Uri: $imageUri")

        return withContext(Dispatchers.IO) {
            try {
                // 2. 【核心修改】通过 ContentResolver 读取流
                val bitmap = decodeBitmapFromUri(context, imageUri)

                if (bitmap == null) {
                    Log.e("AutoBook", "❌ 图片加载失败，无法解码 Uri")
                    return@withContext Result.failure()
                }

                // 3. 识别
                val result = runRapidOCR(context, bitmap)
                val rawText = result.strRes

                Log.i("AutoBook", "🎉 识别成功! 原始内容如下:\n${result.strRes}")

                // 4. 【新增】调用规则引擎进行分析
                val billInfo = com.autobook.lingxi.logic.BillParser.parse(rawText)

                if (billInfo != null) {
                    Log.d("AutoBook", "✅ 规则引擎提取成功! \n金额: ${billInfo.amount} \n商户: ${billInfo.merchant}")
                    // TODO: 存入数据库
                } else {
                    Log.w("AutoBook", "⚠️ 规则引擎无法识别此账单 (稍后将交给 AI 大模型处理)")
                }

                bitmap.recycle()
                Result.success()
            } catch (e: Exception) {
                Log.e("AutoBook", "识别过程崩溃", e)
                Result.retry()
            }
        }
    }

    // 辅助方法：安全地从 Uri 加载 Bitmap
    private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            return BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e("AutoBook", "流读取异常", e)
        } finally {
            inputStream?.close()
        }
        return null
    }

    private fun runRapidOCR(context: Context, bitmap: Bitmap): OcrResult {
        Log.d("AutoBook", "⚡ 正在初始化 RapidOCR 引擎...")
        val rapidOCR = RapidOCR.create(context)
        Log.d("AutoBook", "👀 正在进行 OCR 推理...")
        return rapidOCR.run(bitmap)
    }
}