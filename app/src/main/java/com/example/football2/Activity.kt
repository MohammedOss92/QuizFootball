package com.example.football2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_)

        val btnPlay = findViewById<Button>(R.id.btnPlay)
        val btnStatistics = findViewById<Button>(R.id.btnStatistics)
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        val btnAbout = findViewById<Button>(R.id.btnAbout)

        // الانتقال إلى شاشة اللعب عند الضغط على Play
        btnPlay.setOnClickListener {
            // يمكنك تمرير LEVEL_ID و LOGO_ID افتراضي أو نقله لشاشة اختيار المستويات
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnStatistics.setOnClickListener {
            // أضف شاشة الإحصائيات هنا لاحقاً
            val intent = Intent(this, StaticsActivity::class.java)
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            // أضف شاشة الإعدادات هنا لاحقاً
        }

        btnAbout.setOnClickListener {
            // أضف شاشة حول التطبيق هنا لاحقاً
        }
    }
}