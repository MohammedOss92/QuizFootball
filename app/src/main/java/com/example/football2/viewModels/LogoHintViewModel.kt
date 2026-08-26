package com.example.football2.viewModels


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.football2.entity.LogoHintEntity
import com.example.football2.repository.GameControlRepository
import com.example.football2.repository.LogoHintRepository
import kotlinx.coroutines.Dispatchers
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
    fun loa1dHintStateForLogo(logoId: Int) {
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

    fun u1nlockHideHint(logoId: Int) {
        viewModelScope.launch {
            logoHintRepository.updateHideHint(logoId)
            loadHintStateForLogo(logoId) // إعادة تحميل الحالة لتحديث الـ Flow
        }
    }


    fun loadHintStateForLogo(logoId: Int?) {
        viewModelScope.launch(Dispatchers.IO) {
            // 🟢 إرجاع قيمة null مؤقتاً لتصفير الحالة للشعار الجديد
            _currentLogoHintState.value = null

            val state = logoHintRepository.getHintStateForLogo(logoId)
            _currentLogoHintState.value = state
        }
    }


    private val _currentLogoId = MutableLiveData<Int>()

    // SwitchMap يضمن تحديث الـ LiveData تلقائياً فور تغيير الـ logoId
    val logoHintState: LiveData<LogoHintEntity?> = _currentLogoId.switchMap { id ->
        logoHintRepository.getLogoHintStateLiveData(id)
    }

    fun setLogoId(logoId: Int) {
        _currentLogoId.value = logoId
    }

    fun unlockPlay2erHint(logoId: Int) {
        viewModelScope.launch {
            // 1. جلب السجل الحالي للشعار من DB
            val existingEntity = logoHintRepository.getHintForLogo(logoId)

            val updatedEntity = if (existingEntity != null) {
                existingEntity.copy(player = 1)
            } else {
                LogoHintEntity(
                    id = null,
                    logoId = logoId,
                    facebook = 0,
                    twitter = 0,
                    info = 0,
                    hide = 0,
                    letter = 0,
                    player = 1
                )
            }

            // 2. تحديث الـ State المباشر فوراً
            _currentLogoHintState.value = updatedEntity

            // 3. حفظ السجل في قاعدة البيانات
            logoHintRepository.insertOrUpdateHint(updatedEntity)
        }
    }

    fun unlockPlayerHint(logoId: Int) {
        viewModelScope.launch {
            // 1. جلب السجل الحالي للشعار من DB
            val existingEntity = logoHintRepository.getHintForLogo(logoId)

            // 2. استخدام ?: بدلاً من if-else
            val updatedEntity = existingEntity?.copy(player = 1) ?: LogoHintEntity(
                id = null,
                logoId = logoId,
                facebook = 0,
                twitter = 0,
                info = 0,
                hide = 0,
                letter = 0,
                player = 1
            )

            // 3. تحديث الـ State المباشر فوراً
            _currentLogoHintState.value = updatedEntity

            // 4. حفظ السجل في قاعدة البيانات
            logoHintRepository.insertOrUpdateHint(updatedEntity)
        }
    }

    fun unlockHideHint(logoId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. الحفظ في الداتا بيز (سواء إضافة أو تعديل)
            logoHintRepository.unlockHideHint(logoId)

            // 2. إعادة قراءة الحالة مباشرة من DB وتمريرها للـ UI
            val updatedState = logoHintRepository.getHintStateForLogo(logoId)
            _currentLogoHintState.value = updatedState
        }
    }

    fun unlockLetterHint(logoId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            logoHintRepository.revealOneLetter(logoId)
            val updatedState = logoHintRepository.getHintStateForLogo(logoId)
            _currentLogoHintState.value = updatedState
        }
    }



    fun unlockLetterHintAt(logoId: Int, slotIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. الحفظ في Room DB
            logoHintRepository.revealLetterAtPosition(logoId, slotIndex)

            // 2. إعادة قراءة الحالة فوراً لتحديث الـ StateFlow / LiveData
            val updatedState = logoHintRepository.getHintStateForLogo(logoId)
            _currentLogoHintState.value = updatedState
        }
    }



    fun unlockInfoHint(logoId: Int) {
        viewModelScope.launch {
            // 1. جلب السجل الحالي للشعار من DB
            val existingEntity = logoHintRepository.getHintForLogo(logoId)

            val updatedEntity = if (existingEntity != null) {
                existingEntity.copy(info = 1)
            } else {
                LogoHintEntity(
                    id = null,
                    logoId = logoId,
                    facebook = 0,
                    twitter = 0,
                    info = 1,
                    hide = 0,
                    letter = 0,
                    player = 0
                )
            }

            // 2. تحديث الـ State المباشر فوراً ليعرف التطبيق في المرة الثانية أن التلميح فُتح
            _currentLogoHintState.value = updatedEntity

            // 3. حفظ السجل في قاعدة البيانات
            logoHintRepository.insertOrUpdateHint(updatedEntity)
        }
    }
}