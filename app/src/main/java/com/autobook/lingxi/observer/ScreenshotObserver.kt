package com.autobook.lingxi.observer

import android.content.ContentUris // ✅ 关键修复：补上了这个引用
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
            context.contentResolver.query(contentUri, projection, null, null, sortOrder)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                    val pathCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)

                    val id = cursor.getLong(idCol)
                    val path = cursor.getString(pathCol) ?: ""
                    val name = cursor.getString(nameCol) ?: ""

                    // 如果是截图，则构造 Uri 并发送
                    if (isScreenshot(path, name)) {
                        // 构造 content:// 格式的 Uri
                        val imageUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                        Log.d("AutoBook", "检测到截图，发送 Uri: $imageUri")

                        // 发送 Uri 字符串给 Worker
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
        // 1. 封装数据 (注意：key 还是 IMAGE_PATH，但 value 是 Uri 字符串)
        val inputData = workDataOf("IMAGE_PATH" to imageUriString)

        // 2. 创建任务
        val workRequest = OneTimeWorkRequestBuilder<RecognitionWorker>()
            .setInputData(inputData)
            .addTag("OCR_TASK")
            .build()

        // 3. 提交
        WorkManager.getInstance(context).enqueue(workRequest)
        Log.d("AutoBook", "🚀 任务已提交给 WorkManager 队列")
    }
}