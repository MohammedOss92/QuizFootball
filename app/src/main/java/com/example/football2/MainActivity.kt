package com.example.football2

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.football.R
import com.example.football.databinding.ActivityMainBinding
import com.example.football2.repository.LevelRepository
import com.example.football2.db.AppDatabase
import com.example.football2.adabter.LevelAdapter
import com.example.football2.viewModels.LevelViewModel
import com.example.football2.viewModels.ViewModelFactory
import kotlinx.coroutines.launch
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    // بدلاً من استخدام lateinit، قم بتعريفه هكذا مباشرة في أعلى الكلاس:
    //الحل الأول: التثبيت التلقائي باستخدام الـ KTX extensions (الأسهل والأفضل)
    //إذا كنت تستخدم مكتبة fragment-ktx أو activity-ktx في ملف الـ build.gradle لديك، يمكنك تهيئة الـ ViewModel مباشرة عند تعريفه في أعلى الكلاس دون الحاجة لـ lateinit:
    //private val levelViewModel: LevelViewModel by viewModels()
    // نقوم بإنشاء مصنع (Factory) لتمرير البارامترات المطلوبة للـ ViewModel
    private val levelViewModel: LevelViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // قم بتمرير الـ repository أو المتغيرات التي يطلبها الـ ViewModel هنا
                val database = AppDatabase.getDatabase(applicationContext)
                val repository = LevelRepository(database.levelDao())

                return LevelViewModel(repository) as T
            }
        }
    }
    private lateinit var levelAdapter: LevelAdapter // 1. تعريف الـ Adapter هنا

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // إعداد الـ Database والـ ViewModel... (كودك الحالي)

        // 2. استدعاء دالة الإعداد
        setupRecyclerView()

        levelViewModel.fetchLevelsWithStats()

        // 3. تمرير البيانات عند جاهزيتها
        lifecycleScope.launch {
            levelViewModel.levelsWithStats.collect { list ->
                if (list.isNotEmpty()) {
                    levelAdapter.submitList(list) // تمرير القائمة هنا للـ Adapter
                }
            }
        }
    }

    private fun setupRecyclerView() {
        levelAdapter = LevelAdapter { selectedLevel ->
            // 1. الحصول على القائمة الحالية المعروضة داخل الـ Adapter
            val currentList = levelAdapter.currentList

            // 2. إيجاد ترتيب (Index) المرحلة الحالية التي ضغط عليها المستخدم
            val currentIndex = currentList.indexOf(selectedLevel)

            // 3. فحص هل المرحلة مغلقة أم مفتوحة بناءً على بيانات المرحلة السابقة
            val isLevelUnlocked = if (currentIndex == 0) {
                // المرحلة الأولى دائماً مفتوحة تلقائياً
                true
            } else {
                // المراحل اللاحقة: نأخذ المرحلة السابقة (currentIndex - 1)
                val previousLevel = currentList[currentIndex - 1]

                // الاعتماد على شرط الكود القديم: فحص عدد الشعارات المكتملة في المرحلة السابقة
                // إذا كان عدد الشعارات المحلولة في المرحلة السابقة أكبر من أو يساوي 5 (minimunCompleted)
                // ملاحظة: قم بتعديل أسماء الحقول (completedLogosCount) لتطابق المتغيرات الموجودة لديك في كلاس الإحصائيات (Stats)
                // الوصول المباشر للمتغير داخل كلاس LevelWithStats بدون استخدام كلمة stats
                val completedInPrevious = previousLevel.completed_logos_count

                completedInPrevious >= 5
            }

            // 4. اتخاذ القرار بناءً على حالة القفل
            if (isLevelUnlocked) {
                // المرحلة مفتوحة -> انتقل إلى شاشة الشعارات
                val intent = Intent(this, LogosActivity::class.java).apply {
                    putExtra("LEVEL_ID", selectedLevel.level.leid)
                }
                startActivity(intent)
            } else {
                // المرحلة مغلقة -> إظهار رسالة التنبيه القديمة بنفس النص
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setMessage("لفتح هذا المستوى، يجب عدّ 5 شعارات صحيحة في المستوى السابق أولاً!")
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
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }


}