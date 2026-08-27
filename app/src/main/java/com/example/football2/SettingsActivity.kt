package com.example.football2

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.football2.db.AppDatabase
import com.example.football2.repository.GameControlRepository
import com.example.football2.viewModels.LogoHintViewModel
import com.example.football2.viewModels.ViewModelFactory

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var btnSound: Button
    private lateinit var btnVibration: Button
    private lateinit var logoHintViewModel: LogoHintViewModel

    private var isSoundOn = true
    private var isVibrationOn = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("game_settings", Context.MODE_PRIVATE)

        val database = AppDatabase.getDatabase(this)
        val gameControlRepo = GameControlRepository(
            database.gameControlDao(),
            database.logoDao(),
            database.levelDao(),
            database.hintDao(),
            database.logoHintDao()
        )
        val factory = ViewModelFactory(
            logoHintRepository = com.example.football2.repository.LogoHintRepository(database.logoHintDao()),
            gameControlRepository = gameControlRepo
        )
        logoHintViewModel = ViewModelProvider(this, factory)[LogoHintViewModel::class.java]

        initViews()
        loadSettings()
        setupClickListeners()
    }

    private fun initViews() {
        btnSound = findViewById(R.id.btnSound)
        btnVibration = findViewById(R.id.btnVibration)

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun loadSettings() {
        isSoundOn = prefs.getBoolean("sound_effects", true)
        isVibrationOn = prefs.getBoolean("vibration", true)

        updateSoundButtonText()
        updateVibrationButtonText()
    }

    private fun setupClickListeners() {
        btnSound.setOnClickListener {
            isSoundOn = !isSoundOn
            prefs.edit().putBoolean("sound_effects", isSoundOn).apply()
            updateSoundButtonText()
        }

        btnVibration.setOnClickListener {
            isVibrationOn = !isVibrationOn
            prefs.edit().putBoolean("vibration", isVibrationOn).apply()
            updateVibrationButtonText()
            if (isVibrationOn) triggerVibration()
        }

        findViewById<Button>(R.id.btnRate).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            }
        }

        findViewById<Button>(R.id.btnShare).setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Football Logo Quiz")
                putExtra(Intent.EXTRA_TEXT, "جرب هذه اللعبة الممتعة وخمن شعارات الأندية: https://play.google.com/store/apps/details?id=$packageName")
            }
            startActivity(Intent.createChooser(shareIntent, "مشاركة عبر"))
        }

        findViewById<Button>(R.id.btnCheckUpdates).setOnClickListener {
            Toast.makeText(this, "أنت تستخدم أحدث إصدار من اللعبة", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnResetGame).setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    private fun updateSoundButtonText() {
        btnSound.text = if (isSoundOn) "SOUND EFFECTS ON" else "SOUND EFFECTS OFF"
    }

    private fun updateVibrationButtonText() {
        btnVibration.text = if (isVibrationOn) "VIBRATION ON" else "VIBRATION OFF"
    }

    private fun triggerVibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("إعادة ضبط اللعبة")
            .setMessage("هل أنت تأكد من إرجاع جميع المستويات والنقاط والتلميحات للحالة الافتراضية؟")
            .setPositiveButton("نعم") { _, _ ->
                logoHintViewModel.performResetGame()
                Toast.makeText(this, "تم إعادة تمهيد اللعبة بنجاح", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}