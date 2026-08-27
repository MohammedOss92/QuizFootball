package com.example.football2

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.football2.db.AppDatabase
import com.example.football2.repository.GameControlRepository
import com.example.football2.repository.HintRepository
import com.example.football2.repository.LogoHintRepository
import com.example.football2.repository.LogoRepository
import com.example.football2.viewModels.LogoViewModel
import com.example.football2.viewModels.ViewModelFactory
import kotlinx.coroutines.launch

class StaticsActivity : AppCompatActivity() {
    private lateinit var logoViewModel: LogoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statics)

        // 1. إعداد الـ Repositories والـ ViewModel
        val database = AppDatabase.getDatabase(this)
        val logoRepo = LogoRepository(database.logoDao())
        val hintRepo = HintRepository(database.hintDao())
        val logoHintRepo = LogoHintRepository(database.logoHintDao())
        val gameControlRepo = GameControlRepository(
            database.gameControlDao(),
            database.logoDao(),
            database.levelDao(),
            database.hintDao(),
            database.logoHintDao()
        )

        val factory = ViewModelFactory(
            logoRepository = logoRepo,
            hintRepository = hintRepo,
            logoHintRepository = logoHintRepo,
            gameControlRepository = gameControlRepo
        )

        logoViewModel = ViewModelProvider(this, factory)[LogoViewModel::class.java]

        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        observeStatistics()

    }
        private fun observeStatistics() {
            lifecycleScope.launch {
                // قسم Score & Hints
                val totalScore = logoViewModel.getTotalScore()
                val currentHints = logoViewModel.getCurrentHints()
                val usedHints = logoViewModel.getUsedHints()
                val totalHints = currentHints + usedHints

                setupRow(R.id.rowTotalScore, totalScore.toString(), "Total Score")
                setupRow(R.id.rowTotalHints, totalHints.toString(), "Total Hints")
                setupRow(R.id.rowUsedHints, usedHints.toString(), "Used Hints")
                setupRow(R.id.rowCurrentHints, currentHints.toString(), "Current Hints")

                // قسم Logos & Medals
                val totalLogos = logoViewModel.getTotalLogosCount()
                val completedLogos = logoViewModel.getCompletedLogosCount()
                val goldMedals = logoViewModel.getGoldMedalsCount()
                val silverMedals = logoViewModel.getSilverMedalsCount()
                val bronzeMedals = logoViewModel.getBronzeMedalsCount()

                setupRow(R.id.rowTotalLogos, totalLogos.toString(), "Total Logos")
                setupRow(R.id.rowCompletedLogos, completedLogos.toString(), "Completed Logos")
                setupRow(R.id.rowGoldMedals, goldMedals.toString(), "Gold Medals")
                setupRow(R.id.rowSilverMedals, silverMedals.toString(), "Silver Medals")
                setupRow(R.id.rowBronzeMedals, bronzeMedals.toString(), "Bronze Medals")

                // قسم Levels
                val totalLevels = logoViewModel.getTotalLevelsCount()
                val openLevels = logoViewModel.getOpenLevelsCount()
                val completedLevels = logoViewModel.getCompletedLevelsCount()

                setupRow(R.id.rowTotalLevels, totalLevels.toString(), "Total Levels")
                setupRow(R.id.rowOpenLevels, openLevels.toString(), "Open Levels")
                setupRow(R.id.rowCompletedLevels, completedLevels.toString(), "Completed Levels")
            }
        }

        private fun setupRow(rowId: Int, value: String, title: String) {
            val rowView = findViewById<android.view.View>(rowId) ?: return
            val tvValue = rowView.findViewById<TextView>(R.id.tvValue)
            val tvTitle = rowView.findViewById<TextView>(R.id.tvTitle)

            tvValue?.text = value
            tvTitle?.text = title
        }
    }