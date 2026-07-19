package com.example.football2.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hints")
data class HintEntity(

    @PrimaryKey
    val _hiid: Int?,

    val total_hints: Int?,

    val used_hints: Int?
)