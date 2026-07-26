package com.sarrawi.footballlogoquiz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.football2.entity.LogoHintEntity
import com.example.football2.repository.GameControlRepository
import com.example.football2.repository.LogoHintRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LogoHintViewModel(
    private val logoHintRepository: LogoHintRepository,
    private val gameControlRepository: GameControlRepository
) : ViewModel() {

    private val _currentLogoHintState = MutableStateFlow<LogoHintEntity?>(null)
    val currentLogoHintState: StateFlow<LogoHintEntity?> = _currentLogoHintState.asStateFlow()

    // جلب حالة التلميحات المفتوحة لشعار معين
    fun loadHintStateForLogo(logoId: Int) {
        viewModelScope.launch {
            _currentLogoHintState.value = logoHintRepository.getHintState(logoId)
        }
    }

    // تحديث وتفعيل تلميح الفيسبوك للشعار الحالي
    fun unlockFacebookHint(logoHintId: Int) {
        viewModelScope.launch {
            logoHintRepository.updateFacebookHint(logoHintId)
            loadHintStateForLogo(logoHintId) // تحديث الواجهة بالحالة الجديدة
        }
    }

    // --- العمليات المشتركة والتحكم باللعبة (GameControl) ---

    // عند حل الشعار بشكل صحيح: يتم تحديثه، وفحص إذا كان المستوى اكتمل ليتم قفله أو الانتقال
    fun submitCorrectAnswer(logoId: Int, points: Int, levelId: Int) {
        viewModelScope.launch {
            gameControlRepository.completeLogoAndCheckLevel(logoId, points, levelId)
        }
    }

    // تصفير اللعبة بالكامل مسؤولة عن تصفير كافة الجداول معاً
    fun performResetGame() {
        viewModelScope.launch {
            gameControlRepository.resetGame()
        }
    }

    fun unlockHideHint(logoId: Int) {
        viewModelScope.launch {
            logoHintRepository.updateHideHint(logoId)
            loadHintStateForLogo(logoId) // إعادة تحميل الحالة لتحديث الـ Flow
        }
    }
}