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

    // 🟢 فتح المستوى يدويًا بشرط خصم/فحص النقاط
    suspend fun unlockLevelManually(levelId: Int?, cost: Int = 500): Boolean {
        val totalScore = logoDao.getTotalScore() ?: 0
        if (totalScore >= cost) {
            // 1. تغيير حالة المستوى في جدول المستويات إلى مفتوح (1)
            levelDao.unlockLevel(levelId)

            // 2. تحديث النقاط (خصم تكلفة الفتح)
            // ملاحظة: يمكنك خصم النقاط عن طريق تعديل نقاط أول شعار أو إضافة خصم مجمل
            return true
        }
        return false
    }


    suspend fun unlockLevelManually(levelId: Int, cost: Int = 500): Boolean {
        val currentScore = logoDao.getTotalScore() ?: 0

        if (currentScore >= cost) {
            var remainingToDeduct = cost
            val completedLogos = logoDao.getCompletedLogosWithPoints()

            for (logo in completedLogos) {
                if (remainingToDeduct <= 0) break

                val logoPoints = logo.lo_points ?: 0
                if (logoPoints <= remainingToDeduct) {
                    remainingToDeduct -= logoPoints
                    logo.lo_points = 0
                } else {
                    logo.lo_points = logoPoints - remainingToDeduct
                    remainingToDeduct = 0
                }
                logoDao.updateLogo(logo)
            }

            // 🟢 فتح المستوى بعد الخصم
            levelDao.unlockLevel(levelId)
            return true
        }
        return false
    }

}