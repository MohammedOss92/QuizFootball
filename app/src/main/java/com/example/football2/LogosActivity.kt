package com.example.football2

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.football2.adabter.LogosAdapter
import com.example.football2.databinding.ActivityLogosBinding
import com.example.football2.db.AppDatabase
import com.example.football2.repository.GameControlRepository
import com.example.football2.repository.HintRepository
import com.example.football2.repository.LogoRepository
import com.example.football2.viewModels.LogoViewModel
import com.example.football2.viewModels.ViewModelFactory
import kotlinx.coroutines.launch

class LogosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogosBinding
    private lateinit var logoViewModel: LogoViewModel
    private lateinit var logosAdapter: LogosAdapter
    private var currentLevelId: Int = 1
    private var currentLevelName: String = "LOGOS"

    private var currentDisplayedScore: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentLevelId = intent.getIntExtra("LEVEL_ID", 1)
        currentLevelName = intent.getStringExtra("LEVEL_NAME") ?: "LOGOS"

        val database = AppDatabase.getDatabase(this)

        val logoRepo = LogoRepository(database.logoDao())
        val hintRepo = HintRepository(database.hintDao())
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
            gameControlRepository = gameControlRepo
        )

        logoViewModel = ViewModelProvider(this, factory)[LogoViewModel::class.java]

        setupHeaderUI()
        setupRecyclerView()
        observeViewModel()

        binding.progressBarLogos.visibility = View.VISIBLE
        logoViewModel.loadLogosForLevel(currentLevelId)
    }

    private fun setupHeaderUI() {
        val tvHeaderTitle = binding.titleBar1.findViewById<TextView>(R.id.tvHeaderTitle)
            ?: binding.titleBar1.findViewById<TextView>(R.id.title)
        tvHeaderTitle?.text = currentLevelName.uppercase()

        val btnBack = binding.titleBar1.findViewById<View>(R.id.btnBack)
            ?: binding.titleBar1.findViewById<View>(R.id.back1)

        btnBack?.setOnClickListener {
            finish()
        }
    }

    private fun updateHeaderCounter() {
        lifecycleScope.launch {
            val totalScore = logoViewModel.getTotalScore()

            val tvCounterValue = binding.titleBar1.findViewById<TextView>(R.id.tvCounterValue)
                ?: binding.titleBar1.findViewById<TextView>(R.id.scoreValue)
            val tvCounterLabel = binding.titleBar1.findViewById<TextView>(R.id.tvCounterLabel)
                ?: binding.titleBar1.findViewById<TextView>(R.id.scoreTitle)

            tvCounterValue?.text = String.format(java.util.Locale.ENGLISH, "%03d", totalScore)
            tvCounterValue?.setTextColor(Color.parseColor("#FF4D4D"))

            tvCounterLabel?.text = "SCORE"
            tvCounterLabel?.setTextColor(Color.parseColor("#FF4D4D"))

            if (tvCounterValue != null) {
                animateScoreCounter(
                    textView = tvCounterValue,
                    fromValue = currentDisplayedScore,
                    toValue = totalScore
                )
                // تحديث القيمة الحالية لتكون المرجع للمرة القادمة
                currentDisplayedScore = totalScore
            }
        }
    }

    private fun animateScoreCounter(textView: TextView, fromValue: Int, toValue: Int) {
        if (fromValue == toValue) {
            textView.text = String.format(java.util.Locale.ENGLISH, "%03d", toValue)
            return
        }

        val animator = android.animation.ValueAnimator.ofInt(fromValue, toValue).apply {
            duration = 800 // مدة الحركة بالملي ثانية (0.8 ثانية)
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Int
                textView.text = String.format(java.util.Locale.ENGLISH, "%03d", animatedValue)
            }
        }
        animator.start()
    }

    private fun setupRecyclerView() {
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
        lifecycleScope.launch {
            logoViewModel.logos.collect { logosList ->
                binding.progressBarLogos.visibility = View.GONE
                logosAdapter.submitList(logosList)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        logoViewModel.loadLogosForLevel(currentLevelId)
        updateHeaderCounter()
    }
}
//package com.example.football2
//
//import android.content.Intent
//import android.graphics.Color
//import android.os.Bundle
//import android.view.View
//import android.widget.ImageButton
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.GridLayoutManager
//import com.example.football2.databinding.ActivityLogosBinding
//import com.example.football2.adabter.LogosAdapter
//import com.example.football2.db.AppDatabase
//import com.example.football2.repository.GameControlRepository
//import com.example.football2.repository.HintRepository
//import com.example.football2.repository.LogoRepository
//import com.example.football2.viewModels.LogoViewModel
//import com.example.football2.viewModels.ViewModelFactory
//
//import kotlinx.coroutines.launch
//
//class LogosActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityLogosBinding
//    private lateinit var logoViewModel: LogoViewModel
//    private lateinit var logosAdapter: LogosAdapter
//    private var currentLevelId: Int = 0
//    private var currentLevelName: String = ""
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityLogosBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        currentLevelId = intent.getIntExtra("LEVEL_ID", 1)
//        currentLevelName = intent.getStringExtra("LEVEL_NAME") ?: "LOGOS"
//
//        val database = AppDatabase.getDatabase(this)
//
//        val logoRepo = LogoRepository(database.logoDao())
//        val hintRepo = HintRepository(database.hintDao())
//        val gameControlRepo = GameControlRepository(
//            database.gameControlDao(),
//            database.logoDao(),
//            database.levelDao(),
//            database.hintDao(),
//            database.logoHintDao()
//        )
//
//        val factory = ViewModelFactory(
//            logoRepository = logoRepo,
//            hintRepository = hintRepo,
//            gameControlRepository = gameControlRepo
//        )
//
//        logoViewModel = ViewModelProvider(this, factory)[LogoViewModel::class.java]
//
//        setupRecyclerView()
//        observeViewModel()
//
//        // 1. إعداد زر الرجوع والعنوان
//        setupHeaderUI()
//
//        binding.progressBarLogos.visibility = View.VISIBLE
//        logoViewModel.loadLogosForLevel(currentLevelId)
//    }
//
//    private fun setupHeaderUI() {
//        // إعداد عنوان المستوى أو الشاشة
//        val tvHeaderTitle = binding.titleBar1.findViewById<TextView>(R.id.tvHeaderTitle)
//            ?: binding.titleBar1.findViewById<TextView>(R.id.title)
////        tvHeaderTitle?.text = "LOGOS"
//        tvHeaderTitle?.text = currentLevelName.uppercase()
//
//        // إعداد زر الرجوع
//        val btnBack = binding.titleBar1.findViewById<View>(R.id.btnBack)
//
//            ?: binding.titleBar1.findViewById<View>(R.id.back1)
//
//        btnBack?.setOnClickListener {
//            finish()
//        }
//    }
//
//    // 2. دالة حساب وتحديث الـ SCORE / HINTS
//    private fun updateHeaderCounter() {
//        lifecycleScope.launch {
//            // جلب المجموع الكلي للـ Score للشعارات المحلولة من الـ Repository
//            val totalScore = logoViewModel.getTotalScore()
//
//            val tvCounterValue = binding.titleBar1.findViewById<TextView>(R.id.tvCounterValue)
//                ?: binding.titleBar1.findViewById<TextView>(R.id.scoreValue)
//            val tvCounterLabel = binding.titleBar1.findViewById<TextView>(R.id.tvCounterLabel)
//                ?: binding.titleBar1.findViewById<TextView>(R.id.scoreTitle)
//
////            tvCounterValue?.text = String.format("%03d", totalScore)
//            tvCounterValue?.text = String.format(java.util.Locale.ENGLISH, "%03d", totalScore)
//            tvCounterValue?.setTextColor(Color.parseColor("#FF4D4D"))
//
//            tvCounterLabel?.text = "SCORE"
//            tvCounterLabel?.setTextColor(Color.parseColor("#FF4D4D"))
//        }
//    }
//
//    private fun setupRecyclerView() {
//        logosAdapter = LogosAdapter { selectedLogo ->
//            val intent = Intent(this, QuizActivity::class.java).apply {
//                putExtra("LOGO_ID", selectedLogo._loid ?: 0)
//                putExtra("LEVEL_ID", currentLevelId)
//            }
//            startActivity(intent)
//        }
//
//        binding.rvLogosGrid.apply {
//            layoutManager = GridLayoutManager(this@LogosActivity, 3)
//            adapter = logosAdapter
//            setHasFixedSize(true)
//        }
//    }
//
//    private fun observeViewModel() {
//        lifecycleScope.launch {
//            logoViewModel.logos.collect { logosList ->
//                binding.progressBarLogos.visibility = View.GONE
//                logosAdapter.submitList(logosList)
//            }
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        logoViewModel.loadLogosForLevel(currentLevelId)
//        // 3. تحديث الـ Score فور العودة من حل لغز في QuizActivity
//        updateHeaderCounter()
//    }
//}
