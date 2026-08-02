package com.example.football2.dao

import androidx.room.*
import com.example.football2.entity.LogoHintEntity

@Dao
interface LogoHintDao {

    @Query("SELECT * FROM logo_hints WHERE lo_hi_logo = :logoId LIMIT 1")
    suspend fun getHintState(logoId: Int): LogoHintEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogoHint(logoHint: LogoHintEntity)

    @Query("UPDATE logo_hints SET lo_hi_facebook = 1 WHERE lo_hi_logo = :logoHintId")
    suspend fun updateFacebookHint(logoHintId: Int)

    @Query("DELETE FROM logo_hints")
    suspend fun clearLogoHintsTable()

    @Query("UPDATE logo_hints SET lo_hi_hide = 1 WHERE lo_hi_logo = :logoId")
    suspend fun updateHideHint(logoId: Int)

    // 🟢 التصحيح: البحث حسب lo_hi_logo وليس _lo_hi_id
    @Query("SELECT * FROM logo_hints WHERE lo_hi_logo = :logoId LIMIT 1")
    suspend fun getHintStateForLogo(logoId: Int): LogoHintEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceLogoHint(logoHint: LogoHintEntity)


    @Query("UPDATE logo_hints SET lo_hi_info = :status WHERE lo_hi_logo = :logoId")
    suspend fun updateInfoHint(logoId: Int, status: Int)

    @Query("SELECT * FROM logo_hints WHERE lo_hi_logo = :logoId LIMIT 1")
    suspend fun getHintForLogo(logoId: Int): LogoHintEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHint(hint: LogoHintEntity)
}