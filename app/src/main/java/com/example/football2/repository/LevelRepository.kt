package com.example.football2.repository

import com.example.football2.dao.LevelDao
import com.example.football2.entity.LevelWithStats

class LevelRepository(private val levelDao: LevelDao) {

    suspend fun getLevelsCount(): Int = levelDao.getLevelsCount()

    suspend fun getOpenLevelsCount(): Int = levelDao.getOpenLevelsCount()

    suspend fun getCompletedLevelsCount(): Int = levelDao.getCompletedLevelsCount()

    suspend fun getLastLevelWebId(): Int? = levelDao.getLastLevelWebId()

    suspend fun getLevelsWithStats(): List<LevelWithStats> = levelDao.getLevelsWithStats()

    suspend fun setLevelOpened(levelId: Int) {
        levelDao.setLevelOpened(levelId)
    }

    suspend fun addNewLevel(country: String, flag: String, webId: Int) {
        levelDao.addNewLevel(country, flag, webId)
    }
}