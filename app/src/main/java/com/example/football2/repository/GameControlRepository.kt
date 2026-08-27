package com.example.football2.repository
import com.example.football2.dao.GameControlDao
import com.example.football2.dao.HintDao
import com.example.football2.dao.LevelDao
import com.example.football2.dao.LogoDao
import com.example.football2.dao.LogoHintDao

class GameControlRepository(
    private val gameControlDao: GameControlDao,
    private val logoDao: LogoDao,
    private val levelDao: LevelDao,
    private val hintDao: HintDao,
    private val logoHintDao: LogoHintDao
) {

    // دالة لحل الشعار وفحص حالة المستوى فوراً
    suspend fun completeLogoAndCheckLevel(logoId: Int, points: Int, levelId: Int) {
        gameControlDao.completeLogoAndCheckLevel(logoDao, levelDao, logoId, points, levelId)
    }

    // دالة تصفير اللعبة بالكامل مسؤولة عن كافة الجداول
    suspend fun resetGame() {
        gameControlDao.resetGame(logoDao, levelDao, hintDao, logoHintDao)
    }

    // 🟢 ربط استعلامات المستويات مع LevelDao
    suspend fun getTotalLevelsCount(): Int = levelDao.getTotalLevelsCount()
    suspend fun getOpenLevelsCount(): Int = levelDao.getOpenLevelsCount()
    suspend fun getCompletedLevelsCount(): Int = levelDao.getCompletedLevelsCount()
}