package com.example.football2.dao

import androidx.room.*
import com.example.football2.entity.LevelEntity
import com.example.football2.entity.LevelWithStats
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelDao {

    @Query("SELECT COUNT(_leid) FROM levels")
    suspend fun getLevelsCount(): Int



    @Query("UPDATE levels SET le_open = 1 WHERE _leid = :levelId")
    suspend fun unlockLevel(levelId: Int?)

    @Query("SELECT COUNT(_leid) FROM levels WHERE le_open = 1")
    suspend fun getOpenLevelsCount(): Int

    @Query("SELECT COUNT(_leid) FROM levels WHERE le_completed = 1")
    suspend fun getCompletedLevelsCount(): Int

    @Query("SELECT MAX(le_order) FROM levels")
    suspend fun getMaxLevelOrder(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevel(level: LevelEntity)

    @Query("SELECT le_web_id FROM levels ORDER BY le_web_id DESC LIMIT 1")
    suspend fun getLastLevelWebId(): Int?

    @Query("UPDATE levels SET le_completed = 1 WHERE le_web_id = :levelId")
    suspend fun setLevelCompleted(levelId: Int)

    @Query("UPDATE levels SET le_open = 1 WHERE le_web_id = :levelId")
    suspend fun setLevelOpened(levelId: Int)

    @Query("SELECT _leid FROM levels ORDER BY le_order ASC LIMIT 1")
    suspend fun getFirstLevelId(): Int?

    @Query("UPDATE levels SET le_open = 0, le_completed = 0")
    suspend fun resetLevelsTable()

    // استعلام شاشة المستويات الرئيسية مع الإحصائيات مدمجة
    @Query("""
        SELECT levels.*, 
        (SELECT SUM(lo_points) FROM logos WHERE lo_level = levels.le_web_id AND lo_completed = 1) AS level_score,
        (SELECT COUNT(_loid) FROM logos WHERE lo_level = levels.le_web_id) AS logos_count,
        (SELECT COUNT(_loid) FROM logos WHERE lo_level = levels.le_web_id AND lo_completed = 1) AS completed_logos_count
        FROM levels ORDER BY le_order ASC
    """)
    suspend fun getLevelsW2ithStats(): List<LevelWithStats>


    @Query("""
        SELECT levels.*, 
        (SELECT SUM(lo_points) FROM logos WHERE lo_level = levels.le_web_id AND lo_completed = 1) AS level_score,
        (SELECT COUNT(_loid) FROM logos WHERE lo_level = levels.le_web_id) AS logos_count,
        (SELECT COUNT(_loid) FROM logos WHERE lo_level = levels.le_web_id AND lo_completed = 1) AS completed_logos_count
        FROM levels ORDER BY le_order ASC
    """)
    fun getLevelsWithStats(): Flow<List<LevelWithStats>>

    @Transaction
    suspend fun addNewLevel(country: String, flag: String, webId: Int) {
        // جلب أعلى ترتيب موجود، وإذا لم يوجد (الجدول فارغ) نبدأ من 0
        val maxOrder = getMaxLevelOrder() ?: 0

        val newLevel = LevelEntity(
            leid = webId,                // استخدام اسم المتغير الجديد leid بدلاً من _leid
            leCountry = country,         // استخدام leCountry بدلاً من le_country
            leFlag = flag,               // استخدام leFlag بدلاً من le_flag
            leOpen = 1,                  // حقل إجباري (Int)
            leCompleted = 0,             // استخدام leCompleted بدلاً من le_completed
            leFlagSdcard = 0,            // استخدام leFlagSdcard بدلاً من le_flag_sdcard
            leOrder = maxOrder + 1,      // استخدام leOrder بدلاً من le_order
            leStatus = 0,                // استخدام leStatus بدلاً من le_status
            leWebId = webId              // استخدام leWebId بدلاً من le_web_id
        )

        insertLevel(newLevel)
    }

    @Query("SELECT COUNT(_leid) FROM levels")
    suspend fun getTotalLevelsCount(): Int


}