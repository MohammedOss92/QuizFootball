package com.example.football2.viewModels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.football2.entity.LogoEntity
import com.example.football2.repository.GameControlRepository
import com.example.football2.repository.HintRepository
import com.example.football2.repository.LogoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LogoViewModel(private val logoRepository: LogoRepository, private val hintRepository: HintRepository, private val gameControlRepository: GameControlRepository) : ViewModel() {

    private val _logos = MutableStateFlow<List<LogoEntity>>(emptyList())
    val logos: StateFlow<List<LogoEntity>> = _logos.asStateFlow()

    private val _totalScore = MutableStateFlow(0)
    val totalScore: StateFlow<Int> = _totalScore.asStateFlow()

    // مؤشرات الميداليات لشاشة الإحصائيات
    private val _goldMedals = MutableStateFlow(0)
    val goldMedals: StateFlow<Int> = _goldMedals.asStateFlow()

    private val _silverMedals = MutableStateFlow(0)
    val silverMedals: StateFlow<Int> = _silverMedals.asStateFlow()

    private val _bronzeMedals = MutableStateFlow(0)
    val bronzeMedals: StateFlow<Int> = _bronzeMedals.asStateFlow()


    private val _questionText = MutableStateFlow<String>("")
    val questionText: StateFlow<String> = _questionText.asStateFlow()

    // تحميل نص السؤال مباشرة باستخدام الـ Repository
    fun loadQuestionForLogo(logoId: Int) {
        viewModelScope.launch {
            val infoText = logoRepository.getQuestionText(logoId)
            _questionText.value = infoText.takeIf { !it.isNullOrBlank() } ?: "ما هو اسم هذا النادي؟"
        }
    }

    // جلب شعارات مستوى معين
    fun loadLogosForLevel(levelId: Int) {
        viewModelScope.launch {
            _logos.value = logoRepository.getLevelLogos(levelId)
        }
    }

    // جلب النقاط والميداليات الإجمالية لشاشة الإحصائيات
    fun loadGameStatistics() {
        viewModelScope.launch {
            _totalScore.value = logoRepository.getTotalScore()
            _goldMedals.value = logoRepository.getGoldMedalsCount()
            _silverMedals.value = logoRepository.getSilverMedalsCount()
            _bronzeMedals.value = logoRepository.getBronzeMedalsCount()
        }
    }

    // حفظ موضع الحرف المساعد للشعار الحالي
    fun saveLetterHintPosition(logoId: Int, position: String) {
        viewModelScope.launch {
            logoRepository.addLetterHintPos(logoId, position)
        }
    }


    suspend fun getTotalScore(): Int = logoRepository.getTotalScore()
    suspend fun getCurrentHints(): Int = hintRepository.getCurrentHintsCount()
    suspend fun getUsedHints(): Int = hintRepository.getUsedHintsCount()

    // 2. Logos & Medals
    suspend fun getTotalLogosCount(): Int = logoRepository.getTotalLogosCount()
    suspend fun getCompletedLogosCount(): Int = logoRepository.getCompletedLogosCount()
    suspend fun getGoldMedalsCount(): Int = logoRepository.getGoldMedalsCount()
    suspend fun getSilverMedalsCount(): Int = logoRepository.getSilverMedalsCount()
    suspend fun getBronzeMedalsCount(): Int = logoRepository.getBronzeMedalsCount()

    // 3. Levels
    suspend fun getTotalLevelsCount(): Int = gameControlRepository.getTotalLevelsCount()
    suspend fun getOpenLevelsCount(): Int = gameControlRepository.getOpenLevelsCount()
    suspend fun getCompletedLevelsCount(): Int = gameControlRepository.getCompletedLevelsCount()
}