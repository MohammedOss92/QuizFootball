package com.sarrawi.footballlogoquiz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.football2.repository.HintRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HintViewModel(private val hintRepository: HintRepository) : ViewModel() {

    private val _currentHints = MutableStateFlow(8) // القيمة الافتراضية 8 مساعدات عند البداية
    val currentHints: StateFlow<Int> = _currentHints.asStateFlow()

    // جلب رصيد المساعدات الفعلي المتوفر للاعب حالياً
    fun loadCurrentHints() {
        viewModelScope.launch {
            _currentHints.value = hintRepository.getCurrentHints()
        }
    }

    // استهلاك وسيلة مساعدة (عند الضغط على زر تلميح)
    fun useHint() {
        viewModelScope.launch {
            hintRepository.addUsedHint()
            loadCurrentHints() // إعادة تحميل الرصيد المتبقي
        }
    }

    // إضافة رصيد مساعدات إضافي (مكافأة أو شراء)
    fun rewardHints(amount: Int) {
        viewModelScope.launch {
            hintRepository.addTotalHints(amount)
            loadCurrentHints()
        }
    }
}