package com.example.football2

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.football2.adabter.LevelAdapter
import com.example.football2.databinding.ActivityMainBinding
import com.example.football2.db.AppDatabase
import com.example.football2.repository.GameControlRepository
import com.example.football2.repository.HintRepository
import com.example.football2.repository.LevelRepository
import com.example.football2.repository.LogoRepository
import com.example.football2.viewModels.LevelViewModel
import com.example.football2.viewModels.LogoViewModel
import com.example.football2.viewModels.ViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var logoViewModel: LogoViewModel
    private var currentLevelName: String = ""

    private val logosActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // يعمل هذا الكود فوراً وبمجرد إغلاق شاشة اللعب والعودة للواجهة!
        levelViewModel.fetchLevel2sWithStats()
    }

    private val levelViewModel: LevelViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = AppDatabase.getDatabase(applicationContext)
                val repository = LevelRepository(database.levelDao())
                @Suppress("UNCHECKED_CAST")
                return LevelViewModel(repository) as T
            }
        }
    }

    private lateinit var levelAdapter: LevelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        setupToolbar()
        // 1. إعداد الـ RecyclerView مع معالجة الفتح اليدوي
        setupRecyclerView()

        // 2. تفعيل المراقبة الحية للبيانات
        observeViewModel()
    }

    private fun setupToolbar() {
        // ضبط العنوان ليكون LEVELS
        val tvHeaderTitle = binding.titleBar1.findViewById<TextView>(R.id.tvHeaderTitle)
            ?: binding.titleBar1.findViewById<TextView>(R.id.title)
        tvHeaderTitle?.text = "LEVELS"

        // ضبط زر الرجوع
        val btnBack = binding.titleBar1.findViewById<View>(R.id.btnBack)
            ?: binding.titleBar1.findViewById<View>(R.id.back1)
        btnBack?.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // إعادة جلب البيانات فور العودة من شاشة اللعب
        levelViewModel.fetchLevel2sWithStats()
        updateHeaderCounter()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                levelViewModel.levelsWithStats.collect { list ->
                    if (list.isNotEmpty()) {
                        levelAdapter.submitList(list.toList()) {
                            levelAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
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
        }

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
        }
    }

//    private fun setupRecyclerView() {
//        levelAdapter = LevelAdapter { selectedLevel ->
//            val currentList = levelAdapter.currentList
//            val currentIndex = currentList.indexOf(selectedLevel)
//
//            // 1. فحص هل المستوى مفتوح تلقائياً بناءً على إنجاز المستوى السابق أو إن كان المستوى الأول
//            val isLevelUnlocked = if (currentIndex == 0) {
//                true
//            } else {
//                val previousLevel = currentList[currentIndex - 1]
//                previousLevel.completed_logos_count >= 3 || selectedLevel.level.leOpen == 1
//            }
//
//            if (isLevelUnlocked || selectedLevel.level.leid == 1) {
//                // فتح شاشة الشعارات عند توفر الشرط
//                val intent = Intent(this, LogosActivity::class.java).apply {
//                    putExtra("LEVEL_ID", selectedLevel.level.leid)
//                    putExtra("LEVEL_NAME", selectedLevel.level.leCountry ?: "LOGOS")
//                }
//                logosActivityLauncher.launch(intent)
//            } else {
//                // 2. 🟢 الفتح اليدوي: إظهار مربع حوار يتيح للمستخدم الشراء/الفتح يدويًا بالنقاط
//                val unlockCost = 500
//                AlertDialog.Builder(this)
//                    .setTitle("فتح المستوى يدويًا")
//                    .setMessage("هذا المستوى مغلق. هل ترغب في فتحه يدويًا مقابل $unlockCost نقطة؟")
//                    .setPositiveButton("فتح الآن") { _, _ ->
//                        lifecycleScope.launch {
//                            val isUnlocked = logoViewModel.unlockLevelManually(selectedLevel.level.leid, unlockCost)
//                            if (isUnlocked) {
//                                Toast.makeText(this@MainActivity, "تم فتح المستوى بنجاح!", Toast.LENGTH_SHORT).show()
//                                // تحديث البيانات والعداد
//                                levelViewModel.fetchLevel2sWithStats()
//                                updateHeaderCounter()
//                            } else {
//                                Toast.makeText(this@MainActivity, "نقاطك غير كافية لفتح هذا المستوى!", Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                    }
//                    .setNegativeButton("إلغاء", null)
//                    .show()
//            }
//        }
//
//        binding.rvLevels.apply {
//            layoutManager = GridLayoutManager(this@MainActivity, 2)
//            adapter = levelAdapter
//            setHasFixedSize(true)
//        }
//    }


    private fun setupRecyclerView() {
        levelAdapter = LevelAdapter { selectedLevel ->
            val currentList = levelAdapter.currentList
            val currentIndex = currentList.indexOf(selectedLevel)

            // فحص ما إذا كان المستوى مفتوحاً بالطريقة العادية
            val isLevelUnlocked = selectedLevel.level.leOpen == 1 || currentIndex == 0 ||
                    (currentIndex > 0 && currentList[currentIndex - 1].completed_logos_count >= 3)

            if (isLevelUnlocked || selectedLevel.level.leid == 1) {
                // فتح شاشة الشعارات مباشرة
                val intent = Intent(this, LogosActivity::class.java).apply {
                    putExtra("LEVEL_ID", selectedLevel.level.leid ?: 1)
                    putExtra("LEVEL_NAME", selectedLevel.level.leCountry ?: "LOGOS")
                }
                logosActivityLauncher.launch(intent)
            } else {
                // 🟢 إذا كان المستوى مغلقاً -> نطلب منه الشراء يدوياً
                val unlockCost = 500
                val levelId = selectedLevel.level.leid ?: return@LevelAdapter

                AlertDialog.Builder(this)
                    .setTitle("فتح المستوى يدويًا")
                    .setMessage("هذا المستوى مغلق. هل ترغب في فتحه مقابل $unlockCost نقطة؟")
                    .setPositiveButton("فتح الآن") { _, _ ->

                        // 🟢 هنا يتم استدعاء الدالة داخل Coroutine Scope
                        lifecycleScope.launch {
                            val isUnlocked = logoViewModel.unlockLevelManually(levelId, unlockCost)

                            if (isUnlocked) {
                                Toast.makeText(this@MainActivity, "تم فتح المستوى بنجاح!", Toast.LENGTH_SHORT).show()

                                // تحديث القائمة والعداد في أعلى الواجهة فوراً بعد الخصم
                                levelViewModel.fetchLevel2sWithStats()
                                updateHeaderCounter()
                            } else {
                                Toast.makeText(this@MainActivity, "نقاطك غير كافية لفتح هذا المستوى!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("إلغاء", null)
                    .show()
            }
        }

        binding.rvLevels.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = levelAdapter
            setHasFixedSize(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}