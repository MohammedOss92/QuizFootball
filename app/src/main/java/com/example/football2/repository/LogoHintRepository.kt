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
}