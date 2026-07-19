package com.example.football2.dao
import androidx.room.Dao
import androidx.room.Transaction

@Dao
abstract class GameControlDao {

    @Transaction
    open suspend fun completeLogoAndCheckLevel(
        logoDao: LogoDao,
        levelDao: LevelDao,
        logoId: Int,
        points: Int,
        levelId: Int
    ) {
        logoDao.updateLogoCompletedState(logoId, points)
        val unsolvedCount = logoDao.getUnsolvedLogosCount(levelId)
        if (unsolvedCount == 0) {
            levelDao.setLevelCompleted(levelId)
        }
    }

    @Transaction
    open suspend fun resetGame(
        logoDao: LogoDao,
        levelDao: LevelDao,
        hintDao: HintDao,
        logoHintDao: LogoHintDao
    ) {
        logoDao.resetLogosTable()
        levelDao.resetLevelsTable()
        hintDao.resetHintsTable()
        logoHintDao.clearLogoHintsTable()

        levelDao.getFirstLevelId()?.let { firstId ->
            levelDao.setLevelOpened(firstId)
        }
    }
}