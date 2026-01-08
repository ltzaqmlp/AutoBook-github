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
import com.autobook.lingxi.data.AppDatabase
import com.autobook.lingxi.data.BillEntity

class RecognitionWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val imageUriString = inputData.getString("IMAGE_PATH") ?: return Result.failure()
        val imageUri = Uri.parse(imageUriString)

        Log.d("AutoBook", "✅ WorkManager 启动，准备读取 Uri: $imageUri")

        return withContext(Dispatchers.IO) {
            try {
                val bitmap = decodeBitmapFromUri(context, imageUri)
                if (bitmap == null) {
                    Log.e("AutoBook", "❌ 图片加载失败")
                    return@withContext Result.failure()
                }

                val result = runRapidOCR(context, bitmap)
                val rawText = result.strRes
                // Log.i("AutoBook", "OCR 原始内容:\n$rawText") // 日志太多可以注释掉这行

                // 调用升级版规则引擎
                val billList = com.autobook.lingxi.logic.BillParser.parse(rawText)

                if (billList.isNotEmpty()) {
                    Log.d("AutoBook", "✅ 成功提取到 ${billList.size} 笔账单！准备存入数据库...")

                    // 1. 转换数据格式 (BillInfo -> BillEntity)
                    val entities = billList.map { bill ->
                        com.autobook.lingxi.data.BillEntity(
                            amount = bill.amount,
                            merchant = bill.merchant,
                            dateStr = bill.date,
                            timestamp = System.currentTimeMillis(),
                            type = if (bill.amount > 0) "支出" else "收入" // 简单判断
                        )
                    }

                    // 2. 获取数据库实例
                    val database = com.autobook.lingxi.data.AppDatabase.getDatabase(context)

                    // 3. 存入数据库
                    database.billDao().insertAll(entities)

                    // 4. 验证一下是否存进去了
                    val count = database.billDao().getCount()
                    Log.i("AutoBook", "💾 数据保存成功！当前数据库里共有 $count 笔账单。")

                } else {
                    Log.w("AutoBook", "⚠️ 规则引擎未提取到有效账单")
                }

                bitmap.recycle()
                Result.success()
            } catch (e: Exception) {
                Log.e("AutoBook", "识别过程崩溃", e)
                Result.retry()
            }
        }
    }

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