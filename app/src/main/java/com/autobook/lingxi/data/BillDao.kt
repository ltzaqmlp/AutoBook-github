package com.autobook.lingxi.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    // 插入
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bills: List<BillEntity>)

    // 🔥【必须补上这个】更新
    @Update
    suspend fun updateBill(bill: BillEntity)

    // 🔥【必须补上这个】删除
    @Delete
    suspend fun deleteBill(bill: BillEntity)

    // 查询所有
    @Query("SELECT * FROM bills ORDER BY timestamp DESC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query("SELECT COUNT(*) FROM bills")
    suspend fun getCount(): Int
}