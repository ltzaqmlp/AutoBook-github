package com.autobook.lingxi.observer

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.autobook.lingxi.service.RecognitionWorker

class ScreenshotObserver(
    private val context: Context,
    handler: Handler = Handler(Looper.getMainLooper())
) : ContentObserver(handler) {

    // 【新增】记录上一次处理的图片 ID，防止重复触发
    private var lastProcessedId: Long = -1L

    // 【新增】记录上一次处理的时间，防止极短时间内重复处理同一ID（双重保险）
    private var lastProcessedTime: Long = 0L

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        handleMediaChange()
    }

    private fun handleMediaChange() {
        val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            // 只查询最新的一条
            context.contentResolver.query(contentUri, projection, null, null, sortOrder)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                    val pathCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)

                    val id = cursor.getLong(idCol)
                    val path = cursor.getString(pathCol) ?: ""
                    val name = cursor.getString(nameCol) ?: ""

                    // 【核心修复】防抖动检查
                    // 1. 如果这张图的 ID 和上次一样，说明是重复通知，直接跳过
                    if (id == lastProcessedId) {
                        return
                    }

                    // 2. 更新最后处理的 ID
                    lastProcessedId = id

                    // 如果是截图，则构造 Uri 并发送
                    if (isScreenshot(path, name)) {
                        val imageUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                        Log.d("AutoBook", "📸 检测到新截图 (ID=$id)，准备分析...")
                        triggerRecognitionWork(imageUri.toString())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AutoBook", "读取媒体库崩溃", e)
        }
    }

    private fun isScreenshot(path: String, name: String): Boolean {
        val p = path.lowercase()
        val n = name.lowercase()
        return p.contains("screenshot") || p.contains("截屏") ||
                n.contains("screenshot") || n.contains("截屏")
    }

    private fun triggerRecognitionWork(imageUriString: String) {
        val inputData = workDataOf("IMAGE_PATH" to imageUriString)

        val workRequest = OneTimeWorkRequestBuilder<RecognitionWorker>()
            .setInputData(inputData)
            .addTag("OCR_TASK")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        Log.d("AutoBook", "🚀 任务已提交给 WorkManager 队列")
    }
}