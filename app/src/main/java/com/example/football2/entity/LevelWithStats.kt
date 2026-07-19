package com.example.football2.entity

import androidx.room.Embedded

data class LevelWithStats(
    @Embedded val level: LevelEntity,
    val level_score: Int?,
    val logos_count: Int,
    val completed_logos_count: Int
)