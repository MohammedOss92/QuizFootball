package com.example.football2.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logos")
data class LogoEntity(

    @PrimaryKey
    val _loid: Int?,

    val lo_name: String?,
    val lo_image: String?,
    val lo_level: Int?,
    val lo_wikipedia: String?,
    val lo_info: String?,
    val lo_player: String?,
    val lo_letter: String?,
    val lo_tries: Int?,
    var lo_points: Int?,
    var lo_completed: String?,
    val lo_image_sdcard: Int?,
    val lo_order: Int?,
    val lo_status: Int?,
    val lo_web_id: Int?
)