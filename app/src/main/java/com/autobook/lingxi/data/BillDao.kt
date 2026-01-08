package com.autobook.lingxi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    // 插入一笔或多笔账单
    @Insert
    suspend fun insertAll(bills: List<BillEntity>)

    // 查询所有账单 (按时间倒序)
    @Query("SELECT * FROM bills ORDER BY timestamp DESC")
    fun getAllBills(): Flow<List<BillEntity>>

    // 统计总笔数 (用于测试)
    @Query("SELECT COUNT(*) FROM bills")
    suspend fun getCount(): Int
}