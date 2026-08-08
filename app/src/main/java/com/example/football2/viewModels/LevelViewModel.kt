package com.example.football2.viewModels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.football2.entity.LevelWithStats
import com.example.football2.repository.LevelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LevelViewModel(private val levelRepository: LevelRepository) : ViewModel() {

    private val _levelsWithStats = MutableStateFlow<List<LevelWithStats>>(emptyList())
//    val levelsWithStats: StateFlow<List<LevelWithStats>> = _levelsWithStats.asStateFlow()

    private val _openLevelsCount = MutableStateFlow(0)
    val openLevelsCount: StateFlow<Int> = _openLevelsCount.asStateFlow()

    // جلب المستويات مع الإحصائيات للشاشة الرئيسية اختيار المستويات
    fun fetchLevel2sWithStats() {
        viewModelScope.launch {
            _levelsWithStats.value = levelRepository.getLev2elsWithStats()
        }
    }

    val levelsWithStats: StateFlow<List<LevelWithStats>> = levelRepository.getLevelsWithStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // للحفاظ على التدفق نشطاً أثناء مراقبة الشاشة
            initialValue = emptyList()
        )

    // جلب عدد المستويات المفتوحة
    fun fetchOpenLevelsCount() {
        viewModelScope.launch {
            _openLevelsCount.value = levelRepository.getOpenLevelsCount()
        }
    }

    // فتح مستوى جديد
    fun unlockLevel(levelId: Int) {
        viewModelScope.launch {
            levelRepository.setLevelOpened(levelId)
            fetchLevel2sWithStats() // تحديث القائمة بعد الفتح
        }
    }


    
}