package com.example.football2

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.football.databinding.ActivityLogosBinding
import com.example.football2.repository.LogoRepository
import com.sarrawi.footballlogoquiz.data.AppDatabase
import com.sarrawi.footballlogoquiz.ui.adapter.LogosAdapter
import com.sarrawi.footballlogoquiz.ui.viewmodel.LogoViewModel
import com.sarrawi.footballlogoquiz.ui.viewmodel.ViewModelFactory

import kotlinx.coroutines.launch
import kotlin.jvm.java


class LogosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogosBinding
    private lateinit var logoViewModel: LogoViewModel
    private lateinit var logosAdapter: LogosAdapter
    private var currentLevelId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. إعداد الـ View Binding لملف XML الذي صممناه سابقاً
        binding = ActivityLogosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. استقبال الـ Level ID الممرر من شاشة المستويات
        currentLevelId = intent.getIntExtra("LEVEL_ID", 1)

        // يمكنك تخصيص العنوان العلوي بناءً على رقم المستوى الممرر
        binding.tvLevelTitle.text = "المستوى $currentLevelId"

        // 3. إعداد الـ ViewModel يدوياً بدون Injection باستخدام الـ Factory
        val database = AppDatabase.getDatabase(this)
        val logoRepository = LogoRepository(database.logoDao())
        val factory = ViewModelFactory(logoRepository = logoRepository)
        logoViewModel = ViewModelProvider(this, factory)[LogoViewModel::class.java]

        // 4. تهيئة الـ RecyclerView والـ Adapter
        setupRecyclerView()

        // 5. مراقبة الـ StateFlow لجلب البيانات وتحديث الشاشة تلقائياً
        observeViewModel()

        // 6. تحميل البيانات الخاصة بهذا المستوى
        binding.progressBarLogos.visibility = View.VISIBLE
        logoViewModel.loadLogosForLevel(currentLevelId)

        // زر العودة للخلف
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        // تمرير الكائن المعدل LogoEntity لشاشة الـ Quiz
        logosAdapter = LogosAdapter { selectedLogo ->
            val intent = Intent(this, QuizActivity::class.java).apply {
                putExtra("LOGO_ID", selectedLogo._loid ?: 0)
                putExtra("LEVEL_ID", currentLevelId)
            }
            startActivity(intent)
        }

        binding.rvLogosGrid.apply {
            layoutManager = GridLayoutManager(this@LogosActivity, 3)
            adapter = logosAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        // استخدام lifecycleScope لمراقبة الـ StateFlow بأمان في الـ Activity التقليدية
        lifecycleScope.launch {
            logoViewModel.logos.collect { logosList ->
                // إخفاء مؤشر التحميل بمجرد وصول البيانات
                binding.progressBarLogos.visibility = View.GONE

                // تمرير القائمة المحدثة للـ Adapter ليقوم بعمل الـ DiffUtil وتحديث الواجهة
                logosAdapter.submitList(logosList)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // إعادة تحميل البيانات عند العودة من شاشة اللعب لتحديث الشعارات التي تم حلّها فوراُ
        logoViewModel.loadLogosForLevel(currentLevelId)
    }
}