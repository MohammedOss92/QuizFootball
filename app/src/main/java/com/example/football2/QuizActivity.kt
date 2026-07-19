package com.example.football2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.football.R
import com.example.football.databinding.ActivityQuizBinding
import com.example.football2.adabter.LettersAdapter
import com.example.football2.entity.LogoEntity
import com.example.football2.repository.GameControlRepository
import com.example.football2.repository.HintRepository
import com.example.football2.repository.LogoHintRepository
import com.example.football2.repository.LogoRepository
import com.sarrawi.footballlogoquiz.data.AppDatabase
import com.sarrawi.footballlogoquiz.ui.viewmodel.HintViewModel
import com.sarrawi.footballlogoquiz.ui.viewmodel.LogoHintViewModel
import com.sarrawi.footballlogoquiz.ui.viewmodel.LogoViewModel
import com.sarrawi.footballlogoquiz.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class QuizActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuizBinding

    private lateinit var logoViewModel: LogoViewModel
    private lateinit var hintViewModel: HintViewModel
    private lateinit var logoHintViewModel: LogoHintViewModel

    private var currentLogoId: Int = 0
    private var currentLevelId: Int = 0
    private var currentLogo: LogoEntity? = null

    // متغيرات إضافية لإدارة نظام الحروف والمربعات
    private lateinit var lettersAdapter: LettersAdapter
    private val answerSlots = ArrayList<TextView?>()          // تتبع مربعات الإجابة العلوية
    private val slotSourcePositions = HashMap<Int, Int>()     // يربط بين رقم المربع ومكانه في الـ GridView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. استقبال المعرفات
        currentLogoId = intent.getIntExtra("LOGO_ID", 0)
        currentLevelId = intent.getIntExtra("LEVEL_ID", 0)

        // 2. تجهيز قاعدة البيانات والـ Repositories يدوياً
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

        // 3. بناء الـ ViewModels عبر الـ Factory
        val factory = ViewModelFactory(
            logoRepository = logoRepo,
            hintRepository = hintRepo,
            logoHintRepository = logoHintRepo,
            gameControlRepository = gameControlRepo
        )

        logoViewModel = ViewModelProvider(this, factory)[LogoViewModel::class.java]
        hintViewModel = ViewModelProvider(this, factory)[HintViewModel::class.java]
        logoHintViewModel = ViewModelProvider(this, factory)[LogoHintViewModel::class.java]

        // 4. تحميل البيانات عند فتح الشاشة
        logoViewModel.loadLogosForLevel(currentLevelId)
        hintViewModel.loadCurrentHints()
        logoHintViewModel.loadHintStateForLogo(currentLogoId)

        // 5. مراقبة الـ StateFlows
        observeGameStates()

        // 6. إعداد الأحداث
        setupActions()
    }

    private fun observeGameStates() {
        // مراقبة رصيد المساعدات من الـ HintViewModel لتحديث شريط العنوان
        lifecycleScope.launch {
            hintViewModel.currentHints.collect { hintsCount ->
                val tvHintCount = findViewById<TextView>(R.id.scoreValue)
                tvHintCount?.text = hintsCount.toString()
            }
        }

        // مراقبة بيانات الشعار
        lifecycleScope.launch {
            logoViewModel.logos.collect { logosList ->
                currentLogo = logosList.find { it._loid == currentLogoId }
                currentLogo?.let { logo ->
                    // عرض الشعار
                    val resId = resources.getIdentifier(logo.lo_image, "drawable", packageName)
                    if (resId != 0) binding.logo.setImageResource(resId)

                    // التحقق إذا كان الشعار محلولاً مسبقاً لإظهار شاشة الاكتمال
                    if (logo.lo_completed == "1") {
                        showCompletedLayout(logo)
                    } else {
                        binding.completedLayout.visibility = View.GONE
                        binding.leftHints.visibility = View.VISIBLE
                        binding.rightHints.visibility = View.VISIBLE
                        binding.ballsGrid.visibility = View.VISIBLE

                        // تهيئة الحروف والمربعات بناءً على الإجابة الصحيحة
                        setupKeyboard(logo.lo_name ?: "")
                    }
                }
            }
        }

        // مراقبة حالة المساعدات للشعار الحالي
        lifecycleScope.launch {
            logoHintViewModel.currentLogoHintState.collect { hintEntity ->
                hintEntity?.let {
                    if (it.facebook == 1) {
                        // تفعيل ميزة التلميح بدون خصم نقاط مجدداً إن لزم الأمر
                    }
                }
            }
        }
    }

    private fun setupActions() {
        // حدث ضغط زر الفيسبوك
        binding.facebook.setOnClickListener {
            handleHintUsage {
                logoHintViewModel.unlockFacebookHint(currentLogoId)
                Toast.makeText(this, "تم فتح تلميح فيسبوك", Toast.LENGTH_SHORT).show()
            }
        }

        // حدث ضغط زر تلميح معلومات النادي (Info)
//        binding.info.setOnClickListener {
//            handleHintUsage {
//                binding.infoText.text = "تأسس هذا النادي في دولة: ${currentLogo?.lo_name}"
//                binding.infoPopup.visibility = View.VISIBLE
//            }
//        }

        binding.info.setOnClickListener {
            // 1. إظهار دايلوج التأكيد القياسي أولاً لحماية النقاط/المساعدات
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Hints")
            builder.setMessage("Show a clue sentence of the answer!\nCost : 1 hint")

            builder.setPositiveButton("OK") { dialog, _ ->
                // 2. عند الضغط على OK نقوم بتنفيذ منطق خصم المساعدة وإظهار النص من الـ DB
                handleHintUsage {
                    // جلب النص ديناميكياً من حقل lo_info للشعار الحالي
                    val infoMessage = currentLogo?.lo_info ?: "لا توجد معلومات متاحة لهذا النادي"

                    // إظهار نافذة الكارد الأسود المخصصة وتمرير النص لها مع أيقونة الويكيبيديا
                    showBlackCustomDialog(infoMessage, R.drawable.wikipedia_pressed)
                }
                dialog.dismiss()
            }



            builder.setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()

            // تلوين الأزرار باللون البنفسجي المطابق لتطبيقك الأصلي
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
        }

        fun showBlackCustomDialog(message: String, imageResId: Int) {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_hint_info, null)
            builder.setView(dialogView)

            val dialog = builder.create()

            val ivDialogIcon = dialogView.findViewById<android.widget.ImageView>(R.id.ivDialogIcon)
            val tvDialogMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
            val btnOk = dialogView.findViewById<android.widget.Button>(R.id.btnOk)

            ivDialogIcon.setImageResource(imageResId)
            tvDialogMessage.text = message

            btnOk.setOnClickListener {
                dialog.dismiss()
            }

            // جعل خلفية النافذة شفافة ليظهر الكارد الأسود ذو الحواف الدائرية بشكل صحيح
            dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            dialog.show()
        }
        // إغلاق بوب أب المعلومات
        binding.okInfo.setOnClickListener {
            binding.infoPopup.visibility = View.GONE
        }

        // حدث ضغط زر تلميح اللاعب (Player)
        binding.player.setOnClickListener {
            handleHintUsage {
                binding.playerName.text = "معلومات إضافية متوفرة"
                binding.playerPopup.visibility = View.VISIBLE
            }
        }

        // إغلاق بوب أب اللاعب
        binding.okPlayer.setOnClickListener {
            binding.playerPopup.visibility = View.GONE
        }

        // أزرار الانتقال بين الشعارات
        binding.nextLogoButton.setOnClickListener {
            navigateToNextLogo()
        }

        binding.prevLogoButton.setOnClickListener {
            navigateToPrevLogo()
        }

        binding.hide.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Hints")
            builder.setMessage("Remove the wrong letters!\nCost : 1 hint")

            builder.setPositiveButton("OK") { dialog, _ ->
                handleHintUsage {
                    val correctAnswer = currentLogo?.lo_name
                    if (!correctAnswer.isNullOrEmpty()) {
                        // استدعاء دالة الحذف المحدثة
                        lettersAdapter.removeWrongLetters(correctAnswer)
                    }
                }
                dialog.dismiss()
            }

            builder.setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()

            // 🟢 التصحيح هنا: استخدام android.content.DialogInterface للوصول للأزرار وتلوينها 🟢
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
            dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
        }
    }

    fun showBlackCustomDialog(message: String, imageResId: Int) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_hint_info, null)
        builder.setView(dialogView)

        val dialog = builder.create()

        val ivDialogIcon = dialogView.findViewById<android.widget.ImageView>(R.id.ivDialogIcon)
        val tvDialogMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnOk = dialogView.findViewById<android.widget.Button>(R.id.btnOk)

        ivDialogIcon.setImageResource(imageResId)
        tvDialogMessage.text = message

        btnOk.setOnClickListener {
            dialog.dismiss()
        }

        // جعل خلفية النافذة شفافة ليظهر الكارد الأسود ذو الحواف الدائرية بشكل صحيح
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.show()
    }

    // منطق توليد وتوزيع لوحة الحروف والمربعات ديناميكياً
    private fun setupKeyboard(correctAnswer: String) {
        val shuffledLetters = generateShuffledLetters(correctAnswer)

        // 1. إعداد لوحة الحروف بالأسفل عبر الـ Adapter
        lettersAdapter = LettersAdapter(this, shuffledLetters) { position, letter ->
            addLetterToAnswer(position, letter)
        }
        binding.ballsGrid.adapter = lettersAdapter

        // 2. تنظيف حاويات مربعات الإجابة العلوية
        binding.spacesGrid1.removeAllViews()
        binding.spacesGrid2.removeAllViews()
        answerSlots.clear()
        slotSourcePositions.clear()

        // 3. بناء مربعات الإجابة العلوية فارغة تماماً
        for (i in correctAnswer.indices) {
            if (correctAnswer[i] == ' ') {
                val spaceView = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(24, 10)
                }
                binding.spacesGrid1.addView(spaceView)
                answerSlots.add(null)
            } else {
                // عمل inflate للمربع العلوي
                val slotView = LayoutInflater.from(this).inflate(R.layout.item_letter_ball, binding.spacesGrid1, false) as FrameLayout
                val tvSlot = slotView.findViewById<TextView>(R.id.tvLetter)

                // تأكيد تفريغ النص تماماً حتى لا يظهر أي حرف مسبق
                tvSlot.text = ""

                // تعيين الخلفية المخصصة للمربعات العلوية الفارغة
                tvSlot.setBackgroundResource(R.drawable.hint_background)

                val slotIndex = i
                slotView.setOnClickListener {
                    removeLetterFromAnswer(slotIndex)
                }

                // توزيع المربعات العلوية على السطر الأول أو الثاني لمنع الخروج عن الشاشة
                if (i < 8) {
                    binding.spacesGrid1.addView(slotView)
                } else {
                    binding.spacesGrid2.addView(slotView)
                }

                // الاحتفاظ بمرجع المربع العلوي لإضافة الحروف إليه لاحقاً
                answerSlots.add(tvSlot)
            }
        }
    }

    private fun addLetterToAnswer(gridPosition: Int, letter: Char) {
        for (i in answerSlots.indices) {
            val tvSlot = answerSlots[i]
            if (tvSlot != null && tvSlot.text.isEmpty()) {
                tvSlot.text = letter.toString()
                slotSourcePositions[i] = gridPosition
                lettersAdapter.hideLetter(gridPosition)
                checkAnswerComplete()
                break
            }
        }
    }

    private fun removeLetterFromAnswer(slotIndex: Int) {
        val tvSlot = answerSlots[slotIndex]
        if (tvSlot != null && tvSlot.text.isNotEmpty()) {
            val originalGridPos = slotSourcePositions[slotIndex]
            if (originalGridPos != null) {
                lettersAdapter.showLetter(originalGridPos)
            }
            tvSlot.text = ""
            slotSourcePositions.remove(slotIndex)
        }
    }

    private fun checkAnswerComplete() {
        val currentEnteredAnswer = answerSlots.map { it?.text ?: " " }.joinToString("").trim()
        val realAnswer = currentLogo?.lo_name?.trim() ?: ""

        if (currentEnteredAnswer.length == realAnswer.length) {
            if (currentEnteredAnswer.equals(realAnswer, ignoreCase = true)) {
                binding.whistle.visibility = View.VISIBLE
                showCompletedLayout(currentLogo!!)
                logoHintViewModel.submitCorrectAnswer(currentLogoId, 100, currentLevelId)
            } else {
                binding.wrong.visibility = View.VISIBLE
                binding.root.postDelayed({ binding.wrong.visibility = View.GONE }, 1500)
            }
        }
    }

    private fun generateShuffledLetters(answer: String): List<Char> {
        val cleanAnswer = answer.replace(" ", "").uppercase().trim()
        val lettersList = cleanAnswer.toMutableList()
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

        while (lettersList.size < 18) {
            val randomChar = alphabet.random()
            if (lettersList.count { it == randomChar } < 2) {
                lettersList.add(randomChar)
            }
        }
        return lettersList.shuffled()
    }

    private inline fun handleHintUsage(onHintUnlocked: () -> Unit) {
        if (hintViewModel.currentHints.value > 0) {
            hintViewModel.useHint()
            onHintUnlocked()
        } else {
            Toast.makeText(this, "لا يوجد رصيد مساعدات كافٍ!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCompletedLayout(logo: LogoEntity) {
        binding.leftHints.visibility = View.GONE
        binding.rightHints.visibility = View.GONE
        binding.ballsGrid.visibility = View.GONE

        binding.completedLayout.visibility = View.VISIBLE
        binding.loName.text = logo.lo_name
        binding.points.text = "${logo.lo_points} Pt"
    }

    private fun navigateToNextLogo() {
        val logosList = logoViewModel.logos.value
        if (logosList.isNotEmpty()) {
            val currentIndex = logosList.indexOfFirst { it._loid == currentLogoId }

            if (currentIndex != -1 && currentIndex < logosList.size - 1) {
                val nextLogo = logosList[currentIndex + 1]
                updateActivityForNewLogo(nextLogo._loid ?:0)
            } else {
                Toast.makeText(this, "لقد وصلت لآخر شعار في هذا المستوى!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToPrevLogo() {
        val logosList = logoViewModel.logos.value
        if (logosList.isNotEmpty()) {
            val currentIndex = logosList.indexOfFirst { it._loid == currentLogoId }

            if (currentIndex > 0) {
                val prevLogo = logosList[currentIndex - 1]
                // استخدام ?: 0 لتجنب مشكلة الـ Int? وتمرير 0 كقيمة بديلة افتراضية
                updateActivityForNewLogo(prevLogo._loid ?: 0)
            } else {
                Toast.makeText(this, "هذا هو الشعار الأول في المستوى!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateActivityForNewLogo(newLogoId: Int) {
        currentLogoId = newLogoId

        // إعادة تصفير النوافذ المنبثقة وحالة التحقق السابقة لضمان عدم تداخل البيانات
        binding.whistle.visibility = View.GONE
        binding.wrong.visibility = View.GONE
        binding.infoPopup.visibility = View.GONE
        binding.playerPopup.visibility = View.GONE
        binding.infoText.text = ""
        binding.playerName.text = ""

        // إعادة تحميل حالة المساعدات للشعار الجديد
        logoHintViewModel.loadHintStateForLogo(currentLogoId)

        val logosList = logoViewModel.logos.value
        currentLogo = logosList.find { it._loid == currentLogoId }
        currentLogo?.let { logo ->
            val resId = resources.getIdentifier(logo.lo_image, "drawable", packageName)
            if (resId != 0) binding.logo.setImageResource(resId)

            if (logo.lo_completed == "1") {
                showCompletedLayout(logo)
            } else {
                binding.completedLayout.visibility = View.GONE
                binding.leftHints.visibility = View.VISIBLE
                binding.rightHints.visibility = View.VISIBLE
                binding.ballsGrid.visibility = View.VISIBLE
                setupKeyboard(logo.lo_name ?: "")
            }
        }
    }
}

//package com.example.football2
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.widget.FrameLayout
//import android.widget.LinearLayout
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.lifecycleScope
//import com.example.football.R
//import com.example.football.databinding.ActivityQuizBinding
//import com.example.football2.adabter.LettersAdapter
//import com.example.football2.entity.LogoEntity
//import com.example.football2.repository.GameControlRepository
//import com.example.football2.repository.HintRepository
//import com.example.football2.repository.LogoHintRepository
//import com.example.football2.repository.LogoRepository
//import com.sarrawi.footballlogoquiz.data.AppDatabase
//import com.sarrawi.footballlogoquiz.ui.viewmodel.HintViewModel
//import com.sarrawi.footballlogoquiz.ui.viewmodel.LogoHintViewModel
//import com.sarrawi.footballlogoquiz.ui.viewmodel.LogoViewModel
//import com.sarrawi.footballlogoquiz.ui.viewmodel.ViewModelFactory
//
//import kotlinx.coroutines.launch
//
//class QuizActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityQuizBinding
//
//    private lateinit var logoViewModel: LogoViewModel
//    private lateinit var hintViewModel: HintViewModel
//    private lateinit var logoHintViewModel: LogoHintViewModel
//
//    private var currentLogoId: Int = 0
//    private var currentLevelId: Int = 0
//    private var currentLogo: LogoEntity? = null
//
//    // متغيرات إضافية لإدارة نظام الحروف والمربعات
//    private lateinit var lettersAdapter: LettersAdapter
//    private val answerSlots = ArrayList<TextView?>()          // تتبع مربعات الإجابة العلوية
//    private val slotSourcePositions = HashMap<Int, Int>()     // يربط بين رقم المربع ومكانه في الـ GridView
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityQuizBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // 1. استقبال المعرفات
//        currentLogoId = intent.getIntExtra("LOGO_ID", 0)
//        currentLevelId = intent.getIntExtra("LEVEL_ID", 0)
//
//        // 2. تجهيز قاعدة البيانات والـ Repositories يدوياً
//        val database = AppDatabase.getDatabase(this)
//        val logoRepo = LogoRepository(database.logoDao())
//        val hintRepo = HintRepository(database.hintDao())
//        val logoHintRepo = LogoHintRepository(database.logoHintDao())
//        val gameControlRepo = GameControlRepository(
//            database.gameControlDao(),
//            database.logoDao(),
//            database.levelDao(),
//            database.hintDao(),
//            database.logoHintDao()
//        )
//
//        // 3. بناء الـ ViewModels عبر الـ Factory
//        val factory = ViewModelFactory(
//            logoRepository = logoRepo,
//            hintRepository = hintRepo,
//            logoHintRepository = logoHintRepo,
//            gameControlRepository = gameControlRepo
//        )
//
//        logoViewModel = ViewModelProvider(this, factory)[LogoViewModel::class.java]
//        hintViewModel = ViewModelProvider(this, factory)[HintViewModel::class.java]
//        logoHintViewModel = ViewModelProvider(this, factory)[LogoHintViewModel::class.java]
//
//        // 4. تحميل البيانات عند فتح الشاشة
//        logoViewModel.loadLogosForLevel(currentLevelId)
//        hintViewModel.loadCurrentHints()
//        logoHintViewModel.loadHintStateForLogo(currentLogoId)
//
//        // 5. مراقبة الـ StateFlows
//        observeGameStates()
//
//        // 6. إعداد الأحداث
//        setupActions()
//    }
//
//    private fun observeGameStates() {
//        // مراقبة رصيد المساعدات من الـ HintViewModel لتحديث شريط العنوان
//        // مراقبة رصيد المساعدات من الـ HintViewModel لتحديث شريط العنوان
//        lifecycleScope.launch {
//            hintViewModel.currentHints.collect { hintsCount ->
//                // البحث عن المعرف الفعلي في الشاشة وتحديثه مباشرة
//                val tvHintCount = findViewById<TextView>(R.id.scoreValue)
//                tvHintCount?.text = hintsCount.toString()
//            }
//        }
//
//        // مراقبة بيانات الشعار
//        lifecycleScope.launch {
//            logoViewModel.logos.collect { logosList ->
//                currentLogo = logosList.find { it._loid == currentLogoId }
//                currentLogo?.let { logo ->
//                    // عرض الشعار
//                    val resId = resources.getIdentifier(logo.lo_image, "drawable", packageName)
//                    if (resId != 0) binding.logo.setImageResource(resId)
//
//                    // التحقق إذا كان الشعار محلولاً مسبقاً لإظهار شاشة الاكتمال
//                    if (logo.lo_completed == "1") {
//                        showCompletedLayout(logo)
//                    } else {
//                        binding.completedLayout.visibility = View.GONE
//                        binding.leftHints.visibility = View.VISIBLE
//                        binding.rightHints.visibility = View.VISIBLE
//                        binding.ballsGrid.visibility = View.VISIBLE
//
//                        // تهيئة الحروف والمربعات بناءً على الإجابة الصحيحة
//                        setupKeyboard(logo.lo_name ?: "")
//                    }
//                }
//            }
//        }
//
//        // مراقبة حالة المساعدات للشعار الحالي
//        lifecycleScope.launch {
//            logoHintViewModel.currentLogoHintState.collect { hintEntity ->
//                hintEntity?.let {
//                    if (it.facebook == 1) {
//                        // تفعيل ميزة التلميح بدون خصم نقاط مجدداً إن لزم الأمر
//                    }
//                }
//            }
//        }
//    }
//
//    private fun setupActions() {
//        // حدث ضغط زر الفيسبوك
//        binding.facebook.setOnClickListener {
//            handleHintUsage {
//                logoHintViewModel.unlockFacebookHint(currentLogoId)
//                Toast.makeText(this, "تم فتح تلميح فيسبوك", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        // حدث ضغط زر تلميح معلومات النادي (Info)
//        binding.info.setOnClickListener {
//            handleHintUsage {
//                binding.infoText.text = "تأسس هذا النادي في دولة: ${currentLogo?.lo_name}"
//                binding.infoPopup.visibility = View.VISIBLE
//            }
//        }
//
//        // إغلاق بوب أب المعلومات
//        binding.okInfo.setOnClickListener {
//            binding.infoPopup.visibility = View.GONE
//        }
//
//        // حدث ضغط زر تلميح اللاعب (Player)
//        binding.player.setOnClickListener {
//            handleHintUsage {
//                binding.playerName.text = "معلومات إضافية متوفرة"
//                binding.playerPopup.visibility = View.VISIBLE
//            }
//        }
//
//        // إغلاق بوب أب اللاعب
//        binding.okPlayer.setOnClickListener {
//            binding.playerPopup.visibility = View.GONE
//        }
//
//        // أزرار الانتقال عند اكتمال الشعار
//        binding.nextLogoButton.setOnClickListener {
//            // منطق الانتقال للشعار التالي
//        }
//
//        binding.prevLogoButton.setOnClickListener {
//            // منطق الانتقال للشعار السابق
//        }
//    }
//
//    // منطق توليد وتوزيع لوحة الحروف والمربعات ديناميكياً
//    private fun setupKeyboard(correctAnswer: String) {
//        val shuffledLetters = generateShuffledLetters(correctAnswer)
//
//        // 1. إعداد لوحة الحروف بالأسفل عبر الـ Adapter
//        lettersAdapter = LettersAdapter(this, shuffledLetters) { position, letter ->
//            addLetterToAnswer(position, letter)
//        }
//        binding.ballsGrid.adapter = lettersAdapter
//
//        // 2. تنظيف حاويات مربعات الإجابة العلوية
//        binding.spacesGrid1.removeAllViews()
//        binding.spacesGrid2.removeAllViews()
//        answerSlots.clear()
//        slotSourcePositions.clear()
//
//        // 3. بناء مربعات الإجابة العلوية فارغة تماماً
//        for (i in correctAnswer.indices) {
//            if (correctAnswer[i] == ' ') {
//                val spaceView = View(this).apply {
//                    layoutParams = LinearLayout.LayoutParams(24, 10)
//                }
//                binding.spacesGrid1.addView(spaceView)
//                answerSlots.add(null)
//            } else {
//                // عمل inflate للمربع العلوي
//                val slotView = LayoutInflater.from(this).inflate(R.layout.item_letter_ball, binding.spacesGrid1, false) as FrameLayout
//                val tvSlot = slotView.findViewById<TextView>(R.id.tvLetter)
//
//                // تأكيد تفريغ النص تماماً حتى لا يظهر أي حرف مسبق
//                tvSlot.text = ""
//
//                // تعيين الخلفية المخصصة للمربعات العلوية الفارغة
//                tvSlot.setBackgroundResource(R.drawable.hint_background)
//
//                val slotIndex = i
//                slotView.setOnClickListener {
//                    removeLetterFromAnswer(slotIndex)
//                }
//
//                // توزيع المربعات العلوية على السطر الأول أو الثاني لمنع الخروج عن الشاشة
//                if (i < 8) {
//                    binding.spacesGrid1.addView(slotView)
//                } else {
//                    binding.spacesGrid2.addView(slotView)
//                }
//
//                // الاحتفاظ بمرجع المربع العلوي لإضافة الحروف إليه لاحقاً
//                answerSlots.add(tvSlot)
//            }
//        }
//    }
//
//    private fun addLetterToAnswer(gridPosition: Int, letter: Char) {
//        for (i in answerSlots.indices) {
//            val tvSlot = answerSlots[i]
//            if (tvSlot != null && tvSlot.text.isEmpty()) {
//                tvSlot.text = letter.toString()
//                slotSourcePositions[i] = gridPosition
//                lettersAdapter.hideLetter(gridPosition)
//                checkAnswerComplete()
//                break
//            }
//        }
//    }
//
//    private fun removeLetterFromAnswer(slotIndex: Int) {
//        val tvSlot = answerSlots[slotIndex]
//        if (tvSlot != null && tvSlot.text.isNotEmpty()) {
//            val originalGridPos = slotSourcePositions[slotIndex]
//            if (originalGridPos != null) {
//                lettersAdapter.showLetter(originalGridPos)
//            }
//            tvSlot.text = ""
//            slotSourcePositions.remove(slotIndex)
//        }
//    }
//
//    private fun checkAnswerComplete() {
//        val currentEnteredAnswer = answerSlots.map { it?.text ?: " " }.joinToString("").trim()
//        val realAnswer = currentLogo?.lo_name?.trim() ?: ""
//
//        if (currentEnteredAnswer.length == realAnswer.length) {
//            if (currentEnteredAnswer.equals(realAnswer, ignoreCase = true)) {
//                binding.whistle.visibility = View.VISIBLE
//                showCompletedLayout(currentLogo!!)
//                logoHintViewModel.submitCorrectAnswer(currentLogoId, 100, currentLevelId)
//            } else {
//                binding.wrong.visibility = View.VISIBLE
//                binding.root.postDelayed({ binding.wrong.visibility = View.GONE }, 1500)
//            }
//        }
//    }
//
//    private fun generateShuffledLetters(answer: String): List<Char> {
//        // 1. تنظيف نص الإجابة بالكامل وتحويله لحروف كبيرة (مثال: "Arsenal" تصبح "ARSENAL")
//        val cleanAnswer = answer.replace(" ", "").uppercase().trim()
//
//        // تحويل الكلمة الناتجة إلى قائمة حروف (ستحتوي على A, R, S, E, N, A, L كاملة)
//        val lettersList = cleanAnswer.toMutableList()
//
//        // 2. الحروف الإنجليزية الجاهزة للاستخدام كمخزون عشوائي
//        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
//
//        // 3. ملء اللوحة بحروف عشوائية مختلفة حتى نصل إلى 18 حرفاً تماماً
//        while (lettersList.size < 18) {
//            val randomChar = alphabet.random()
//
//            // لمنع ملء اللوحة بحرف واحد (مثل A)، نتحقق من أن الحرف العشوائي المختار
//            // لم يتكرر بشكل مفرط في القائمة لتوفير تنوع حقيقي للمستخدم
//            if (lettersList.count { it == randomChar } < 2) {
//                lettersList.add(randomChar)
//            }
//        }
//
//        // 4. خلط جميع الحروف (حروف الكلية + الحروف العشوائية) بشكل عشوائي تماماً
//        return lettersList.shuffled()
//    }
//
//    private inline fun handleHintUsage(onHintUnlocked: () -> Unit) {
//        if (hintViewModel.currentHints.value > 0) {
//            hintViewModel.useHint()
//            onHintUnlocked()
//        } else {
//            Toast.makeText(this, "لا يوجد رصيد مساعدات كافٍ!", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun showCompletedLayout(logo: LogoEntity) {
//        binding.leftHints.visibility = View.GONE
//        binding.rightHints.visibility = View.GONE
//        binding.ballsGrid.visibility = View.GONE
//
//        binding.completedLayout.visibility = View.VISIBLE
//        binding.loName.text = logo.lo_name
//        binding.points.text = "${logo.lo_points} Pt"
//    }
//
//
//}