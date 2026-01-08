package com.autobook.lingxi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.autobook.lingxi.observer.ScreenshotObserver
import com.autobook.lingxi.ui.theme.AutoBookTheme // 如果报红，检查下你的 theme 包名

class MainActivity : ComponentActivity() {

    private var screenshotObserver: ScreenshotObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 注意：如果你的项目名不是 AutoBook，这里的主题名可能会不同，请保持默认或根据实际情况修改
            AutoBookTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    // 注册监听器
    private fun registerObserver() {
        if (screenshotObserver == null) {
            screenshotObserver = ScreenshotObserver(this)
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                screenshotObserver!!
            )
        }
    }

    // 注销监听器 (防止内存泄漏)
    private fun unregisterObserver() {
        screenshotObserver?.let {
            contentResolver.unregisterContentObserver(it)
            screenshotObserver = null
        }
    }

    // 为了简单演示，我们在 Start/Stop 生命周期管理监听
    // 实际产品中可能需要放在 Service 里以保活
    override fun onStart() {
        super.onStart()
        // 检查权限，如果有权限则直接注册
        if (hasStoragePermission()) {
            registerObserver()
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterObserver()
    }

    private fun hasStoragePermission(): Boolean {
        // Android 13 (Tiramisu) 及以上使用 READ_MEDIA_IMAGES
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    // 把 registerObserver 暴露给 Compose 调用
    fun enableObservation() {
        registerObserver()
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val activity = context as? MainActivity

    // 动态权限申请器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "权限已获取，开始监听截图", Toast.LENGTH_SHORT).show()
            activity?.enableObservation()
        } else {
            Toast.makeText(context, "需要权限才能自动记账", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "AutoBook 灵犀记账", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "状态：监听服务运行中...")
        Spacer(modifier = Modifier.height(40.dp))

        Button(onClick = {
            // Android 13+ 和 旧版本区分
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionLauncher.launch(permission)
        }) {
            Text(text = "授予相册权限 (启动监听)")
        }
    }
}