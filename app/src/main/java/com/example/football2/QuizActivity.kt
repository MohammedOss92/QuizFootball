package com.example.football2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.football2.R
import com.example.football2.databinding.ActivityQuizBinding
import com.example.football2.adabter.LettersAdapter
import com.example.football2.db.AppDatabase
import com.example.football2.entity.LogoEntity
import com.example.football2.repository.GameControlRepository
import com.example.football2.repository.HintRepository
import com.example.football2.repository.LogoHintRepository
import com.example.football2.repository.LogoRepository
import com.example.football2.viewModels.HintViewModel
import com.example.football2.viewModels.LogoHintViewModel
import com.example.football2.viewModels.LogoViewModel
import com.example.football2.viewModels.ViewModelFactory

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
    private var isWhistlePlayedForCurrentLogo = false

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

                        // تشغيل أنيميشن الصافرة والصوت أولاً إذا لم يتم تشغيلهما لهذه المرحلة
                        if (!isWhistlePlayedForCurrentLogo) {
                            playWhistleAnimationAndStartGame(logo)
                        }
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

    // 🟢 دالة تشغيل أنيميشن الصفارة والصوت عند بداية السؤال
    private fun playWhistleAnimationAndStartGame(logo: LogoEntity) {
        isWhistlePlayedForCurrentLogo = true

        // إخفاء عناصر اللعب أثناء الأنيميشن
        binding.leftHints.visibility = View.INVISIBLE
        binding.rightHints.visibility = View.INVISIBLE
        binding.ballsGrid.visibility = View.INVISIBLE

        // 1. تشغيل صوت الصفارة
        try {
            val mediaPlayer = android.media.MediaPlayer.create(this, R.raw.whistle)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. إظهار الصفارة وتشغيل أنيميشن الهز (shake_whistle)
        binding.whistle.visibility = View.VISIBLE
        val animShakeWhistle = AnimationUtils.loadAnimation(applicationContext, R.anim.shake_whistle)
        binding.whistle.startAnimation(animShakeWhistle)

        // 3. عند انتهاء الأنيميشن -> إخفاء الصفارة وإظهار لوحة اللعب والتفاعل
        animShakeWhistle.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                binding.whistle.visibility = View.GONE

                setupKeyboard(logo.lo_name ?: "")

                applyHideHintIfUnlocked()
                applyRevealedLettersIfUnlocked()

                binding.leftHints.visibility = View.VISIBLE
                binding.rightHints.visibility = View.VISIBLE
                binding.ballsGrid.visibility = View.VISIBLE
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
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
            val currentHintState = logoHintViewModel.currentLogoHintState.value
            val isInfoUnlocked = currentHintState?.info == 1
            val infoMessage = currentLogo?.lo_info ?: "لا توجد معلومات متاحة لهذا النادي"

            if (isInfoUnlocked) {
                showBlackCustomDialog(infoMessage, R.drawable.wikipedia_pressed)
            } else {
                val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                builder.setTitle("Hints")
                builder.setMessage("Show a clue sentence of the answer!\nCost : 1 hint")

                builder.setPositiveButton("OK") { dialog, _ ->
                    handleHintUsage {
                        logoHintViewModel.unlockInfoHint(currentLogoId)
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

    private fun addLette2rToAnswer(gridPosition: Int, letter: Char) {
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

    private fun addLetterToAnswer(gridPosition: Int, letter: Char) {
        if (isSelectingSlotForLetterHint) {
            clearQuestionMarks()
            isSelectingSlotForLetterHint = false
        }

        for (i in answerSlots.indices) {
            val tvSlot = answerSlots[i]
            if (tvSlot != null && tvSlot.text.isEmpty()) {

                // 🔊 تشغيل صوت الكيك عند اختيار الحرف
                try {
                    val mediaPlayer = android.media.MediaPlayer.create(this, R.raw.kick)
                    mediaPlayer?.start()
                    mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

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

            try {
                val mediaPlayer = android.media.MediaPlayer.create(this, R.raw.space)
                mediaPlayer?.start()
                mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
            } catch (e: Exception) {
                e.printStackTrace()
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
                // 🔊 1. تشغيل صوت الإجابة الصحيحة (right_crowd)
                try {
                    val mediaPlayer = android.media.MediaPlayer.create(this, R.raw.right_crowd)
                    mediaPlayer?.start()
                    mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                hintViewModel.rewardHints(2)
                Toast.makeText(applicationContext, "+2 Hints!", Toast.LENGTH_SHORT).show()
                logoHintViewModel.submitCorrectAnswer(currentLogoId, 100, currentLevelId)

                binding.root.postDelayed({
                    currentLogo?.let { showCompletedLayout(it) }
                }, 300)

            } else {
                // 📉 خصم تلميح عند الإجابة الخاطئة
                if (hintViewModel.currentHints.value > 0) {
                    hintViewModel.useHint()
                    Toast.makeText(applicationContext, "-1 Hint!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "إجابة خاطئة!", Toast.LENGTH_SHORT).show()
                }

                // 🔊 2. تشغيل صوت الإجابة الخاطئة (wrong_crowd)
                try {
                    val mediaPlayer = android.media.MediaPlayer.create(this, R.raw.wrong_crowd)
                    mediaPlayer?.start()
                    mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                binding.wrong.visibility = View.VISIBLE

                // 🔄 إخفاء واجهة الخطأ وإعادة الأحرف الخاطئة إلى الشبكة بعد 1.5 ثانية
                binding.root.postDelayed({
                    binding.wrong.visibility = View.GONE

                    for (i in answerSlots.indices) {
                        removeLetterFromAnswer(i)
                    }
                }, 1500)
            }
        }
    }
    private fun ch2eckAnswerComplete() {
        val currentEnteredAnswer = answerSlots.map { it?.text ?: "" }.joinToString("").trim()
        val realAnswer = currentLogo?.lo_name?.replace(" ", "")?.trim() ?: ""

        if (currentEnteredAnswer.length == realAnswer.length) {
            if (currentEnteredAnswer.equals(realAnswer, ignoreCase = true)) {
                // تشغيل صوت الجمهور الصحيح أو النجاح
                try {
                    val mediaPlayer = android.media.MediaPlayer.create(this, R.raw.right_crowd)
                    mediaPlayer?.start()
                    mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                hintViewModel.rewardHints(2)
                Toast.makeText(applicationContext, "+2 Hints!", Toast.LENGTH_SHORT).show()
                logoHintViewModel.submitCorrectAnswer(currentLogoId, 100, currentLevelId)

                binding.root.postDelayed({
                    currentLogo?.let { showCompletedLayout(it) }
                }, 300)

            }
            else {
                // 🔴 التعديل هنا: خصم تلميح عند الإجابة الخاطئة
                if (hintViewModel.currentHints.value > 0) {
                    hintViewModel.useHint() // خصم تلميح واحد
                    Toast.makeText(applicationContext, "-1 Hint!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "إجابة خاطئة! لا يوجد رصيد تلميحات للخصم", Toast.LENGTH_SHORT).show()
                }

                // تشغيل صوت الإجابة الخاطئة
                try {
                    val mediaPlayer = android.media.MediaPlayer.create(this, R.raw.wrong_crowd)
                    mediaPlayer?.start()
                    mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

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
        isWhistlePlayedForCurrentLogo = false // إعادة ضبط متغير الأنيميشن للشعار الجديد

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
                playWhistleAnimationAndStartGame(logo)
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

                        val existingSourcePos = slotSourcePositions[i]
                        if (existingSourcePos != null) {
                            lettersAdapter.showLetter(existingSourcePos)
                            slotSourcePositions.remove(i)
                        }

                        tvSlot.text = correctChar.toString()
                        tvSlot.setTextColor(android.graphics.Color.YELLOW)

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

            findAvailablePositionOfLetter(correctChar)?.let { gridPosition ->
                slotSourcePositions[slotIndex] = gridPosition
                lettersAdapter.hideLetter(gridPosition)
            }

            updateLetterButtonState(true)

            logoHintViewModel.unlockLetterHintAt(currentLogoId, slotIndex)
            checkAnswerComplete()
        }
    }

    private fun findAvailablePositionOfLetter(targetChar: Char): Int? {
        if (!::lettersAdapter.isInitialized) return null
        val usedPositions = slotSourcePositions.values.toSet()

        for (i in 0 until lettersAdapter.getItemCountSize()) {
            val charAtPos = lettersAdapter.getLetterAt(i)
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
//import com.example.football2.R
//import com.example.football2.databinding.ActivityQuizBinding
//import com.example.football2.adabter.LettersAdapter
//import com.example.football2.db.AppDatabase
//import com.example.football2.entity.LogoEntity
//import com.example.football2.repository.GameControlRepository
//import com.example.football2.repository.HintRepository
//import com.example.football2.repository.LogoHintRepository
//import com.example.football2.repository.LogoRepository
//import com.example.football2.viewModels.HintViewModel
//import com.example.football2.viewModels.LogoHintViewModel
//import com.example.football2.viewModels.LogoViewModel
//import com.example.football2.viewModels.ViewModelFactory
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
//        logoHintViewModel.loadHintStateForLogo(currentLogoId)
//    }
//
//    private fun observeGameStates() {
//        lifecycleScope.launch {
//            hintViewModel.currentHints.collect { hintsCount ->
//                updateHeaderHintCounter(hintsCount)
//            }
//        }
//
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
//                        setupKeyboard(logo.lo_name ?: "")
//
//                        applyHideHintIfUnlocked()
//                        applyRevealedLettersIfUnlocked()
//                    }
//                }
//            }
//        }
//
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
//                    val letterMask = hintEntity.letter ?: 0
//                    if (letterMask > 0) {
//                        updateLetterButtonState(true)
//                    } else {
//                        updateLetterButtonState(false)
//                    }
//
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
//            val currentHintState = logoHintViewModel.currentLogoHintState.value
//            val isInfoUnlocked = currentHintState?.info == 1
//            val infoMessage = currentLogo?.lo_info ?: "لا توجد معلومات متاحة لهذا النادي"
//
//            // 🟢 الحالة الأولى: التلميح مفتوح مسبقاً لهذا الشعار -> عرض السؤال مباشرة بدون خصم
//            if (isInfoUnlocked) {
//                showBlackCustomDialog(infoMessage, R.drawable.wikipedia_pressed)
//            }
//            // 🔴 الحالة الثانية: التلميح غير مفتوح -> طلب التأكيد والخصم لأول مرة فقط
//            else {
//                val builder = androidx.appcompat.app.AlertDialog.Builder(this)
//                builder.setTitle("Hints")
//                builder.setMessage("Show a clue sentence of the answer!\nCost : 1 hint")
//
//                builder.setPositiveButton("OK") { dialog, _ ->
//                    handleHintUsage {
//                        // 1. فتح التلميح وحفظه في DB عبر الـ ViewModel
//                        logoHintViewModel.unlockInfoHint(currentLogoId)
//
//                        // 2. إظهار الدايلوج بالتلميح
//                        showBlackCustomDialog(infoMessage, R.drawable.wikipedia_pressed)
//                    }
//                    dialog.dismiss()
//                }
//                builder.setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }
//
//                val dialog = builder.create()
//                dialog.show()
//                dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
//                dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
//            }
//        }
//
////        binding.info.setOnClickListener {
////            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
////            builder.setTitle("Hints")
////            builder.setMessage("Show a clue sentence of the answer!\nCost : 1 hint")
////
////            builder.setPositiveButton("OK") { dialog, _ ->
////                handleHintUsage {
////                    val infoMessage = currentLogo?.lo_info ?: "لا توجد معلومات متاحة لهذا النادي"
////                    showBlackCustomDialog(infoMessage, R.drawable.wikipedia_pressed)
////                }
////                dialog.dismiss()
////            }
////            builder.setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }
////
////            val dialog = builder.create()
////            dialog.show()
////            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
////            dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
////        }
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
//    private fun che2ckAnswerComplete() {
//        val currentEnteredAnswer = answerSlots.map { it?.text ?: "" }.joinToString("").trim()
//        val realAnswer = currentLogo?.lo_name?.replace(" ", "")?.trim() ?: ""
//
//        if (currentEnteredAnswer.length == realAnswer.length) {
//            if (currentEnteredAnswer.equals(realAnswer, ignoreCase = true)) {
//
//                binding.whistle.visibility = View.VISIBLE
//
//                hintViewModel.rewardHints(2)
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
//    private fun checkAnswerComplete() {
//        val currentEnteredAnswer = answerSlots.map { it?.text ?: "" }.joinToString("").trim()
//        val realAnswer = currentLogo?.lo_name?.replace(" ", "")?.trim() ?: ""
//
//        if (currentEnteredAnswer.length == realAnswer.length) {
//            if (currentEnteredAnswer.equals(realAnswer, ignoreCase = true)) {
//
//                // 1. تشغيل صوت الصفارة
//                val mediaPlayer = android.media.MediaPlayer.create(this, R.raw.whistle)
//                mediaPlayer?.start()
//                mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
//
//                // 2. تحميل تشغيل أنيميشين هزّ الصفارة (shake_whistle)
//                val animShakeWhistle = android.view.animation.AnimationUtils.loadAnimation(
//                    applicationContext,
//                    R.anim.shake_whistle
//                )
//
//                binding.whistle.visibility = View.VISIBLE
//                binding.whistle.startAnimation(animShakeWhistle)
//
//                // 3. الاستماع لانتهاء الأنيميشين لإخفاء الصفارة وعرض الواجهة المكتملة
//                animShakeWhistle.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
//                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}
//
//                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
//                        binding.whistle.visibility = View.GONE
//
//                        // تحديث النقاط وتحديث DB
//                        hintViewModel.rewardHints(2)
//                        Toast.makeText(applicationContext, "+1 Hint!", Toast.LENGTH_SHORT).show()
//                        logoHintViewModel.submitCorrectAnswer(currentLogoId, 100, currentLevelId)
//
//                        // عرض واجهة الإكمال بعد الانتهاء
//                        currentLogo?.let { showCompletedLayout(it) }
//                    }
//
//                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
//                })
//
//            } else {
//                binding.wrong.visibility = View.VISIBLE
//                binding.root.postDelayed({ binding.wrong.visibility = View.GONE }, 1500)
//            }
//        }
//    }
//
//
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
//            binding.letter.alpha = 0.5f
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
//        updateHideButtonState(false)
//        updateLetterButtonState(false)
//
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
//                val isRevealed = (letterMask and (1 shl i)) != 0
//                if (isRevealed) {
//                    val tvSlot = answerSlots.getOrNull(i)
//                    if (tvSlot != null && tvSlot.currentTextColor != android.graphics.Color.YELLOW) {
//                        val correctChar = correctAnswer[i].uppercaseChar()
//
//                        // إرجاع الحرف العادي السابق للكيبورد
//                        val existingSourcePos = slotSourcePositions[i]
//                        if (existingSourcePos != null) {
//                            lettersAdapter.showLetter(existingSourcePos)
//                            slotSourcePositions.remove(i)
//                        }
//
//                        // تعيين الحرف باللون الأصفر
//                        tvSlot.text = correctChar.toString()
//                        tvSlot.setTextColor(android.graphics.Color.YELLOW)
//
//                        // 🟢 البحث عن حرف واحد فقط مرئي (غير مخفي) في الكيبورد لتجنب إخفاء الحروف المتشابهة
//                        findAvailablePositionOfLetter(correctChar)?.let { gridPosition ->
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
//            // 🟢 البحث عن موقع أول حرف غير مخفي إطلاقاً بدلاً من إخفاء المتشابهات
//            findAvailablePositionOfLetter(correctChar)?.let { gridPosition ->
//                slotSourcePositions[slotIndex] = gridPosition
//                lettersAdapter.hideLetter(gridPosition)
//            }
//
//            // تعطيل زر A فوراً
//            updateLetterButtonState(true)
//
//            logoHintViewModel.unlockLetterHintAt(currentLogoId, slotIndex)
//            checkAnswerComplete()
//        }
//    }
//
//    // 🟢 دالة مساعدة تجد الحرف الشاغل المتاح في الكيبورد
//    private fun findAvailablePositionOfLetter(targetChar: Char): Int? {
//        if (!::lettersAdapter.isInitialized) return null
//        val usedPositions = slotSourcePositions.values.toSet()
//
//        for (i in 0 until lettersAdapter.getItemCountSize()) {
//            val charAtPos = lettersAdapter.getLetterAt(i) // تأكد من وجود هذه الدالة في LettersAdapter أو استبدالها ببديل القائمة
//            if (charAtPos == targetChar && !usedPositions.contains(i)) {
//                return i
//            }
//        }
//        return lettersAdapter.findPositionOfLetter(targetChar).takeIf { it != -1 }
//    }
//
//    private fun clearQuestionMarks() {
//        for (slot in answerSlots) {
//            if (slot != null && slot.currentTextColor != android.graphics.Color.YELLOW) {
//                if (slot.text.toString().trim() == "?") {
//                    slot.text = ""
//                }
//            }
//        }
//    }
//}
