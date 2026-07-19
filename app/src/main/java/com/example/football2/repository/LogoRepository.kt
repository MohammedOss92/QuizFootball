package com.example.football2.repository

import com.example.football2.dao.LogoDao
import com.example.football2.entity.LogoEntity

class LogoRepository(private val logoDao: LogoDao) {

    suspend fun getTotalScore(): Int = logoDao.getTotalScore() ?: 0

    suspend fun getTotalLogosCount(): Int = logoDao.getTotalLogosCount()

    suspend fun getCompletedLogosCount(): Int = logoDao.getCompletedLogosCount()

    suspend fun getGoldMedalsCount(): Int = logoDao.getGoldMedalsCount()

    suspend fun getSilverMedalsCount(): Int = logoDao.getSilverMedalsCount()

    suspend fun getBronzeMedalsCount(): Int = logoDao.getBronzeMedalsCount()

    suspend fun getOneLogo(logoId: Int): LogoEntity? = logoDao.getOneLogo(logoId)

    suspend fun getLogosCountByLevel(levelId: Int): Int = logoDao.getLogosCountByLevel(levelId)

    suspend fun getLevelLogos(levelId: Int): List<LogoEntity> = logoDao.getLevelLogos(levelId)

    suspend fun getPrevLogo(logoId: Int, levelId: Int): LogoEntity? = logoDao.getPrevLogo(logoId, levelId)

    suspend fun getNextLogo(logoId: Int, levelId: Int): LogoEntity? = logoDao.getNextLogo(logoId, levelId)

    suspend fun addLetterHintPos(logoId: Int, pos: String) {
        logoDao.addLetterHintPos(logoId, pos)
    }

    suspend fun insertLogo(logo: LogoEntity) {
        logoDao.insertLogo(logo)
    }
}