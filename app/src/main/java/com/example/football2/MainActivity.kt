package com.example.football2

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import com.example.football2.repository.LevelRepository
import com.example.football2.viewModels.LevelViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val logosActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 🟢 يعمل هذا الكود فوراً وبمجرد إغلاق شاشة اللعب والعودة للواجهة!
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

        // 1. إعداد الـ RecyclerView
        setupRecyclerView()

        // 2. تفعيل المراقبة الحية للبيانات
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // إعادة جلب البيانات فور العودة من شاشة اللعب
        levelViewModel.fetchLevel2sWithStats()
    }

    private fun observeViewModel() {

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                levelViewModel.levelsWithStats.collect { list ->
                    if (list.isNotEmpty()) {
                        // 1. إرسال نسخة جديدة كلياً من القائمة
                        levelAdapter.submitList(list.toList()) {
                            // 2. 🟢 هذا هو السطر المنقذ: يُجبر الـ RecyclerView على إعادة رسم الواجهة فوراً
                            levelAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        levelAdapter = LevelAdapter { selectedLevel ->
            val currentList = levelAdapter.currentList
            val currentIndex = currentList.indexOf(selectedLevel)

            // فحص فتح المستوى بناءً على المستوى السابق
            val isLevelUnlocked = if (currentIndex == 0) {
                true
            } else {
                val previousLevel = currentList[currentIndex - 1]
                previousLevel.completed_logos_count >= 3
            }

            if (isLevelUnlocked || selectedLevel.level.leid == 1) {
                val intent = Intent(this, LogosActivity::class.java).apply {
                    putExtra("LEVEL_ID", selectedLevel.level.leid)
                }
                startActivity(intent)
            } else {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setMessage("لفتح هذا المستوى، يجب حل 3 شعارات صحيحة في المستوى السابق أولاً!")
                    .setPositiveButton("حسناً", null)
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

