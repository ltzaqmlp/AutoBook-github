package com.autobook.lingxi.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autobook.lingxi.data.AppDatabase
import com.autobook.lingxi.data.BillEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class BillViewModel(application: Application) : AndroidViewModel(application) {
    private val billDao = AppDatabase.getDatabase(application).billDao()

    val allBills: StateFlow<List<BillEntity>> = billDao.getAllBills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMonthExpense: StateFlow<Double> = allBills.map { bills ->
        calculateMonthlyExpense(bills)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val last7DaysTrend: StateFlow<List<Pair<String, Double>>> = allBills.map { bills ->
        calculateLast7Days(bills)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateBill(bill: BillEntity) = viewModelScope.launch(Dispatchers.IO) { billDao.updateBill(bill) }
    fun deleteBill(bill: BillEntity) = viewModelScope.launch(Dispatchers.IO) { billDao.deleteBill(bill) }

    private fun calculateMonthlyExpense(bills: List<BillEntity>): Double {
        return bills.filter { it.type == "支出" }.sumOf { it.amount }
    }

    private fun calculateLast7Days(bills: List<BillEntity>): List<Pair<String, Double>> {
        val result = mutableListOf<Pair<String, Double>>()
        val calendar = Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
        for (i in 6 downTo 0) {
            val targetCal = Calendar.getInstance()
            targetCal.add(Calendar.DAY_OF_YEAR, -i)
            targetCal.set(Calendar.HOUR_OF_DAY, 0); targetCal.set(Calendar.MINUTE, 0); targetCal.set(Calendar.SECOND, 0)
            val start = targetCal.timeInMillis
            targetCal.set(Calendar.HOUR_OF_DAY, 23); targetCal.set(Calendar.MINUTE, 59); targetCal.set(Calendar.SECOND, 59)
            val end = targetCal.timeInMillis
            val dateLabel = dateFormat.format(targetCal.time)
            val sum = bills.filter { it.timestamp in start..end && it.type == "支出" }.sumOf { it.amount }
            result.add(dateLabel to sum)
        }
        return result
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BillViewModel::class.java)) return BillViewModel(application) as T
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}