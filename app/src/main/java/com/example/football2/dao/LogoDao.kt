package com.example.football2.dao

import androidx.room.*
import com.example.football2.entity.LogoEntity

@Dao
interface LogoDao {

    @Query("SELECT SUM(lo_points) FROM logos")
    suspend fun getTotalScore(): Int?

    @Query("SELECT COUNT(_loid) FROM logos")
    suspend fun getTotalLogosCount(): Int

    @Query("SELECT COUNT(_loid) FROM logos WHERE lo_completed = 1")
    suspend fun getCompletedLogosCount(): Int

    @Query("SELECT COUNT(_loid) FROM logos WHERE lo_points = 100")
    suspend fun getGoldMedalsCount(): Int

    @Query("SELECT COUNT(_loid) FROM logos WHERE lo_points IN (60, 80)")
    suspend fun getSilverMedalsCount(): Int

    @Query("SELECT COUNT(_loid) FROM logos WHERE lo_points IN (20, 40)")
    suspend fun getBronzeMedalsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogo(logo: LogoEntity)

    @Query("SELECT lo_web_id FROM logos ORDER BY lo_web_id DESC LIMIT 1")
    suspend fun getLastLogoWebId(): Int?

    @Query("SELECT * FROM logos WHERE _loid = :logoId LIMIT 1")
    suspend fun getOneLogo(logoId: Int): LogoEntity?

    @Query("SELECT COUNT(_loid) FROM logos WHERE lo_level = :levelId")
    suspend fun getLogosCountByLevel(levelId: Int): Int

    @Query("SELECT COUNT(_loid) FROM logos WHERE lo_level = :levelId AND lo_completed = 0")
    suspend fun getUnsolvedLogosCount(levelId: Int): Int

    @Query("SELECT * FROM logos WHERE lo_level = :levelId ORDER BY lo_order ASC")
    suspend fun getLevelLogos(levelId: Int): List<LogoEntity>

    @Query("UPDATE logos SET lo_completed = 1, lo_points = :points WHERE _loid = :logoId")
    suspend fun updateLogoCompletedState(logoId: Int, points: Int)

    @Query("SELECT * FROM logos WHERE _loid < :logoId AND lo_level = :levelId ORDER BY _loid DESC LIMIT 1")
    suspend fun getPrevLogo(logoId: Int, levelId: Int): LogoEntity?

    @Query("SELECT * FROM logos WHERE _loid > :logoId AND lo_level = :levelId ORDER BY _loid ASC LIMIT 1")
    suspend fun getNextLogo(logoId: Int, levelId: Int): LogoEntity?

    @Query("UPDATE logos SET lo_letter = :pos WHERE _loid = :logoId")
    suspend fun addLetterHintPos(logoId: Int, pos: String)

    @Query("UPDATE logos SET lo_tries = 0, lo_points = 0, lo_completed = 0, lo_letter = ''")
    suspend fun resetLogosTable()

    @Query("UPDATE logo_hints SET lo_hi_hide = 1 WHERE lo_hi_logo = :logoId")
    suspend fun updateHideHint(logoId: Int)


    @Query("SELECT * FROM logos WHERE _loid = :logoId LIMIT 1")
    suspend fun getLogoById(logoId: Int): LogoEntity?

    // 🟢 استعلام اختياري لجلب lo_info فقط مباشرة
    @Query("SELECT lo_info FROM logos WHERE _loid = :logoId LIMIT 1")
    suspend fun getLogoInfoById(logoId: Int): String?
}