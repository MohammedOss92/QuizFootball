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

    private lateinit var lettersAdapter: LettersAdapter
    private val answerSlots = ArrayList<TextView?>()
    private val slotSourcePositions = HashMap<Int, Int>()

    private var isSelectingSlotForLetterHint = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentLogoId = intent.getIntExtra("LOGO_ID", 0)
        currentLevelId = intent.getIntExtra("LEVEL_ID", 0)

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
        hintViewModel = ViewModelProvider(this, factory)[HintViewModel::class.java]
        logoHintViewModel = ViewModelProvider(this, factory)[LogoHintViewModel::class.java]

        logoViewModel.loadLogosForLevel(currentLevelId)
        hintViewModel.loadCurrentHints()

        observeGameStates()
        setupActions()

        logoHintViewModel.loadHintStateForLogo(currentLogoId)
    }

    private fun observeGameStates() {
        lifecycleScope.launch {
            hintViewModel.currentHints.collect { hintsCount ->
                updateHeaderHintCounter(hintsCount)
            }
        }

        lifecycleScope.launch {
            logoViewModel.logos.collect { logosList ->
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

                        applyHideHintIfUnlocked()
                        applyRevealedLettersIfUnlocked()
                    }
                }
            }
        }

        lifecycleScope.launch {
            logoHintViewModel.currentLogoHintState.collect { hintEntity ->
                if (hintEntity != null) {
                    if (hintEntity.hide == 1) {
                        updateHideButtonState(true)
                        applyHideHintIfUnlocked()
                    } else {
                        updateHideButtonState(false)
                    }

                    val letterMask = hintEntity.letter ?: 0
                    if (letterMask > 0) {
                        updateLetterButtonState(true)
                    } else {
                        updateLetterButtonState(false)
                    }

                    applyRevealedLettersIfUnlocked()
                }
            }
        }
    }

    private fun applyHideHintIfUnlocked() {
        val hintState = logoHintViewModel.currentLogoHintState.value
        if (hintState?.hide == 1 && ::lettersAdapter.isInitialized) {
            val correctAnswer = currentLogo?.lo_name
            if (!correctAnswer.isNullOrEmpty()) {
                lettersAdapter.removeWrongLetters(correctAnswer)
            }
        }
    }

    private fun updateHeaderHintCounter(hintsCount: Int) {
        val tvCounterValue = findViewById<TextView>(R.id.tvCounterValue) ?: findViewById<TextView>(R.id.scoreValue)
        val tvCounterLabel = findViewById<TextView>(R.id.tvCounterLabel) ?: findViewById<TextView>(R.id.scoreTitle)

        tvCounterValue?.text = hintsCount.toString()
        tvCounterValue?.setTextColor(android.graphics.Color.parseColor("#7CB342"))
        tvCounterLabel?.text = "HINTS"
        tvCounterLabel?.setTextColor(android.graphics.Color.parseColor("#7CB342"))
    }

    private fun setupActions() {
        val btnBack = findViewById<View>(R.id.btnBack)
        btnBack?.setOnClickListener { finish() }

        binding.facebook.setOnClickListener {
            handleHintUsage {
                logoHintViewModel.unlockFacebookHint(currentLogoId)
                Toast.makeText(this, "تم فتح تلميح فيسبوك", Toast.LENGTH_SHORT).show()
            }
        }

        binding.info.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Hints")
            builder.setMessage("Show a clue sentence of the answer!\nCost : 1 hint")

            builder.setPositiveButton("OK") { dialog, _ ->
                handleHintUsage {
                    val infoMessage = currentLogo?.lo_info ?: "لا توجد معلومات متاحة لهذا النادي"
                    showBlackCustomDialog(infoMessage, R.drawable.wikipedia_pressed)
                }
                dialog.dismiss()
            }
            builder.setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }

            val dialog = builder.create()
            dialog.show()
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
            dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
        }

        binding.okInfo.setOnClickListener { binding.infoPopup.visibility = View.GONE }

        binding.player.setOnClickListener {
            handleHintUsage {
                binding.playerName.text = "معلومات إضافية متوفرة"
                binding.playerPopup.visibility = View.VISIBLE
            }
        }

        binding.okPlayer.setOnClickListener { binding.playerPopup.visibility = View.GONE }

        binding.nextLogoButton.setOnClickListener { navigateToNextLogo() }
        binding.prevLogoButton.setOnClickListener { navigateToPrevLogo() }

        // زر المساعدة A
        binding.letter.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Hints")
            builder.setMessage("Show one letter!\nCost : 1 hint")

            builder.setPositiveButton("OK") { dialog, _ ->
                handleHintUsage {
                    isSelectingSlotForLetterHint = true
                    showQuestionMarksForHint()
                }
                dialog.dismiss()
            }

            builder.setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }

            val dialog = builder.create()
            dialog.show()
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
            dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
        }

        // ضغط زر القنبلة
        binding.hide.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Hints")
            builder.setMessage("Remove the wrong letters!\nCost : 1 hint")

            builder.setPositiveButton("OK") { dialog, _ ->
                handleHintUsage {
                    val correctAnswer = currentLogo?.lo_name
                    if (!correctAnswer.isNullOrEmpty()) {
                        if (::lettersAdapter.isInitialized) {
                            lettersAdapter.removeWrongLetters(correctAnswer)
                        }
                        updateHideButtonState(true)
                        logoHintViewModel.unlockHideHint(currentLogoId)
                    }
                }
                dialog.dismiss()
            }

            builder.setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }

            val dialog = builder.create()
            dialog.show()
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

        btnOk.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.show()
    }

    private fun setupKeyboard(correctAnswer: String) {
        val shuffledLetters = generateShuffledLetters(correctAnswer)

        lettersAdapter = LettersAdapter(this, shuffledLetters) { position, letter ->
            addLetterToAnswer(position, letter)
        }
        binding.ballsGrid.adapter = lettersAdapter

        binding.spacesGrid1.removeAllViews()
        binding.spacesGrid2.removeAllViews()
        answerSlots.clear()
        slotSourcePositions.clear()

        for (i in correctAnswer.indices) {
            if (correctAnswer[i] == ' ') {
                val spaceView = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(24, 10)
                }
                binding.spacesGrid1.addView(spaceView)
                answerSlots.add(null)
            } else {
                val slotView = LayoutInflater.from(this).inflate(R.layout.item_letter_ball, binding.spacesGrid1, false) as FrameLayout
                val tvSlot = slotView.findViewById<TextView>(R.id.tvLetter)

                tvSlot.text = ""
                tvSlot.setBackgroundResource(R.drawable.hint_background)

                val slotIndex = i
                slotView.setOnClickListener {
                    if (isSelectingSlotForLetterHint) {
                        revealLetterAtSlot(slotIndex)
                    } else {
                        removeLetterFromAnswer(slotIndex)
                    }
                }

                if (i < 8) {
                    binding.spacesGrid1.addView(slotView)
                } else {
                    binding.spacesGrid2.addView(slotView)
                }

                answerSlots.add(tvSlot)
            }
        }
    }

    private fun addLetterToAnswer(gridPosition: Int, letter: Char) {
        if (isSelectingSlotForLetterHint) {
            clearQuestionMarks()
            isSelectingSlotForLetterHint = false
        }

        for (i in answerSlots.indices) {
            val tvSlot = answerSlots[i]
            if (tvSlot != null && tvSlot.text.isEmpty()) {
                tvSlot.text = letter.toString()
                tvSlot.setTextColor(android.graphics.Color.WHITE)
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
            if (tvSlot.currentTextColor == android.graphics.Color.YELLOW) {
                return
            }

            val originalGridPos = slotSourcePositions[slotIndex]
            if (originalGridPos != null) {
                lettersAdapter.showLetter(originalGridPos)
            }
            tvSlot.text = ""
            slotSourcePositions.remove(slotIndex)
        }
    }

    private fun checkAnswerComplete() {
        val currentEnteredAnswer = answerSlots.map { it?.text ?: "" }.joinToString("").trim()
        val realAnswer = currentLogo?.lo_name?.replace(" ", "")?.trim() ?: ""

        if (currentEnteredAnswer.length == realAnswer.length) {
            if (currentEnteredAnswer.equals(realAnswer, ignoreCase = true)) {

                binding.whistle.visibility = View.VISIBLE

                hintViewModel.rewardHints(1)
                Toast.makeText(applicationContext, "+1 Hint!", Toast.LENGTH_SHORT).show()

                logoHintViewModel.submitCorrectAnswer(currentLogoId, 100, currentLevelId)

                binding.root.postDelayed({
                    showCompletedLayout(currentLogo!!)
                }, 300)

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
                updateActivityForNewLogo(nextLogo._loid ?: 0)
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
                updateActivityForNewLogo(prevLogo._loid ?: 0)
            } else {
                Toast.makeText(this, "هذا هو الشعار الأول في المستوى!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateHideButtonState(isUsed: Boolean) {
        if (isUsed) {
            binding.hide.isEnabled = false
            binding.hide.alpha = 0.5f
        } else {
            binding.hide.isEnabled = true
            binding.hide.alpha = 1.0f
        }
    }

    private fun updateLetterButtonState(isUsed: Boolean) {
        if (isUsed) {
            binding.letter.isEnabled = false
            binding.letter.alpha = 0.5f
        } else {
            binding.letter.isEnabled = true
            binding.letter.alpha = 1.0f
        }
    }

    private fun updateActivityForNewLogo(newLogoId: Int) {
        currentLogoId = newLogoId
        isSelectingSlotForLetterHint = false

        binding.whistle.visibility = View.GONE
        binding.wrong.visibility = View.GONE
        binding.infoPopup.visibility = View.GONE
        binding.playerPopup.visibility = View.GONE
        binding.infoText.text = ""
        binding.playerName.text = ""

        updateHideButtonState(false)
        updateLetterButtonState(false)

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

        logoHintViewModel.loadHintStateForLogo(currentLogoId)
    }

    private fun showQuestionMarksForHint() {
        for (slot in answerSlots) {
            if (slot != null && slot.currentTextColor != android.graphics.Color.YELLOW) {
                slot.text = "?"
                slot.setTextColor(android.graphics.Color.WHITE)
            }
        }
    }

    private fun applyRevealedLettersIfUnlocked() {
        val hintState = logoHintViewModel.currentLogoHintState.value ?: return
        val letterMask = hintState.letter ?: 0
        val correctAnswer = currentLogo?.lo_name ?: return

        if (letterMask > 0 && ::lettersAdapter.isInitialized && answerSlots.isNotEmpty()) {
            for (i in correctAnswer.indices) {
                if (correctAnswer[i] == ' ') continue

                val isRevealed = (letterMask and (1 shl i)) != 0
                if (isRevealed) {
                    val tvSlot = answerSlots.getOrNull(i)
                    if (tvSlot != null && tvSlot.currentTextColor != android.graphics.Color.YELLOW) {
                        val correctChar = correctAnswer[i].uppercaseChar()

                        // إرجاع الحرف العادي السابق للكيبورد
                        val existingSourcePos = slotSourcePositions[i]
                        if (existingSourcePos != null) {
                            lettersAdapter.showLetter(existingSourcePos)
                            slotSourcePositions.remove(i)
                        }

                        // تعيين الحرف باللون الأصفر
                        tvSlot.text = correctChar.toString()
                        tvSlot.setTextColor(android.graphics.Color.YELLOW)

                        // 🟢 البحث عن حرف واحد فقط مرئي (غير مخفي) في الكيبورد لتجنب إخفاء الحروف المتشابهة
                        findAvailablePositionOfLetter(correctChar)?.let { gridPosition ->
                            slotSourcePositions[i] = gridPosition
                            lettersAdapter.hideLetter(gridPosition)
                        }
                    }
                }
            }
        }
    }

    private fun revealLetterAtSlot(slotIndex: Int) {
        isSelectingSlotForLetterHint = false

        val correctAnswer = currentLogo?.lo_name ?: return
        val selectedSlot = answerSlots.getOrNull(slotIndex) ?: return

        clearQuestionMarks()

        val isTargetSlot = selectedSlot.text.toString().trim() == "?" || selectedSlot.text.isEmpty()

        if (isTargetSlot) {
            val correctChar = correctAnswer[slotIndex].uppercaseChar()

            val existingSourcePos = slotSourcePositions[slotIndex]
            if (existingSourcePos != null) {
                lettersAdapter.showLetter(existingSourcePos)
                slotSourcePositions.remove(slotIndex)
            }

            selectedSlot.text = correctChar.toString()
            selectedSlot.setTextColor(android.graphics.Color.YELLOW)

            // 🟢 البحث عن موقع أول حرف غير مخفي إطلاقاً بدلاً من إخفاء المتشابهات
            findAvailablePositionOfLetter(correctChar)?.let { gridPosition ->
                slotSourcePositions[slotIndex] = gridPosition
                lettersAdapter.hideLetter(gridPosition)
            }

            // تعطيل زر A فوراً
            updateLetterButtonState(true)

            logoHintViewModel.unlockLetterHintAt(currentLogoId, slotIndex)
            checkAnswerComplete()
        }
    }

    // 🟢 دالة مساعدة تجد الحرف الشاغل المتاح في الكيبورد
    private fun findAvailablePositionOfLetter(targetChar: Char): Int? {
        if (!::lettersAdapter.isInitialized) return null
        val usedPositions = slotSourcePositions.values.toSet()

        for (i in 0 until lettersAdapter.getItemCountSize()) {
            val charAtPos = lettersAdapter.getLetterAt(i) // تأكد من وجود هذه الدالة في LettersAdapter أو استبدالها ببديل القائمة
            if (charAtPos == targetChar && !usedPositions.contains(i)) {
                return i
            }
        }
        return lettersAdapter.findPositionOfLetter(targetChar).takeIf { it != -1 }
    }

    private fun clearQuestionMarks() {
        for (slot in answerSlots) {
            if (slot != null && slot.currentTextColor != android.graphics.Color.YELLOW) {
                if (slot.text.toString().trim() == "?") {
                    slot.text = ""
                }
            }
        }
    }
}
//
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
//    private lateinit var lettersAdapter: LettersAdapter
//    private val answerSlots = ArrayList<TextView?>()
//    private val slotSourcePositions = HashMap<Int, Int>()
//
//    private var isSelectingSlotForLetterHint = false
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityQuizBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        currentLogoId = intent.getIntExtra("LOGO_ID", 0)
//        currentLevelId = intent.getIntExtra("LEVEL_ID", 0)
//
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
//        logoViewModel.loadLogosForLevel(currentLevelId)
//        hintViewModel.loadCurrentHints()
//
//        observeGameStates()
//        setupActions()
//
//        // 🟢 جلب حالة التلميحات للشعار عند البداية
//        logoHintViewModel.loadHintStateForLogo(currentLogoId)
//    }
//
//    private fun observeGameStates() {
//        // 1. مراقبة عداد المساعدات
//        lifecycleScope.launch {
//            hintViewModel.currentHints.collect { hintsCount ->
//                updateHeaderHintCounter(hintsCount)
//            }
//        }
//
//        // 2. مراقبة قائمة الشعارات ورسم الكيبورد
//        lifecycleScope.launch {
//            logoViewModel.logos.collect { logosList ->
//                currentLogo = logosList.find { it._loid == currentLogoId }
//                currentLogo?.let { logo ->
//                    val resId = resources.getIdentifier(logo.lo_image, "drawable", packageName)
//                    if (resId != 0) binding.logo.setImageResource(resId)
//
//                    if (logo.lo_completed == "1") {
//                        showCompletedLayout(logo)
//                    } else {
//                        binding.completedLayout.visibility = View.GONE
//                        binding.leftHints.visibility = View.VISIBLE
//                        binding.rightHints.visibility = View.VISIBLE
//                        binding.ballsGrid.visibility = View.VISIBLE
//
//                        // رسم الكيبورد والمربعات
//                        setupKeyboard(logo.lo_name ?: "")
//
//                        // 🟢 تطبيق التلميحات المحفوظة فور تجهيز الكيبورد
//                        applyHideHintIfUnlocked()
//                        applyRevealedLettersIfUnlocked()
//                    }
//                }
//            }
//        }
//
//        // 3. مراقبة حالة تلميح الشعار (عند تحميلها من Room DB)
//        lifecycleScope.launch {
//            logoHintViewModel.currentLogoHintState.collect { hintEntity ->
//                if (hintEntity != null) {
//                    if (hintEntity.hide == 1) {
//                        updateHideButtonState(true)
//                        applyHideHintIfUnlocked()
//                    } else {
//                        updateHideButtonState(false)
//                    }
//
//                    // 🟢 استرجاع الحروف عند كل تحديث للحالة من DB
//                    applyRevealedLettersIfUnlocked()
//                }
//            }
//        }
//
//        // 3. مراقبة حالة تلميح الشعار (عند تحميلها من Room DB)
//        lifecycleScope.launch {
//            logoHintViewModel.currentLogoHintState.collect { hintEntity ->
//                if (hintEntity != null) {
//                    // تحديث زر القنبلة
//                    if (hintEntity.hide == 1) {
//                        updateHideButtonState(true)
//                        applyHideHintIfUnlocked()
//                    } else {
//                        updateHideButtonState(false)
//                    }
//
//                    // 🟢 تحديث حالة زر الحرف A (إذا كان هناك حرف مكشوف مسبقاً تعطيل الزر)
//                    val letterMask = hintEntity.letter ?: 0
//                    if (letterMask > 0) {
//                        updateLetterButtonState(true)
//                    } else {
//                        updateLetterButtonState(false)
//                    }
//
//                    // استرجاع الحروف عند كل تحديث للحالة من DB
//                    applyRevealedLettersIfUnlocked()
//                }
//            }
//        }
//    }
//
//    private fun applyHideHintIfUnlocked() {
//        val hintState = logoHintViewModel.currentLogoHintState.value
//        if (hintState?.hide == 1 && ::lettersAdapter.isInitialized) {
//            val correctAnswer = currentLogo?.lo_name
//            if (!correctAnswer.isNullOrEmpty()) {
//                lettersAdapter.removeWrongLetters(correctAnswer)
//            }
//        }
//    }
//
//    private fun updateHeaderHintCounter(hintsCount: Int) {
//        val tvCounterValue = findViewById<TextView>(R.id.tvCounterValue) ?: findViewById<TextView>(R.id.scoreValue)
//        val tvCounterLabel = findViewById<TextView>(R.id.tvCounterLabel) ?: findViewById<TextView>(R.id.scoreTitle)
//
//        tvCounterValue?.text = hintsCount.toString()
//        tvCounterValue?.setTextColor(android.graphics.Color.parseColor("#7CB342"))
//        tvCounterLabel?.text = "HINTS"
//        tvCounterLabel?.setTextColor(android.graphics.Color.parseColor("#7CB342"))
//    }
//
//    private fun setupActions() {
//        val btnBack = findViewById<View>(R.id.btnBack)
//        btnBack?.setOnClickListener { finish() }
//
//        binding.facebook.setOnClickListener {
//            handleHintUsage {
//                logoHintViewModel.unlockFacebookHint(currentLogoId)
//                Toast.makeText(this, "تم فتح تلميح فيسبوك", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        binding.info.setOnClickListener {
//            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
//            builder.setTitle("Hints")
//            builder.setMessage("Show a clue sentence of the answer!\nCost : 1 hint")
//
//            builder.setPositiveButton("OK") { dialog, _ ->
//                handleHintUsage {
//                    val infoMessage = currentLogo?.lo_info ?: "لا توجد معلومات متاحة لهذا النادي"
//                    showBlackCustomDialog(infoMessage, R.drawable.wikipedia_pressed)
//                }
//                dialog.dismiss()
//            }
//            builder.setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }
//
//            val dialog = builder.create()
//            dialog.show()
//            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
//            dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
//        }
//
//        binding.okInfo.setOnClickListener { binding.infoPopup.visibility = View.GONE }
//
//        binding.player.setOnClickListener {
//            handleHintUsage {
//                binding.playerName.text = "معلومات إضافية متوفرة"
//                binding.playerPopup.visibility = View.VISIBLE
//            }
//        }
//
//        binding.okPlayer.setOnClickListener { binding.playerPopup.visibility = View.GONE }
//
//        binding.nextLogoButton.setOnClickListener { navigateToNextLogo() }
//        binding.prevLogoButton.setOnClickListener { navigateToPrevLogo() }
//
//        // زر المساعدة A
//        binding.letter.setOnClickListener {
//            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
//            builder.setTitle("Hints")
//            builder.setMessage("Show one letter!\nCost : 1 hint")
//
//            builder.setPositiveButton("OK") { dialog, _ ->
//                handleHintUsage {
//                    isSelectingSlotForLetterHint = true
//                    showQuestionMarksForHint()
//                }
//                dialog.dismiss()
//            }
//
//            builder.setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }
//
//            val dialog = builder.create()
//            dialog.show()
//            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
//            dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
//        }
//
//        // ضغط زر القنبلة
//        binding.hide.setOnClickListener {
//            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
//            builder.setTitle("Hints")
//            builder.setMessage("Remove the wrong letters!\nCost : 1 hint")
//
//            builder.setPositiveButton("OK") { dialog, _ ->
//                handleHintUsage {
//                    val correctAnswer = currentLogo?.lo_name
//                    if (!correctAnswer.isNullOrEmpty()) {
//                        if (::lettersAdapter.isInitialized) {
//                            lettersAdapter.removeWrongLetters(correctAnswer)
//                        }
//                        updateHideButtonState(true)
//                        logoHintViewModel.unlockHideHint(currentLogoId)
//                    }
//                }
//                dialog.dismiss()
//            }
//
//            builder.setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }
//
//            val dialog = builder.create()
//            dialog.show()
//            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
//            dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
//        }
//    }
//
//    fun showBlackCustomDialog(message: String, imageResId: Int) {
//        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_hint_info, null)
//        builder.setView(dialogView)
//
//        val dialog = builder.create()
//
//        val ivDialogIcon = dialogView.findViewById<android.widget.ImageView>(R.id.ivDialogIcon)
//        val tvDialogMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
//        val btnOk = dialogView.findViewById<android.widget.Button>(R.id.btnOk)
//
//        ivDialogIcon.setImageResource(imageResId)
//        tvDialogMessage.text = message
//
//        btnOk.setOnClickListener { dialog.dismiss() }
//
//        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
//        dialog.show()
//    }
//
//    private fun setupKeyboard(correctAnswer: String) {
//        val shuffledLetters = generateShuffledLetters(correctAnswer)
//
//        lettersAdapter = LettersAdapter(this, shuffledLetters) { position, letter ->
//            addLetterToAnswer(position, letter)
//        }
//        binding.ballsGrid.adapter = lettersAdapter
//
//        binding.spacesGrid1.removeAllViews()
//        binding.spacesGrid2.removeAllViews()
//        answerSlots.clear()
//        slotSourcePositions.clear()
//
//        for (i in correctAnswer.indices) {
//            if (correctAnswer[i] == ' ') {
//                val spaceView = View(this).apply {
//                    layoutParams = LinearLayout.LayoutParams(24, 10)
//                }
//                binding.spacesGrid1.addView(spaceView)
//                answerSlots.add(null)
//            } else {
//                val slotView = LayoutInflater.from(this).inflate(R.layout.item_letter_ball, binding.spacesGrid1, false) as FrameLayout
//                val tvSlot = slotView.findViewById<TextView>(R.id.tvLetter)
//
//                tvSlot.text = ""
//                tvSlot.setBackgroundResource(R.drawable.hint_background)
//
//                val slotIndex = i
//                slotView.setOnClickListener {
//                    if (isSelectingSlotForLetterHint) {
//                        revealLetterAtSlot(slotIndex)
//                    } else {
//                        removeLetterFromAnswer(slotIndex)
//                    }
//                }
//
//                if (i < 8) {
//                    binding.spacesGrid1.addView(slotView)
//                } else {
//                    binding.spacesGrid2.addView(slotView)
//                }
//
//                answerSlots.add(tvSlot)
//            }
//        }
//    }
//
//    private fun addLetterToAnswer(gridPosition: Int, letter: Char) {
//        if (isSelectingSlotForLetterHint) {
//            clearQuestionMarks()
//            isSelectingSlotForLetterHint = false
//        }
//
//        for (i in answerSlots.indices) {
//            val tvSlot = answerSlots[i]
//            if (tvSlot != null && tvSlot.text.isEmpty()) {
//                tvSlot.text = letter.toString()
//                tvSlot.setTextColor(android.graphics.Color.WHITE)
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
//            // الحروف الصفراء المكشوفة لا تُحذف بالضغط
//            if (tvSlot.currentTextColor == android.graphics.Color.YELLOW) {
//                return
//            }
//
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
//        val currentEnteredAnswer = answerSlots.map { it?.text ?: "" }.joinToString("").trim()
//        val realAnswer = currentLogo?.lo_name?.replace(" ", "")?.trim() ?: ""
//
//        if (currentEnteredAnswer.length == realAnswer.length) {
//            if (currentEnteredAnswer.equals(realAnswer, ignoreCase = true)) {
//
//                binding.whistle.visibility = View.VISIBLE
//
//                hintViewModel.rewardHints(1)
//                Toast.makeText(applicationContext, "+1 Hint!", Toast.LENGTH_SHORT).show()
//
//                logoHintViewModel.submitCorrectAnswer(currentLogoId, 100, currentLevelId)
//
//                binding.root.postDelayed({
//                    showCompletedLayout(currentLogo!!)
//                }, 300)
//
//            } else {
//                binding.wrong.visibility = View.VISIBLE
//                binding.root.postDelayed({ binding.wrong.visibility = View.GONE }, 1500)
//            }
//        }
//    }
//
//    private fun generateShuffledLetters(answer: String): List<Char> {
//        val cleanAnswer = answer.replace(" ", "").uppercase().trim()
//        val lettersList = cleanAnswer.toMutableList()
//        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
//
//        while (lettersList.size < 18) {
//            val randomChar = alphabet.random()
//            if (lettersList.count { it == randomChar } < 2) {
//                lettersList.add(randomChar)
//            }
//        }
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
//    private fun navigateToNextLogo() {
//        val logosList = logoViewModel.logos.value
//        if (logosList.isNotEmpty()) {
//            val currentIndex = logosList.indexOfFirst { it._loid == currentLogoId }
//
//            if (currentIndex != -1 && currentIndex < logosList.size - 1) {
//                val nextLogo = logosList[currentIndex + 1]
//                updateActivityForNewLogo(nextLogo._loid ?: 0)
//            } else {
//                Toast.makeText(this, "لقد وصلت لآخر شعار في هذا المستوى!", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun navigateToPrevLogo() {
//        val logosList = logoViewModel.logos.value
//        if (logosList.isNotEmpty()) {
//            val currentIndex = logosList.indexOfFirst { it._loid == currentLogoId }
//
//            if (currentIndex > 0) {
//                val prevLogo = logosList[currentIndex - 1]
//                updateActivityForNewLogo(prevLogo._loid ?: 0)
//            } else {
//                Toast.makeText(this, "هذا هو الشعار الأول في المستوى!", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun updateHideButtonState(isUsed: Boolean) {
//        if (isUsed) {
//            binding.hide.isEnabled = false
//            binding.hide.alpha = 0.5f
//        } else {
//            binding.hide.isEnabled = true
//            binding.hide.alpha = 1.0f
//        }
//    }
//
//    private fun updateLetterButtonState(isUsed: Boolean) {
//        if (isUsed) {
//            binding.letter.isEnabled = false
//            binding.letter.alpha = 0.5f // جعل الزر باهتاً للدلالة على تعطيله
//        } else {
//            binding.letter.isEnabled = true
//            binding.letter.alpha = 1.0f
//        }
//    }
//
//    private fun updateActivityForNewLogo(newLogoId: Int) {
//        currentLogoId = newLogoId
//        isSelectingSlotForLetterHint = false
//
//        binding.whistle.visibility = View.GONE
//        binding.wrong.visibility = View.GONE
//        binding.infoPopup.visibility = View.GONE
//        binding.playerPopup.visibility = View.GONE
//        binding.infoText.text = ""
//        binding.playerName.text = ""
//
//
//
//        updateHideButtonState(false)
//        updateLetterButtonState(false) // 🟢 إعادة تفعيل زر A عند اختيار شعار جديد
//        val logosList = logoViewModel.logos.value
//        currentLogo = logosList.find { it._loid == currentLogoId }
//
//        currentLogo?.let { logo ->
//            val resId = resources.getIdentifier(logo.lo_image, "drawable", packageName)
//            if (resId != 0) binding.logo.setImageResource(resId)
//
//            if (logo.lo_completed == "1") {
//                showCompletedLayout(logo)
//            } else {
//                binding.completedLayout.visibility = View.GONE
//                binding.leftHints.visibility = View.VISIBLE
//                binding.rightHints.visibility = View.VISIBLE
//                binding.ballsGrid.visibility = View.VISIBLE
//
//                setupKeyboard(logo.lo_name ?: "")
//            }
//        }
//
//        // 🟢 تحميل حالة الـ DB للشعار الجديد لتفعيل التلميحات المحفوظة
//        logoHintViewModel.loadHintStateForLogo(currentLogoId)
//    }
//
//    private fun showQuestionMarksForHint() {
//        for (slot in answerSlots) {
//            if (slot != null && slot.currentTextColor != android.graphics.Color.YELLOW) {
//                slot.text = "?"
//                slot.setTextColor(android.graphics.Color.WHITE)
//            }
//        }
//    }
//
//    private fun applyRevealedLettersIfUnlocked() {
//        val hintState = logoHintViewModel.currentLogoHintState.value ?: return
//        val letterMask = hintState.letter ?: 0
//        val correctAnswer = currentLogo?.lo_name ?: return
//
//        if (letterMask > 0 && ::lettersAdapter.isInitialized && answerSlots.isNotEmpty()) {
//            for (i in correctAnswer.indices) {
//                if (correctAnswer[i] == ' ') continue
//
//                // 🟢 فحص قناع البتات لرؤية الخانة المكشوفة
//                val isRevealed = (letterMask and (1 shl i)) != 0
//                if (isRevealed) {
//                    val tvSlot = answerSlots.getOrNull(i)
//                    if (tvSlot != null) {
//                        val correctChar = correctAnswer[i].uppercaseChar()
//
//                        // إرجاع أي حرف عادي سبق وأدخله المستخدم في المربع
//                        val existingSourcePos = slotSourcePositions[i]
//                        if (existingSourcePos != null && tvSlot.currentTextColor != android.graphics.Color.YELLOW) {
//                            lettersAdapter.showLetter(existingSourcePos)
//                            slotSourcePositions.remove(i)
//                        }
//
//                        // 🟢 تثبيت الحرف باللون الأصفر
//                        tvSlot.text = correctChar.toString()
//                        tvSlot.setTextColor(android.graphics.Color.YELLOW)
//
//                        // إخفاء الحرف المطابق من الكيبورد السفلية
//                        val gridPosition = lettersAdapter.findPositionOfLetter(correctChar)
//                        if (gridPosition != -1) {
//                            slotSourcePositions[i] = gridPosition
//                            lettersAdapter.hideLetter(gridPosition)
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private fun revealLetterAtSlot(slotIndex: Int) {
//        isSelectingSlotForLetterHint = false
//
//        val correctAnswer = currentLogo?.lo_name ?: return
//        val selectedSlot = answerSlots.getOrNull(slotIndex) ?: return
//
//        clearQuestionMarks()
//
//        val isTargetSlot = selectedSlot.text.toString().trim() == "?" || selectedSlot.text.isEmpty()
//
//        if (isTargetSlot) {
//            val correctChar = correctAnswer[slotIndex].uppercaseChar()
//
//            val existingSourcePos = slotSourcePositions[slotIndex]
//            if (existingSourcePos != null) {
//                lettersAdapter.showLetter(existingSourcePos)
//                slotSourcePositions.remove(slotIndex)
//            }
//
//            selectedSlot.text = correctChar.toString()
//            selectedSlot.setTextColor(android.graphics.Color.YELLOW)
//
//            val gridPosition = lettersAdapter.findPositionOfLetter(correctChar)
//            if (gridPosition != -1) {
//                slotSourcePositions[slotIndex] = gridPosition
//                lettersAdapter.hideLetter(gridPosition)
//            }
//
//            // 🟢 تعطيل زر المساعدة A فور كشف الحرف
//            updateLetterButtonState(true)
//
//            logoHintViewModel.unlockLetterHintAt(currentLogoId, slotIndex)
//            checkAnswerComplete()
//        }
//    }
//
//    private fun revealLetterAtSlo1t(slotIndex: Int) {
//        val correctAnswer = currentLogo?.lo_name ?: return
//        val selectedSlot = answerSlots.getOrNull(slotIndex) ?: return
//
//        if (selectedSlot.text == "?") {
//            val correctChar = correctAnswer[slotIndex].uppercaseChar()
//
//            val existingSourcePos = slotSourcePositions[slotIndex]
//            if (existingSourcePos != null) {
//                lettersAdapter.showLetter(existingSourcePos)
//                slotSourcePositions.remove(slotIndex)
//            }
//
//            selectedSlot.text = correctChar.toString()
//            selectedSlot.setTextColor(android.graphics.Color.YELLOW)
//
//            val gridPosition = lettersAdapter.findPositionOfLetter(correctChar)
//            if (gridPosition != -1) {
//                slotSourcePositions[slotIndex] = gridPosition
//                lettersAdapter.hideLetter(gridPosition)
//            }
//
//            clearQuestionMarks()
//            isSelectingSlotForLetterHint = false
//
//            // 🟢 الحفظ المباشر بالـ Bitmask في Room DB
//            logoHintViewModel.unlockLetterHintAt(currentLogoId, slotIndex)
//
//            checkAnswerComplete()
//        }
//    }
//
//    private fun clearQuestionMarks() {
//        for (slot in answerSlots) {
//            if (slot != null && slot.text == "?") {
//                slot.text = ""
//            }
//        }
//    }
//}
