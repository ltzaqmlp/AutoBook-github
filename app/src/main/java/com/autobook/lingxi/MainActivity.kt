package com.autobook.lingxi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autobook.lingxi.data.BillEntity
import com.autobook.lingxi.observer.ScreenshotObserver
// ⚠️ 必须先完成 Step 1，这行引用才不会报错
import com.autobook.lingxi.ui.components.TrendChart
// ⚠️ 确保你已经有 BillViewModel 文件
import com.autobook.lingxi.viewmodel.BillViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog

class MainActivity : ComponentActivity() {
    private var screenshotObserver: ScreenshotObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            com.autobook.lingxi.ui.theme.AutoBookTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

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

    private fun unregisterObserver() {
        screenshotObserver?.let {
            contentResolver.unregisterContentObserver(it)
            screenshotObserver = null
        }
    }

    override fun onStart() {
        super.onStart()
        if (checkPermission(this)) {
            registerObserver()
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterObserver()
    }

    fun enableObservation() {
        registerObserver()
    }

    companion object {
        fun checkPermission(context: Context): Boolean {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val application = context.applicationContext as android.app.Application

    // 1. 初始化 ViewModel
    val billViewModel: BillViewModel = viewModel(factory = BillViewModel.Factory(application))

    // 2. 订阅数据
    val bills by billViewModel.allBills.collectAsState()
    val monthlyExpense by billViewModel.currentMonthExpense.collectAsState()
    val trendData by billViewModel.last7DaysTrend.collectAsState()

    // 3. 状态管理
    var hasPermission by remember { mutableStateOf(MainActivity.checkPermission(context)) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedBill by remember { mutableStateOf<BillEntity?>(null) }

    // 删除相关状态
    var billToDelete by remember { mutableStateOf<BillEntity?>(null) }
    var pendingCloseMenu by remember { mutableStateOf<(() -> Unit)?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            activity?.enableObservation()
            scope.launch { snackbarHostState.showSnackbar("监听已启动") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AutoBook 灵犀记账", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = {
                        scope.launch { snackbarHostState.showSnackbar("AI 深度分析功能开发中 (Phase 5)...") }
                    }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 本月支出卡片
            TotalExpenseCard(monthlyExpense)

            // 趋势图 (如果有数据)
            if (trendData.any { it.second > 0 }) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    TrendChart(data = trendData, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 权限提示
            if (!hasPermission) {
                PermissionWarningCard {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    permissionLauncher.launch(permission)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "近期账单",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 账单列表
            if (bills.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无数据，请尝试截取账单详情页", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(bills, key = { it.id }) { bill ->
                        // ✨ 带红色按钮的侧滑组件
                        SwipeToRevealItem(
                            onDeleteClick = { resetMenuCallback ->
                                billToDelete = bill
                                pendingCloseMenu = resetMenuCallback
                            }
                        ) {
                            BillItemCard(bill) {
                                selectedBill = bill
                                showEditDialog = true
                            }
                        }
                    }
                }
            }
        }
    }

    // 删除确认弹窗
    if (billToDelete != null) {
        DeleteConfirmationDialog(
            bill = billToDelete!!,
            onConfirm = {
                billViewModel.deleteBill(billToDelete!!)
                scope.launch { snackbarHostState.showSnackbar("已安全删除") }
                billToDelete = null
                pendingCloseMenu = null
            },
            onDismiss = {
                billToDelete = null
                pendingCloseMenu?.invoke()
                pendingCloseMenu = null
            }
        )
    }

    // 编辑弹窗
    if (showEditDialog && selectedBill != null) {
        EditBillDialog(
            bill = selectedBill!!,
            onDismiss = { showEditDialog = false },
            onDelete = {
                showEditDialog = false
                billToDelete = it
                pendingCloseMenu = null
            },
            onUpdate = {
                billViewModel.updateBill(it)
                showEditDialog = false
            }
        )
    }
}

// --- ✨ 修复版：绝对精准的侧滑组件 ✨ ---
@Composable
fun SwipeToRevealItem(
    onDeleteClick: (resetMenu: () -> Unit) -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val actionWidth = 80.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }

    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val resetMenu: () -> Unit = {
        scope.launch { offsetX.animateTo(0f) }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd // 1. 全局靠右
    ) {
        // --- 背景层 (红色圆圈按钮) ---
        Box(
            modifier = Modifier
                .matchParentSize(), // 2. 这个盒子负责填满高度，不限宽度
            contentAlignment = Alignment.CenterEnd // 3. 它的内容必须靠右
        ) {
            // 4. 这才是真正的红色按钮区域
            Box(
                modifier = Modifier
                    .width(actionWidth)     // 限制宽度 80dp
                    .fillMaxHeight()        // 填满高度
                    .clickable { onDeleteClick(resetMenu) },
                contentAlignment = Alignment.Center // 图标在按钮内居中
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // --- 前景层 (账单卡片) ---
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val target = (offsetX.value + delta).coerceIn(-actionWidthPx, 0f)
                            offsetX.snapTo(target)
                        }
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        if (offsetX.value < -actionWidthPx / 2) {
                            offsetX.animateTo(-actionWidthPx, tween(300))
                        } else {
                            offsetX.animateTo(0f, tween(300))
                        }
                    }
                )
        ) {
            content()
        }
    }
}

// --- 通用组件 ---

@Composable
fun DeleteConfirmationDialog(
    bill: BillEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("确认删除?") },
        text = {
            Text("您确定要删除商户为 ${bill.merchant} 的这笔 ¥${bill.amount} 账单吗？\n此操作无法恢复。")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("确认删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun TotalExpenseCard(amount: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("本月支出", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "¥ ${String.format("%.2f", amount)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun PermissionWarningCard(onRequestPermission: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.clickable { onRequestPermission() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "⚠️ 需要相册权限才能自动记账，点击授权",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun BillItemCard(bill: BillEntity, onClick: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = bill.merchant.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.merchant,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = bill.dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Text(
                text = "-${bill.amount}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EditBillDialog(
    bill: BillEntity,
    onDismiss: () -> Unit,
    onDelete: (BillEntity) -> Unit,
    onUpdate: (BillEntity) -> Unit
) {
    var merchant by remember { mutableStateOf(bill.merchant) }
    var amountStr by remember { mutableStateOf(bill.amount.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("编辑账单", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("商户名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { onDelete(bill) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除")
                    }
                    Row {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val newAmount = amountStr.toDoubleOrNull() ?: bill.amount
                            onUpdate(bill.copy(merchant = merchant, amount = newAmount))
                        }) {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
}