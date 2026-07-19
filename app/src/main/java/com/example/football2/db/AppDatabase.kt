package com.sarrawi.footballlogoquiz.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.football2.dao.GameControlDao
import com.example.football2.dao.HintDao
import com.example.football2.dao.LevelDao
import com.example.football2.dao.LogoDao
import com.example.football2.dao.LogoHintDao
import com.example.football2.entity.HintEntity
import com.example.football2.entity.LevelEntity
import com.example.football2.entity.LogoEntity
import com.example.football2.entity.LogoHintEntity
import com.sarrawi.footballlogoquiz.data.dao.QuizDao


@Database(
    entities = [LevelEntity::class, LogoEntity::class, HintEntity::class, LogoHintEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun quizDao(): QuizDao
    abstract fun levelDao(): LevelDao
    abstract fun logoDao(): LogoDao
    abstract fun hintDao(): HintDao
    abstract fun logoHintDao(): LogoHintDao
    abstract fun gameControlDao(): GameControlDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "FootballLogoQuiz.db" // الاسم الجديد لقاعدة البيانات داخل مجلد التطبيق الخاص
                )
                    .createFromAsset("database/FootballLogoQuiz") // يجب أن يتطابق الاسم تماماً مع الملف داخل مجلد assets
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}