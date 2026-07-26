package com.example.football2.repository

import com.example.football2.dao.LogoHintDao
import com.example.football2.entity.LogoHintEntity

class LogoHintRepository(private val logoHintDao: LogoHintDao) {

    suspend fun getHintState(logoId: Int): LogoHintEntity? = logoHintDao.getHintState(logoId)

    suspend fun insertLogoHint(logoHint: LogoHintEntity) {
        logoHintDao.insertLogoHint(logoHint)
    }

    suspend fun updateFacebookHint(logoHintId: Int) {
        logoHintDao.updateFacebookHint(logoHintId)
    }

    suspend fun updateHideHint(logoId: Int) {
        logoHintDao.updateHideHint(logoId)
    }

    suspend fun getHintStateForLogo(logoId: Int): LogoHintEntity? {
        return logoHintDao.getHintStateForLogo(logoId)
    }

    suspend fun unlockHideHint(logoId: Int) {
        val currentState = logoHintDao.getHintStateForLogo(logoId)

        if (currentState != null) {
            // تحديث السجل الموجود مسبقاً لهذا الشعار
            val updatedState = currentState.copy(hide = 1)
            logoHintDao.insertOrReplaceLogoHint(updatedState)
        } else {
            // إنشاء سجل جديد تماماً وتحديد lo_hi_logo بدقة
            val newState = LogoHintEntity(
                id = null,              // أتركه null لتنشئ Room معرّف فريد تلقائياً (إذا كان PrimaryKey autoGenerate)
                logoId = logoId,        // 🟢 إسناد رقم الشعار للعمود الصحيح
                facebook = 0,
                twitter = 0,
                info = 0,
                hide = 1,               // تفعيل القنبلة
                letter = 0,
                player = 0
            )
            logoHintDao.insertOrReplaceLogoHint(newState)
        }
    }
}