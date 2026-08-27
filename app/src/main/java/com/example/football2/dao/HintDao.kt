package com.example.football2.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface HintDao {

    @Query("SELECT total_hints FROM hints WHERE _hiid = 1")
    suspend fun getTotalHints(): Int?

    @Query("SELECT used_hints FROM hints WHERE _hiid = 1")
    suspend fun getUsedHints(): Int?

    @Query("UPDATE hints SET total_hints = total_hints + :hints WHERE _hiid = 1")
    suspend fun addTotalHints(hints: Int)

    @Query("UPDATE hints SET used_hints = used_hints + 1 WHERE _hiid = 1")
    suspend fun addUsedHint()

    @Query("UPDATE hints SET total_hints = 8, used_hints = 0 WHERE _hiid = 1")
    suspend fun resetHintsTable()

    @Query("SELECT total_hints FROM hints LIMIT 1")
    suspend fun getCurrentHintsCount(): Int

    @Query("SELECT used_hints FROM hints LIMIT 1")
    suspend fun getUsedHintsCount(): Int
}