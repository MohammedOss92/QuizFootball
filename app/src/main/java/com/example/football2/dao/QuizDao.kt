package com.sarrawi.footballlogoquiz.data.dao

import androidx.room.*
import com.example.football2.entity.LevelEntity
import com.example.football2.entity.LevelWithStats
import com.example.football2.entity.LogoEntity
import com.example.football2.entity.LogoHintEntity

@Dao
interface QuizDao {

    // ===================== استعلامات الإحصائيات والقيم المباشرة =====================

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

    @Query("SELECT COUNT(_leid) FROM levels")
    suspend fun getLevelsCount(): Int

    @Query("SELECT COUNT(_leid) FROM levels WHERE le_open = 1")
    suspend fun getOpenLevelsCount(): Int

    @Query("SELECT COUNT(_leid) FROM levels WHERE le_completed = 1")
    suspend fun getCompletedLevelsCount(): Int

    @Query("SELECT total_hints FROM hints WHERE _hiid = 1")
    suspend fun getTotalHints(): Int?

    @Query("SELECT used_hints FROM hints WHERE _hiid = 1")
    suspend fun getUsedHints(): Int?


    // ===================== الإضافة والجلب والتعديل الأساسي =====================

    @Query("SELECT MAX(le_order) FROM levels")
    suspend fun getMaxLevelOrder(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevel(level: LevelEntity)

    // دالة محاكاة addLevel القديمة


    @Transaction
    suspend fun addNewLevel(country: String, flag: String, webId: Int) {
        // 1. جلب أعلى ترتيب موجود في المستويات، وإذا كان الجدول فارغاً نعتبره 0
        val maxOrder = getMaxLevelOrder() ?: 0

        // 2. إنشاء كائن المستوى الجديد مع مطابقة أسماء الحقول بدقة بعد التعديل
        val newLevel = LevelEntity(
            leid = webId,                 // تم التعديل من _leid إلى leid
            leCountry = country,          // تم التعديل من le_country إلى leCountry
            leFlag = flag,                // تم التعديل من le_flag إلى leFlag
            leOrder = maxOrder + 1,       // تم التعديل من le_order إلى leOrder
            leWebId = webId,              // تم التعديل من le_web_id إلى leWebId

            // 3. تعبئة باقي الحقول بالقيم الافتراضية باستخدام الأسماء الجديدة
            leOpen = 1,                   // تم التعديل من le_open إلى leOpen
            leCompleted = 0,              // تم التعديل من le_completed إلى leCompleted
            leFlagSdcard = 0,             // تم التعديل من le_flag_sdcard إلى leFlagSdcard
            leStatus = 0                  // تم التعديل من le_status إلى leStatus
        )

        // 4. حفظ المستوى الجديد في قاعدة البيانات
        insertLevel(newLevel)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogo(logo: LogoEntity)

    @Query("SELECT le_web_id FROM levels ORDER BY le_web_id DESC LIMIT 1")
    suspend fun getLastLevelWebId(): Int?

    @Query("SELECT lo_web_id FROM logos ORDER BY lo_web_id DESC LIMIT 1")
    suspend fun getLastLogoWebId(): Int?

    @Query("SELECT * FROM logos WHERE _loid = :logoId LIMIT 1")
    suspend fun getOneLogo(logoId: Int): LogoEntity?

    @Query("SELECT COUNT(_loid) FROM logos WHERE lo_level = :levelId")
    suspend fun getLogosCountByLevel(levelId: Int): Int


    // ===================== إدارة حالات الاكتمال والمساعدات =====================

    @Query("UPDATE logos SET lo_completed = 1, lo_points = :points WHERE _loid = :logoId")
    suspend fun updateLogoCompletedState(logoId: Int, points: Int)

    @Query("SELECT COUNT(_loid) FROM logos WHERE lo_level = :levelId AND lo_completed = 0")
    suspend fun getUnsolvedLogosCount(levelId: Int): Int

    @Query("UPDATE levels SET le_completed = 1 WHERE le_web_id = :levelId")
    suspend fun setLevelCompleted(levelId: Int)

    // دالة محاكاة setLogoCompleted المركبة القديمة
    @Transaction
    suspend fun completeLogoAndCheckLevel(logoId: Int, points: Int, levelId: Int) {
        updateLogoCompletedState(logoId, points)
        val unsolvedCount = getUnsolvedLogosCount(levelId)
        if (unsolvedCount == 0) {
            setLevelCompleted(levelId)
        }
    }

    @Query("UPDATE hints SET total_hints = total_hints + :hints WHERE _hiid = 1")
    suspend fun addTotalHints(hints: Int)

    @Query("UPDATE hints SET used_hints = used_hints + 1 WHERE _hiid = 1")
    suspend fun addUsedHint()

    @Query("UPDATE levels SET le_open = 1 WHERE le_web_id = :levelId")
    suspend fun setLevelOpened(levelId: Int)


    // ===================== الانتقال بين الشعارات (السابق والتالي) =====================

    @Query("SELECT * FROM logos WHERE _loid < :logoId AND lo_level = :levelId ORDER BY _loid DESC LIMIT 1")
    suspend fun getPrevLogo(logoId: Int, levelId: Int): LogoEntity?

    @Query("SELECT * FROM logos WHERE _loid > :logoId AND lo_level = :levelId ORDER BY _loid ASC LIMIT 1")
    suspend fun getNextLogo(logoId: Int, levelId: Int): LogoEntity?


    // ===================== تلميحات الشعار المحدد =====================

    @Query("SELECT * FROM logo_hints WHERE lo_hi_logo = :logoId LIMIT 1")
    suspend fun getHintState(logoId: Int): LogoHintEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogoHint(logoHint: LogoHintEntity)

    @Query("UPDATE logo_hints SET lo_hi_facebook = 1 WHERE lo_hi_logo = :logoHintId")
    suspend fun updateFacebookHint(logoHintId: Int)

    // يمكنك عمل دوال مشابهة لباقي الحقول (lo_hi_twitter, lo_hi_player...) حسب حقل الـ String الممرر سابقاً

    @Query("UPDATE logos SET lo_letter = :pos WHERE _loid = :logoId")
    suspend fun addLetterHintPos(logoId: Int, pos: String)


    // ===================== استعلامات الشاشات المعقدة (المستويات والشعارات) =====================

    // جلب المستويات مع حساب النقاط الكلية وعدد الشعارات المكتملة مدمجة تلقائياً
    @Query("""
        SELECT levels.*, 
        (SELECT SUM(lo_points) FROM logos WHERE lo_level = levels.le_web_id AND lo_completed = 1) AS level_score,
        (SELECT COUNT(_loid) FROM logos WHERE lo_level = levels.le_web_id) AS logos_count,
        (SELECT COUNT(_loid) FROM logos WHERE lo_level = levels.le_web_id AND lo_completed = 1) AS completed_logos_count
        FROM levels ORDER BY le_order ASC
    """)
    suspend fun getLevelsWithStats(): List<LevelWithStats>

    @Query("SELECT * FROM logos WHERE lo_level = :levelId ORDER BY lo_order ASC")
    suspend fun getLevelLogos(levelId: Int): List<LogoEntity>


    // ===================== إعادة ضبط اللعبة (Reset Game) =====================

    @Query("UPDATE logos SET lo_tries = 0, lo_points = 0, lo_completed = 0, lo_letter = ''")
    suspend fun resetLogosTable()

    @Query("UPDATE levels SET le_open = 0, le_completed = 0")
    suspend fun resetLevelsTable()

    @Query("UPDATE hints SET total_hints = 8, used_hints = 0 WHERE _hiid = 1")
    suspend fun resetHintsTable()

    @Query("DELETE FROM logo_hints")
    suspend fun clearLogoHintsTable()

    @Query("SELECT _leid FROM levels ORDER BY le_order ASC LIMIT 1")
    suspend fun getFirstLevelId(): Int?

    @Transaction
    suspend fun resetGame() {
        resetLogosTable()
        resetLevelsTable()
        resetHintsTable()
        clearLogoHintsTable()

        getFirstLevelId()?.let { firstId ->
            val updateFirstLevelQuery = "UPDATE levels SET le_open = 1 WHERE _leid = $firstId"
            // تفتح المستوى الأول
            setLevelOpened(firstId)
        }
    }
}

// كلاس مساعد (POJO) لاستقبال بيانات الشاشة الرئيسية للمستويات مع إحصاءاتها بدون الحاجة لكورسر يدوي
