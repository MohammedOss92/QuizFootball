package com.example.football2.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logo_hints")
data class LogoHintEntity(

    @PrimaryKey
    @ColumnInfo(name = "_lo_hi_id")
    val id: Int?,

    @ColumnInfo(name = "lo_hi_logo")
    val logoId: Int?,

    @ColumnInfo(name = "lo_hi_facebook")
    val facebook: Int?,

    @ColumnInfo(name = "lo_hi_twitter")
    val twitter: Int?,

    @ColumnInfo(name = "lo_hi_info")
    val info: Int?,

    @ColumnInfo(name = "lo_hi_hide")
    val hide: Int?,

    @ColumnInfo(name = "lo_hi_letter")
    val letter: Int?,

    @ColumnInfo(name = "lo_hi_player")
    val player: Int?
)