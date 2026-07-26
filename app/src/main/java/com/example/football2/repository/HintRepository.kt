package com.example.football2.repository
import com.example.football2.dao.HintDao


class HintRepository(private val hintDao: HintDao) {

    suspend fun getTotalHints(): Int = hintDao.getTotalHints() ?: 8

    suspend fun getUsedHints(): Int = hintDao.getUsedHints() ?: 0

    suspend fun getCurrentHints(): Int {
        val total = hintDao.getTotalHints() ?: 8
        val used = hintDao.getUsedHints() ?: 0
        return total - used
    }

    suspend fun addTotalHints(hints: Int) {
        hintDao.addTotalHints(hints)
    }

    suspend fun addUsedHint() {
        hintDao.addUsedHint()
    }


}