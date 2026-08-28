package com.example.football2

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.football2.adabter.LettersAdapter
import com.example.football2.databinding.ActivityQuizBinding
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

    // 🟢 فصل متغيّري وضع الاختيار للتلميحين
    private var isSelectingSlotForLetterHint = false
    private var isSelectingSlotForLetter2Hint = false

    private var isWhistlePlayedForCurrentLogo = false

    private var currentLogosList: List<LogoEntity> = emptyList()

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
//
//                        if (!isWhistlePlayedForCurrentLogo) {
//                            playWhistleAnimationAndStartGame(logo)
//                        }
//                    }
//                }
//            }
//        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                logoViewModel.logos.collect { logosList ->
                    // 🟢 حفظ القائمة في المتغير المحلي لضمان توفرها دائماً للأزرار
                    currentLogosList = logosList

                    currentLogo = logosList.find { it._loid == currentLogoId }
                    currentLogo?.let { logo ->
                        val resId = resources.getIdentifier(logo.lo_image, "drawable", packageName)
                        if (resId != 0) binding.logo.setImageResource(resId)

                        if (logo.lo_completed == "1") {
                            showCompletedLayout(logo)
                        } else {
                            binding.completedLayout.visibility = View.GONE

                            if (!isWhistlePlayedForCurrentLogo) {
                                playWhistleAnimationAndStartGame(logo)
                            }
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

                    updateLetter2ButtonState()

                    if (hintEntity.player == 1) {
                        updatePlayerButtonState(true)
                    } else {
                        updatePlayerButtonState(false)
                    }

                    applyRevealedLettersIfUnlocked()
                }
            }
        }
    }

    private fun playWhistleAnimationAndStartGame(logo: LogoEntity) {
        isWhistlePlayedForCurrentLogo = true

        binding.leftHints.visibility = View.INVISIBLE
        binding.rightHints.visibility = View.INVISIBLE
        binding.ballsGrid.visibility = View.INVISIBLE

//        try {
//            val mediaPlayer = MediaPlayer.create(this, R.raw.whistle)
//            mediaPlayer?.start()
//            mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
        // تشغيل صوت الصافرة عند التفعيل
        playSound(R.raw.whistle)

        binding.whistle.visibility = View.VISIBLE
        val animShakeWhistle = AnimationUtils.loadAnimation(applicationContext, R.anim.shake_whistle)
        binding.whistle.startAnimation(animShakeWhistle)

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
                lettersAdapter.applyHideHintWithoutAnimation(correctAnswer)
            }
        }
    }

    private fun applyRevealedLettersIfUnlocked() {
        val hintState = logoHintViewModel.currentLogoHintState.value ?: return
        val letterMask = hintState.letter ?: 0
        val correctAnswer = currentLogo?.lo_name ?: return

        if (letterMask <= 0 || answerSlots.isEmpty()) return

        for (i in correctAnswer.indices) {
            if (correctAnswer[i] == ' ') continue

            if ((letterMask and (1 shl i)) != 0) {
                val tvSlot = answerSlots.getOrNull(i) ?: continue

                if (tvSlot.text.toString().isNotEmpty() && tvSlot.text.toString() != "?") {
                    continue
                }

                val correctChar = correctAnswer[i].uppercaseChar()

                tvSlot.text = correctChar.toString()
                tvSlot.setTextColor(Color.YELLOW)

                if (::lettersAdapter.isInitialized) {
                    val gridPosition = findAvailablePositionOfLetter(correctChar)
                    if (gridPosition != null) {
                        slotSourcePositions[i] = gridPosition
                        lettersAdapter.hideLetter(gridPosition)
                    }
                }
            }
        }
    }

    private fun updateHeaderHintCounter(hintsCount: Int) {
        val tvCounterValue = findViewById<TextView>(R.id.tvCounterValue) ?: findViewById<TextView>(R.id.scoreValue)
        val tvCounterLabel = findViewById<TextView>(R.id.tvCounterLabel) ?: findViewById<TextView>(R.id.scoreTitle)

        tvCounterValue?.text = hintsCount.toString()
        tvCounterValue?.setTextColor(Color.parseColor("#7CB342"))
        tvCounterLabel?.text = "HINTS"
        tvCounterLabel?.setTextColor(Color.parseColor("#7CB342"))
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
                val builder = AlertDialog.Builder(this)
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
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#9C27B0"))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#9C27B0"))
            }
        }

        binding.okInfo.setOnClickListener { binding.infoPopup.visibility = View.GONE }

        binding.player.setOnClickListener {
            val currentHintState = logoHintViewModel.currentLogoHintState.value
            val isPlayerUnlocked = currentHintState?.player == 1
            val playerNameText = currentLogo?.lo_player ?: "لا يتوفر لاعب لهذا النادي"

            val showPlayerPopup = {
                binding.playerName.text = playerNameText
                binding.playerPopup.visibility = View.VISIBLE
            }

            if (isPlayerUnlocked) {
                showPlayerPopup()
            } else {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Hints")
                builder.setMessage("Show one more player!\nCost : 1 hint")

                builder.setPositiveButton("OK") { dialog, _ ->
                    handleHintUsage {
                        logoHintViewModel.unlockPlayerHint(currentLogoId)
                        showPlayerPopup()
                    }
                    dialog.dismiss()
                }

                builder.setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }

                val dialog = builder.create()
                dialog.show()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#9C27B0"))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#9C27B0"))
            }
        }

        binding.okPlayer.setOnClickListener { binding.playerPopup.visibility = View.GONE }

        binding.nextLogoButton.setOnClickListener { navigateToNextLogo() }
        binding.prevLogoButton.setOnClickListener { navigateToPrevLogo() }

        // 🟢 الزر الأول: ينشط isSelectingSlotForLetterHint
        binding.letter.setOnClickListener {
            playSound(R.raw.kick)
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Hints")
            builder.setMessage("Show one letter!\nCost : 1 hint")

            builder.setPositiveButton("OK") { dialog, _ ->
                handleHintUsage {
                    isSelectingSlotForLetter2Hint = false
                    isSelectingSlotForLetterHint = true
                    showQuestionMarksForHint()
                }
                dialog.dismiss()
            }

            builder.setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }

            val dialog = builder.create()
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#9C27B0"))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#9C27B0"))
        }

        // 🟢 الزر الثاني: ينشط isSelectingSlotForLetter2Hint
        binding.letter2.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Hints")
            builder.setMessage("Show one letter!\nCost : 1 hint")

            builder.setPositiveButton("OK") { dialog, _ ->
                handleHintUsage {
                    isSelectingSlotForLetterHint = false
                    isSelectingSlotForLetter2Hint = true
                    showQuestionMarksForHint()
                }
                dialog.dismiss()
            }
            builder.setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }
            val dialog = builder.create()
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#9C27B0"))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#9C27B0"))
        }

        binding.hide.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Hints")
            builder.setMessage("Remove the wrong letters!\nCost : 1 hint")

            builder.setPositiveButton("OK") { dialog, _ ->
                handleHintUsage {
                    val correctAnswer = currentLogo?.lo_name
                    if (!correctAnswer.isNullOrEmpty()) {
                        if (::lettersAdapter.isInitialized) {
                            removeWrongLettersFromSlots(correctAnswer)
                            lettersAdapter.removeWrongLettersWithAnimation(
                                correctAnswer,
                                binding.ballsGrid
                            )
                        }

//                        try {
//                            val mediaPlayer = MediaPlayer.create(this, R.raw.explosion)
//                            mediaPlayer?.start()
//                            mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
//                        } catch (e: Exception) {
//                            e.printStackTrace()
//                        }
                        playSound(R.raw.explosion)

                        updateHideButtonState(true)
                        logoHintViewModel.unlockHideHint(currentLogoId)
                    }
                }
                dialog.dismiss()
            }

            builder.setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }

            val dialog = builder.create()
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#9C27B0"))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#9C27B0"))
        }
    }

    private fun removeWrongLettersFromSlots(correctAnswer: String) {
        for (i in answerSlots.indices) {
            val slot = answerSlots[i] ?: continue

            if (slot.currentTextColor == Color.YELLOW) {
                continue
            }

            val enteredText = slot.text.toString().trim()

            if (enteredText.isEmpty() || enteredText == "?") {
                continue
            }

            val enteredChar = enteredText.first().uppercaseChar()

            val correctChar = correctAnswer
                .replace(" ", "")
                .getOrNull(
                    answerSlots
                        .subList(0, i + 1)
                        .count { it != null } - 1
                )
                ?.uppercaseChar()

            if (correctChar != null && enteredChar != correctChar) {
                val gridPosition = slotSourcePositions[i]

                if (gridPosition != null) {
                    slotSourcePositions.remove(i)
                }

                slot.text = ""
                slot.setBackgroundResource(R.drawable.hint_background)
            }
        }
    }

    private fun showQuestionMarksForHint() {
        for (i in answerSlots.indices) {
            val tvSlot = answerSlots[i]
            if (tvSlot != null && tvSlot.text.isEmpty()) {
                tvSlot.text = "?"
                tvSlot.setTextColor(Color.GRAY)
            }
        }
    }

    private fun clearQuestionMarks() {
        for (tvSlot in answerSlots) {
            if (tvSlot != null && tvSlot.text == "?") {
                tvSlot.text = ""
                tvSlot.setTextColor(Color.WHITE)
            }
        }
    }

    // 🟢 تنفيذ كشف حرف للزر الأول مع الحفظ في الـ ViewModel
    private fun revealLetterAtSlot(slotIndex: Int) {
        if (!isSelectingSlotForLetterHint) return

        val correctAnswer = currentLogo?.lo_name ?: return
        val selectedSlot = answerSlots.getOrNull(slotIndex) ?: return

        if (selectedSlot.text.toString().trim() != "?") return
        if (correctAnswer.getOrNull(slotIndex) == ' ') return

        val correctChar = correctAnswer[slotIndex].uppercaseChar()
//        val correctChar = correctAnswer[slotIndex]
        clearQuestionMarks()
        isSelectingSlotForLetterHint = false

        selectedSlot.text = correctChar.toString()
        selectedSlot.setTextColor(Color.YELLOW)

        val gridPosition = findAvailablePositionOfLetter(correctChar)
        if (gridPosition != null) {
            slotSourcePositions[slotIndex] = gridPosition
            lettersAdapter.hideLetter(gridPosition)
        }

        logoHintViewModel.unlockLetterHintAt(currentLogoId, slotIndex)
        updateLetterButtonState(true)
        checkAnswerComplete()
    }

    // 🟢 تنفيذ كشف حرف مخصص للزر الثاني بدون حفظ الحرف كـ Letter Mask دائم
    private fun revealLetterAtSlot2(slotIndex: Int) {
        if (!isSelectingSlotForLetter2Hint) return

        val correctAnswer = currentLogo?.lo_name ?: return
        val selectedSlot = answerSlots.getOrNull(slotIndex) ?: return

        if (selectedSlot.text.toString().trim() != "?") return
        if (correctAnswer.getOrNull(slotIndex) == ' ') return

        val correctChar = correctAnswer[slotIndex].uppercaseChar()
//        val correctChar = correctAnswer[slotIndex]

        clearQuestionMarks()
        isSelectingSlotForLetter2Hint = false

        selectedSlot.text = correctChar.toString()
        selectedSlot.setTextColor(Color.YELLOW)

        val gridPosition = findAvailablePositionOfLetter(correctChar)
        if (gridPosition != null) {
            slotSourcePositions[slotIndex] = gridPosition
            lettersAdapter.hideLetter(gridPosition)
        }

        updateLetter2ButtonState()
        checkAnswerComplete()
    }

    private fun findAvailablePositionOfLetter(targetChar: Char): Int? {
        if (!::lettersAdapter.isInitialized) return null

        val usedPositions = slotSourcePositions.values.toSet()

        for (i in 0 until lettersAdapter.getItemCountSize()) {
            val letter = lettersAdapter.getLetterAt(i)
            if (letter.uppercaseChar() == targetChar.uppercaseChar() && !usedPositions.contains(i)) {
                return i
            }

//            if (letter == targetChar && !usedPositions.contains(i)) {
//                return i
//            }
        }
        return null
    }

    private fun updateLetterButtonState(isUsed: Boolean) {
        binding.letter.isSelected = isUsed
        binding.letter.alpha = if (isUsed) 0.5f else 1.0f
    }

    private fun updateLetter2ButtonState() {
        binding.letter2.isEnabled = true
        binding.letter2.alpha = 1.0f
    }

    private fun updateHideButtonState(isUsed: Boolean) {
        binding.hide.isSelected = isUsed
        binding.hide.alpha = if (isUsed) 0.5f else 1.0f
    }

    private fun updatePlayerButtonState(isUsed: Boolean) {
        binding.player.isSelected = isUsed
        binding.player.alpha = if (isUsed) 0.5f else 1.0f
    }

    fun showBlackCustomDialog(message: String, imageResId: Int) {
        val builder = AlertDialog.Builder(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_hint_info, null)
        builder.setView(dialogView)

        val dialog = builder.create()

        val ivDialogIcon = dialogView.findViewById<ImageView>(R.id.ivDialogIcon)
        val tvDialogMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)

        ivDialogIcon.setImageResource(imageResId)
        tvDialogMessage.text = message

        btnOk.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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

                // 🟢 تحديد أي الدالتين سيتم تنفيذها بناءً على الزر المضغوط
                slotView.setOnClickListener {
                    when {
                        isSelectingSlotForLetterHint -> revealLetterAtSlot(slotIndex)
                        isSelectingSlotForLetter2Hint -> revealLetterAtSlot2(slotIndex)
                        else -> removeLetterFromAnswer(slotIndex)
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
        if (isSelectingSlotForLetterHint || isSelectingSlotForLetter2Hint) {
            clearQuestionMarks()
            isSelectingSlotForLetterHint = false
            isSelectingSlotForLetter2Hint = false
        }

        for (i in answerSlots.indices) {
            val tvSlot = answerSlots[i]
            if (tvSlot != null && tvSlot.text.isEmpty()) {
//
//                try {
//                    val mediaPlayer = MediaPlayer.create(this, R.raw.kick)
//                    mediaPlayer?.start()
//                    mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
                // تشغيل صوت النقر على الكرات
                playSound(R.raw.kick)

                tvSlot.text = letter.toString()
                tvSlot.setTextColor(Color.WHITE)
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
            if (tvSlot.currentTextColor == Color.YELLOW) {
                return
            }

//            try {
//                val mediaPlayer = MediaPlayer.create(this, R.raw.space)
//                mediaPlayer?.start()
//                mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
            playSound(R.raw.space)

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
//                try {
//                    val mediaPlayer = MediaPlayer.create(this, R.raw.right_crowd)
//                    mediaPlayer?.start()
//                    mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
                playSound(R.raw.wrong_crowd)

                hintViewModel.rewardHints(2)
                Toast.makeText(applicationContext, "+2 Hints!", Toast.LENGTH_SHORT).show()
                logoHintViewModel.submitCorrectAnswer(currentLogoId, 100, currentLevelId)

                binding.root.postDelayed({
                    currentLogo?.let { showCompletedLayout(it) }
                }, 300)

            } else {
                if (hintViewModel.currentHints.value > 0) {
                    hintViewModel.useHint()
                    Toast.makeText(applicationContext, "-1 Hint!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "إجابة خاطئة!", Toast.LENGTH_SHORT).show()
                }

                try {
                    val mediaPlayer = MediaPlayer.create(this, R.raw.wrong_crowd)
                    mediaPlayer?.start()
                    mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                binding.wrong.visibility = View.VISIBLE

                binding.root.postDelayed({
                    binding.wrong.visibility = View.GONE

                    for (i in answerSlots.indices) {
                        removeLetterFromAnswer(i)
                    }
                }, 1500)
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

    private fun generateShuffledLetters2(answer: String): List<Char> {
        // إزالة الفراغات والاحتفاظ بالحروف العربية
        val cleanAnswer = answer.replace(" ", "").trim()
        val lettersList = cleanAnswer.toMutableList()

        // الأبجدية العربية
        val arabicAlphabet = "أبتثجحخدذرزسشصضطظعغفقكلمنهوي"

        // ملء الشبكة حتى 18 حرفاً بحروف عربية عشوائية
        while (lettersList.size < 18) {
            val randomChar = arabicAlphabet.random()
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
        if (currentLogosList.isEmpty()) {
            Toast.makeText(this, "جاري تحميل البيانات...", Toast.LENGTH_SHORT).show()
            return
        }

        val currentIndex = currentLogosList.indexOfFirst { it._loid == currentLogoId }
        if (currentIndex != -1 && currentIndex < currentLogosList.size - 1) {
            val nextLogo = currentLogosList[currentIndex + 1]
            switchLogo(nextLogo._loid)
        } else {
            Toast.makeText(this, "هذا هو الشعار الأخير في هذا المستوى!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToPrevLogo() {
        if (currentLogosList.isEmpty()) {
            Toast.makeText(this, "جاري تحميل البيانات...", Toast.LENGTH_SHORT).show()
            return
        }

        val currentIndex = currentLogosList.indexOfFirst { it._loid == currentLogoId }
        if (currentIndex > 0) {
            val prevLogo = currentLogosList[currentIndex - 1]
            switchLogo(prevLogo._loid)
        } else {
            Toast.makeText(this, "هذا هو الشعار الأول!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun switchLogo(newLogoId: Int?) {
        if (newLogoId != null) {
            currentLogoId = newLogoId
        }
        isWhistlePlayedForCurrentLogo = false

        // إعادة تحميل تلميحات الشعار الجديد
        logoHintViewModel.loadHintStateForLogo(newLogoId)

        // البحث عن الشعار في القائمة المحلية وتحديث الواجهة
        val logo = currentLogosList.find { it._loid == currentLogoId }

        logo?.let {
            val resId = resources.getIdentifier(it.lo_image, "drawable", packageName)
            if (resId != 0) binding.logo.setImageResource(resId)

            if (it.lo_completed == "1") {
                showCompletedLayout(it)
            } else {
                binding.completedLayout.visibility = View.GONE
                binding.leftHints.visibility = View.VISIBLE
                binding.rightHints.visibility = View.VISIBLE
                binding.ballsGrid.visibility = View.VISIBLE
                playWhistleAnimationAndStartGame(it)
            }
        }
    }

    private fun updateActivityForNewLogo(newLogoId: Int) {
        currentLogoId = newLogoId
        isWhistlePlayedForCurrentLogo = false
        logoHintViewModel.loadHintStateForLogo(currentLogoId)
        logoViewModel.loadLogosForLevel(currentLevelId)
    }

    // دالة للتحقق مما إذا كان الصوت مفعل من الإعدادات
    private fun isSoundEnabled(): Boolean {
        val prefs = getSharedPreferences("game_settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("sound_effects", true)
    }

    // دالة موحدة لتشغيل الأصوات مع التحقق التلقائي
    private fun playSound(soundResId: Int) {
        if (!isSoundEnabled()) return
        try {
            val mediaPlayer = MediaPlayer.create(this, soundResId)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener { mp -> mp.release() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
//findAvailablePositionOfLetter
//revealLetterAtSlot و revealLetterAtSlot2
//generateShuffledLetters
//private fun observeGameStates() {
//
//    lifecycleScope.launch {
//        repeatOnLifecycle(Lifecycle.State.STARTED) {
//            hintViewModel.currentHints.collect { hintsCount ->
//                updateHeaderHintCounter(hintsCount)
//            }
//        }
//    }
//
//    lifecycleScope.launch {
//        repeatOnLifecycle(Lifecycle.State.STARTED) {
//            logoViewModel.logos.collect { logosList ->
//                currentLogo = logosList.find { it._loid == currentLogoId }
//                currentLogo?.let { logo ->
//                    val resId = resources.getIdentifier(logo.lo_image, "drawable", packageName)
//                    if (resId != 0) binding.logo.setImageResource(resId)
//
//                    // 🟢 عرض السؤال دائماً
//                    binding.questionText.text = logo.lo_info.takeIf { !it.isNullOrBlank() } ?: "من هو هذا اللاعب/الفريق؟"
//
//                    if (logo.lo_completed == "1") {
//                        showCompletedLayout(logo)
//                    } else {
//                        // 🟢 تم إزالة binding.completedLayout.visibility = View.GONE لتبقى العناصر ظاهرة دائماً
//                        if (!isWhistlePlayedForCurrentLogo) {
//                            playWhistleAnimationAndStartGame(logo)
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    lifecycleScope.launch {
//        repeatOnLifecycle(Lifecycle.State.STARTED) {
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
//                    updateLetter2ButtonState()
//
//                    if (hintEntity.player == 1) {
//                        updatePlayerButtonState(true)
//                    } else {
//                        updatePlayerButtonState(false)
//                    }
//
//                    applyRevealedLettersIfUnlocked()
//                }
//            }
//        }
//    }
//}