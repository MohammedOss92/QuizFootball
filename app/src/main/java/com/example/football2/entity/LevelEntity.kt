package com.example.football2.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "levels")
data class LevelEntity(
    @PrimaryKey
    @ColumnInfo(name = "_leid")
    val leid: Int?,

    @ColumnInfo(name = "le_completed")
    val leCompleted: Int?,

    @ColumnInfo(name = "le_country")
    val leCountry: String?,

    @ColumnInfo(name = "le_flag")
    val leFlag: String?,

    @ColumnInfo(name = "le_flag_sdcard")
    val leFlagSdcard: Int?,

    // حل مشكلة الـ INT والـ Null للهيكل القديم
    @ColumnInfo(name = "le_open", typeAffinity = ColumnInfo.INTEGER)
    val leOpen: Int?,

    @ColumnInfo(name = "le_order", typeAffinity = ColumnInfo.INTEGER)
    val leOrder: Int?,

    @ColumnInfo(name = "le_status")
    val leStatus: Int?,

    @ColumnInfo(name = "le_web_id")
    val leWebId: Int?
)