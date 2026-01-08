package com.autobook.lingxi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val amount: Double,       // 金额
    val merchant: String,     // 商户名
    val dateStr: String,      // 原文时间 (如 "1月4日 10:27")
    val timestamp: Long,      // 记录入库的时间戳 (方便排序)
    val category: String = "未分类",
    val type: String = "支出" // 支出/收入
)