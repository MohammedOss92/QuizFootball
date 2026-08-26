package com.example.football2

import android.content.DialogInterface
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.football2.adabter.LettersAdapter2
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

class QuizActivity3 : AppCompatActivity() {

    private lateinit var binding: ActivityQuizBinding

    private lateinit var logoViewModel: LogoViewModel
    private lateinit var hintViewModel: HintViewModel
    private lateinit var logoHintViewModel: LogoHintViewModel

    private var currentLogoId = 0
    private var currentLevelId = 0

    private var currentLogo: LogoEntity? = null

    private lateinit var lettersAdapter: LettersAdapter2

    private val answerSlots =
        ArrayList<TextView?>()

    private val slotSourcePositions =
        HashMap<Int, Int>()

    private var isSelectingSlotForLetterHint = false

    private var isWhistlePlayedForCurrentLogo = false

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        binding =
            ActivityQuizBinding.inflate(
                layoutInflater
            )

        setContentView(
            binding.root
        )

        currentLogoId =
            intent.getIntExtra(
                "LOGO_ID",
                0
            )

        currentLevelId =
            intent.getIntExtra(
                "LEVEL_ID",
                0
            )

        val database =
            AppDatabase.getDatabase(
                this
            )

        val logoRepo =
            LogoRepository(
                database.logoDao()
            )

        val hintRepo =
            HintRepository(
                database.hintDao()
            )

        val logoHintRepo =
            LogoHintRepository(
                database.logoHintDao()
            )

        val gameControlRepo =
            GameControlRepository(
                database.gameControlDao(),
                database.logoDao(),
                database.levelDao(),
                database.hintDao(),
                database.logoHintDao()
            )

        val factory =
            ViewModelFactory(
                logoRepository =
                    logoRepo,

                hintRepository =
                    hintRepo,

                logoHintRepository =
                    logoHintRepo,

                gameControlRepository =
                    gameControlRepo
            )

        logoViewModel =
            ViewModelProvider(
                this,
                factory
            )[LogoViewModel::class.java]

        hintViewModel =
            ViewModelProvider(
                this,
                factory
            )[HintViewModel::class.java]

        logoHintViewModel =
            ViewModelProvider(
                this,
                factory
            )[LogoHintViewModel::class.java]

        logoViewModel.loadLogosForLevel(
            currentLevelId
        )

        hintViewModel.loadCurrentHints()

        observeGameStates()

        setupActions()

        logoHintViewModel
            .loadHintStateForLogo(
                currentLogoId
            )
    }

    // =========================================================
    // OBSERVERS
    // =========================================================

    private fun observeGameStates() {

        lifecycleScope.launch {

            hintViewModel.currentHints.collect {

                updateHeaderHintCounter(
                    it
                )
            }
        }

        lifecycleScope.launch {

            logoViewModel.logos.collect { logos ->

                currentLogo =
                    logos.find {
                        it._loid ==
                                currentLogoId
                    }

                currentLogo?.let { logo ->

                    val resId =
                        resources.getIdentifier(
                            logo.lo_image,
                            "drawable",
                            packageName
                        )

                    if (resId != 0) {

                        binding.logo
                            .setImageResource(
                                resId
                            )
                    }

                    if (
                        logo.lo_completed ==
                        "1"
                    ) {

                        showCompletedLayout(
                            logo
                        )

                    } else {

                        binding.completedLayout
                            .visibility =
                            View.GONE

                        if (
                            !isWhistlePlayedForCurrentLogo
                        ) {

                            playWhistleAnimationAndStartGame(
                                logo
                            )
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {

            logoHintViewModel
                .currentLogoHintState
                .collect { hintEntity ->

                    if (
                        hintEntity != null
                    ) {

                        if (
                            hintEntity.hide == 1
                        ) {

                            updateHideButtonState(
                                true
                            )

                            applyHideHintIfUnlocked()

                        } else {

                            updateHideButtonState(
                                false
                            )
                        }

                        val letterMask =
                            hintEntity.letter
                                ?: 0

                        updateLetterButtonState(
                            letterMask > 0
                        )

                        updatePlayerButtonState(
                            hintEntity.player == 1
                        )

                        applyRevealedLettersIfUnlocked()
                    }
                }
        }
    }

    // =========================================================
    // WHISTLE
    // =========================================================

    private fun playWhistleAnimationAndStartGame(
        logo: LogoEntity
    ) {

        isWhistlePlayedForCurrentLogo =
            true

        binding.leftHints.visibility =
            View.INVISIBLE

        binding.rightHints.visibility =
            View.INVISIBLE

        binding.ballsGrid.visibility =
            View.INVISIBLE

        try {

            val player =
                MediaPlayer.create(
                    this,
                    R.raw.whistle
                )

            player?.start()

            player?.setOnCompletionListener {
                it.release()
            }

        } catch (
            e: Exception
        ) {

            e.printStackTrace()
        }

        binding.whistle.visibility =
            View.VISIBLE

        val animation =
            AnimationUtils.loadAnimation(
                this,
                R.anim.shake_whistle
            )

        animation.setAnimationListener(
            object :
                Animation.AnimationListener {

                override fun onAnimationStart(
                    animation: Animation?
                ) {
                }

                override fun onAnimationEnd(
                    animation: Animation?
                ) {

                    binding.whistle.visibility =
                        View.GONE

                    setupKeyboard(
                        logo.lo_name ?: ""
                    )

                    applyHideHintIfUnlocked()

                    applyRevealedLettersIfUnlocked()

                    binding.leftHints.visibility =
                        View.VISIBLE

                    binding.rightHints.visibility =
                        View.VISIBLE

                    binding.ballsGrid.visibility =
                        View.VISIBLE
                }

                override fun onAnimationRepeat(
                    animation: Animation?
                ) {
                }
            }
        )

        binding.whistle.startAnimation(
            animation
        )
    }

    // =========================================================
    // APPLY HIDE
    // =========================================================

    private fun applyHideHintIfUnlocked() {

        if (
            !::lettersAdapter.isInitialized
        ) {
            return
        }

        val hintState =
            logoHintViewModel
                .currentLogoHintState
                .value
                ?: return

        if (
            hintState.hide == 1
        ) {

            val answer =
                currentLogo?.lo_name

            if (
                !answer.isNullOrEmpty()
            ) {

                // بدون Animation عند الرجوع
                lettersAdapter
                    .applyHideHintWithoutAnimation(
                        answer
                    )
            }
        }
    }

    // =========================================================
    // HEADER
    // =========================================================

    private fun updateHeaderHintCounter(
        count: Int
    ) {

        val value =
            findViewById<TextView>(
                R.id.tvCounterValue
            )
                ?: findViewById(
                    R.id.scoreValue
                )

        val label =
            findViewById<TextView>(
                R.id.tvCounterLabel
            )
                ?: findViewById(
                    R.id.scoreTitle
                )

        value?.text =
            count.toString()

        value?.setTextColor(
            Color.parseColor(
                "#7CB342"
            )
        )

        label?.text =
            "HINTS"

        label?.setTextColor(
            Color.parseColor(
                "#7CB342"
            )
        )
    }

    // =========================================================
    // ACTIONS
    // =========================================================

    private fun setupActions() {

        findViewById<View>(
            R.id.btnBack
        )?.setOnClickListener {

            finish()
        }

        // =====================================================
        // LETTER HINT
        // =====================================================

        binding.letter.setOnClickListener {

            val dialog =
                AlertDialog.Builder(this)
                    .setTitle("Hints")
                    .setMessage(
                        "Show one letter!\nCost : 1 hint"
                    )
                    .setPositiveButton(
                        "OK"
                    ) { d, _ ->

                        handleHintUsage {

                            isSelectingSlotForLetterHint =
                                true

                            showQuestionMarksForHint()
                        }

                        d.dismiss()
                    }
                    .setNegativeButton(
                        "CANCEL"
                    ) { d, _ ->
                        d.dismiss()
                    }
                    .create()

            dialog.show()

            dialog.getButton(
                DialogInterface.BUTTON_POSITIVE
            ).setTextColor(
                Color.parseColor(
                    "#9C27B0"
                )
            )

            dialog.getButton(
                DialogInterface.BUTTON_NEGATIVE
            ).setTextColor(
                Color.parseColor(
                    "#9C27B0"
                )
            )
        }

        // =====================================================
        // HIDE
        // =====================================================

        binding.hide.setOnClickListener {

            val dialog =
                AlertDialog.Builder(this)
                    .setTitle("Hints")
                    .setMessage(
                        "Remove the wrong letters!\nCost : 1 hint"
                    )
                    .setPositiveButton(
                        "OK"
                    ) { d, _ ->

                        handleHintUsage {

                            val answer =
                                currentLogo?.lo_name

                            if (
                                !answer.isNullOrEmpty()
                            ) {

                                // =================================
                                // 1. حذف الحروف الخاطئة من الإجابة
                                // =================================

                                removeWrongEnteredLetters(
                                    answer
                                )

                                // =================================
                                // 2. Animation للحروف الخاطئة
                                //    داخل GridView
                                // =================================

                                if (
                                    ::lettersAdapter.isInitialized
                                ) {

                                    lettersAdapter
                                        .removeWrongLetters(
                                            answer
                                        )
                                }

                                // =================================
                                // صوت الانفجار
                                // =================================

                                try {

                                    val player =
                                        MediaPlayer.create(
                                            this,
                                            R.raw.explosion
                                        )

                                    player?.start()

                                    player?.setOnCompletionListener {
                                        it.release()
                                    }

                                } catch (
                                    e: Exception
                                ) {

                                    e.printStackTrace()
                                }

                                // =================================
                                // حفظ حالة Hide
                                // =================================

                                updateHideButtonState(
                                    true
                                )

                                logoHintViewModel
                                    .unlockHideHint(
                                        currentLogoId
                                    )
                            }
                        }

                        d.dismiss()
                    }
                    .setNegativeButton(
                        "CANCEL"
                    ) { d, _ ->
                        d.dismiss()
                    }
                    .create()

            dialog.show()

            dialog.getButton(
                DialogInterface.BUTTON_POSITIVE
            ).setTextColor(
                Color.parseColor(
                    "#9C27B0"
                )
            )

            dialog.getButton(
                DialogInterface.BUTTON_NEGATIVE
            ).setTextColor(
                Color.parseColor(
                    "#9C27B0"
                )
            )
        }

        // =====================================================
        // NEXT / PREVIOUS
        // =====================================================

        binding.nextLogoButton.setOnClickListener {

            navigateToNextLogo()
        }

        binding.prevLogoButton.setOnClickListener {

            navigateToPrevLogo()
        }
    }

    // =========================================================
    // REMOVE WRONG ENTERED LETTERS
    // =========================================================

    private fun removeWrongEnteredLetters(
        correctAnswer: String
    ) {

        for (
        i in answerSlots.indices
        ) {

            val slot =
                answerSlots[i]
                    ?: continue

            // Letter Hint
            // يبقى محفوظاً
            if (
                slot.currentTextColor ==
                Color.YELLOW
            ) {
                continue
            }

            val entered =
                slot.text
                    .toString()
                    .trim()

            if (
                entered.isEmpty() ||
                entered == "?"
            ) {
                continue
            }

            val correctChar =
                correctAnswer
                    .getOrNull(i)
                    ?.uppercaseChar()

            val enteredChar =
                entered
                    .firstOrNull()
                    ?.uppercaseChar()

            if (
                correctChar != null &&
                enteredChar != correctChar
            ) {

                // حذف الحرف من الإجابة
                slot.text = ""

                // لا نستخدم showLetter()
                //
                // لأن Hide يجب أن يبقيه مخفياً
                slotSourcePositions.remove(
                    i
                )
            }
        }
    }

    // =========================================================
    // SETUP KEYBOARD - GRIDVIEW
    // =========================================================

    private fun setupKeyboard(
        correctAnswer: String
    ) {

        val shuffledLetters =
            generateShuffledLetters(
                correctAnswer
            )

//        lettersAdapter =
//            LettersAdapter2(
//                this,
//                shuffledLetters
//            ) { position, letter ->
//
//                addLetterToAnswer(
//                    position,
//                    letter
//                )
//            }

        // GridView
        binding.ballsGrid.adapter =
            lettersAdapter
        ///////////////////////////
        lettersAdapter = LettersAdapter2(
            shuffledLetters
        ) { position, letter ->

            addLetterToAnswer(
                position,
                letter
            )
        }

        binding.ballsGrid.adapter = lettersAdapter


        ////////////////////////////

        // تنظيف الإجابة
        binding.spacesGrid1.removeAllViews()
        binding.spacesGrid2.removeAllViews()

        answerSlots.clear()
        slotSourcePositions.clear()

        // =====================================================
        // Answer Slots
        // =====================================================

        for (
        i in correctAnswer.indices
        ) {

            if (
                correctAnswer[i] == ' '
            ) {

                val space =
                    View(this).apply {

                        layoutParams =
                            LinearLayout.LayoutParams(
                                24,
                                10
                            )
                    }

                binding.spacesGrid1
                    .addView(space)

                answerSlots.add(
                    null
                )

            } else {

                val slotView =
                    LayoutInflater
                        .from(this)
                        .inflate(
                            R.layout.item_letter_ball,
                            binding.spacesGrid1,
                            false
                        ) as FrameLayout

                val tvSlot =
                    slotView.findViewById<TextView>(
                        R.id.tvLetter
                    )

                tvSlot.text =
                    ""

                tvSlot.setBackgroundResource(
                    R.drawable.hint_background
                )

                val slotIndex =
                    i

                slotView.setOnClickListener {

                    if (
                        isSelectingSlotForLetterHint
                    ) {

                        revealLetterAtSlot(
                            slotIndex
                        )

                    } else {

                        removeLetterFromAnswer(
                            slotIndex
                        )
                    }
                }

                if (
                    i < 8
                ) {

                    binding.spacesGrid1
                        .addView(
                            slotView
                        )

                } else {

                    binding.spacesGrid2
                        .addView(
                            slotView
                        )
                }

                answerSlots.add(
                    tvSlot
                )
            }
        }
    }

    // =========================================================
    // ADD LETTER
    // =========================================================

    private fun addLetterToAnswer(
        gridPosition: Int,
        letter: Char
    ) {

        if (
            isSelectingSlotForLetterHint
        ) {

            clearQuestionMarks()

            isSelectingSlotForLetterHint =
                false
        }

        for (
        i in answerSlots.indices
        ) {

            val slot =
                answerSlots[i]

            if (
                slot != null &&
                slot.text.isEmpty()
            ) {

                try {

                    val player =
                        MediaPlayer.create(
                            this,
                            R.raw.kick
                        )

                    player?.start()

                    player?.setOnCompletionListener {
                        it.release()
                    }

                } catch (
                    e: Exception
                ) {
                    e.printStackTrace()
                }

                slot.text =
                    letter.toString()

                slot.setTextColor(
                    Color.WHITE
                )

                slotSourcePositions[i] =
                    gridPosition

                lettersAdapter.hideLetter(
                    gridPosition
                )

                checkAnswerComplete()

                break
            }
        }
    }

    // =========================================================
    // REMOVE LETTER
    // =========================================================

    private fun removeLetterFromAnswer(
        slotIndex: Int
    ) {

        val slot =
            answerSlots.getOrNull(
                slotIndex
            )

        if (
            slot != null &&
            slot.text.isNotEmpty()
        ) {

            if (
                slot.currentTextColor ==
                Color.YELLOW
            ) {
                return
            }

            try {

                val player =
                    MediaPlayer.create(
                        this,
                        R.raw.space
                    )

                player?.start()

                player?.setOnCompletionListener {
                    it.release()
                }

            } catch (
                e: Exception
            ) {
                e.printStackTrace()
            }

            val gridPosition =
                slotSourcePositions[
                    slotIndex
                ]

            if (
                gridPosition != null
            ) {

                lettersAdapter.showLetter(
                    gridPosition
                )
            }

            slot.text =
                ""

            slotSourcePositions.remove(
                slotIndex
            )
        }
    }

    // =========================================================
    // CHECK ANSWER
    // =========================================================

    private fun checkAnswerComplete() {

        val entered =
            answerSlots
                .map {
                    it?.text ?: ""
                }
                .joinToString("")
                .trim()

        val answer =
            currentLogo?.lo_name
                ?.replace(
                    " ",
                    ""
                )
                ?.trim()
                ?: ""

        if (
            entered.length !=
            answer.length
        ) {
            return
        }

        if (
            entered.equals(
                answer,
                ignoreCase = true
            )
        ) {

            try {

                val player =
                    MediaPlayer.create(
                        this,
                        R.raw.right_crowd
                    )

                player?.start()

                player?.setOnCompletionListener {
                    it.release()
                }

            } catch (
                e: Exception
            ) {
                e.printStackTrace()
            }

            hintViewModel.rewardHints(
                2
            )

            Toast.makeText(
                this,
                "+2 Hints!",
                Toast.LENGTH_SHORT
            ).show()

            logoHintViewModel
                .submitCorrectAnswer(
                    currentLogoId,
                    100,
                    currentLevelId
                )

            binding.root.postDelayed({

                currentLogo?.let {
                    showCompletedLayout(
                        it
                    )
                }

            }, 300)

        } else {

            if (
                hintViewModel
                    .currentHints
                    .value > 0
            ) {

                hintViewModel.useHint()

                Toast.makeText(
                    this,
                    "-1 Hint!",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "إجابة خاطئة!",
                    Toast.LENGTH_SHORT
                ).show()
            }

            try {

                val player =
                    MediaPlayer.create(
                        this,
                        R.raw.wrong_crowd
                    )

                player?.start()

                player?.setOnCompletionListener {
                    it.release()
                }

            } catch (
                e: Exception
            ) {
                e.printStackTrace()
            }

            binding.wrong.visibility =
                View.VISIBLE

            binding.root.postDelayed({

                binding.wrong.visibility =
                    View.GONE

                for (
                i in answerSlots.indices
                ) {

                    removeLetterFromAnswer(
                        i
                    )
                }

            }, 1500)
        }
    }

    // =========================================================
    // GENERATE LETTERS
    // =========================================================

    private fun generateShuffledLetters(
        answer: String
    ): List<Char> {

        val clean =
            answer
                .replace(
                    " ",
                    ""
                )
                .uppercase()
                .trim()

        val list =
            clean.toMutableList()

        val alphabet =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

        while (
            list.size < 18
        ) {

            val random =
                alphabet.random()

            if (
                list.count {
                    it == random
                } < 2
            ) {

                list.add(
                    random
                )
            }
        }

        return list.shuffled()
    }

    // =========================================================
    // QUESTION MARKS
    // =========================================================

    private fun showQuestionMarksForHint() {

        for (
        slot in answerSlots
        ) {

            if (
                slot != null &&
                slot.currentTextColor !=
                Color.YELLOW
            ) {

                slot.text =
                    "?"

                slot.setTextColor(
                    Color.WHITE
                )
            }
        }
    }

    private fun clearQuestionMarks() {

        for (
        slot in answerSlots
        ) {

            if (
                slot != null &&
                slot.currentTextColor !=
                Color.YELLOW &&
                slot.text
                    .toString()
                    .trim() == "?"
            ) {

                slot.text =
                    ""
            }
        }
    }

    // =========================================================
    // REVEAL LETTER
    // =========================================================

    private fun revealLetterAtSlot(
        slotIndex: Int
    ) {

        isSelectingSlotForLetterHint =
            false

        val answer =
            currentLogo?.lo_name
                ?: return

        val slot =
            answerSlots.getOrNull(
                slotIndex
            )
                ?: return

        clearQuestionMarks()

        if (
            slot.text
                .toString()
                .trim()
                .isNotEmpty()
        ) {
            return
        }

        val correctChar =
            answer[
                slotIndex
            ].uppercaseChar()

        val oldPosition =
            slotSourcePositions[
                slotIndex
            ]

        if (
            oldPosition != null
        ) {

            lettersAdapter.showLetter(
                oldPosition
            )

            slotSourcePositions.remove(
                slotIndex
            )
        }

        slot.text =
            correctChar.toString()

        slot.setTextColor(
            Color.YELLOW
        )

        findAvailablePositionOfLetter(
            correctChar
        )?.let {

            slotSourcePositions[
                slotIndex
            ] = it

            lettersAdapter.hideLetter(
                it
            )
        }

        updateLetterButtonState(
            true
        )

        logoHintViewModel
            .unlockLetterHintAt(
                currentLogoId,
                slotIndex
            )

        checkAnswerComplete()
    }

    // =========================================================
    // FIND LETTER
    // =========================================================

    private fun findAvailablePositionOfLetter(
        targetChar: Char
    ): Int? {

        if (
            !::lettersAdapter.isInitialized
        ) {
            return null
        }

        val used =
            slotSourcePositions
                .values
                .toSet()

        for (
        i in 0 until
                lettersAdapter
                    .getItemCountSize()
        ) {

            val char =
                lettersAdapter
                    .getLetterAt(i)

            if (
                char.uppercaseChar() ==
                targetChar.uppercaseChar() &&

                !used.contains(i)
            ) {

                return i
            }
        }

        val position =
            lettersAdapter
                .findPositionOfLetter(
                    targetChar
                )

        return position.takeIf {
            it != -1
        }
    }

    // =========================================================
    // REVEALED LETTERS
    // =========================================================

    private fun applyRevealedLettersIfUnlocked() {

        val state =
            logoHintViewModel
                .currentLogoHintState
                .value
                ?: return

        val mask =
            state.letter ?: 0

        val answer =
            currentLogo?.lo_name
                ?: return

        if (
            mask <= 0 ||
            !::lettersAdapter.isInitialized ||
            answerSlots.isEmpty()
        ) {
            return
        }

        for (
        i in answer.indices
        ) {

            if (
                answer[i] == ' '
            ) {
                continue
            }

            val revealed =
                (
                        mask and
                                (1 shl i)
                        ) != 0

            if (!revealed) {
                continue
            }

            val slot =
                answerSlots.getOrNull(
                    i
                )
                    ?: continue

            if (
                slot.currentTextColor ==
                Color.YELLOW
            ) {
                continue
            }

            val char =
                answer[
                    i
                ].uppercaseChar()

            val oldPosition =
                slotSourcePositions[i]

            if (
                oldPosition != null
            ) {

                lettersAdapter.showLetter(
                    oldPosition
                )

                slotSourcePositions.remove(
                    i
                )
            }

            slot.text =
                char.toString()

            slot.setTextColor(
                Color.YELLOW
            )

            findAvailablePositionOfLetter(
                char
            )?.let {

                slotSourcePositions[i] =
                    it

                lettersAdapter.hideLetter(
                    it
                )
            }
        }
    }

    // =========================================================
    // HINT USAGE
    // =========================================================

    private inline fun handleHintUsage(
        onHintUnlocked: () -> Unit
    ) {

        if (
            hintViewModel
                .currentHints
                .value > 0
        ) {

            hintViewModel.useHint()

            onHintUnlocked()

        } else {

            Toast.makeText(
                this,
                "لا يوجد رصيد مساعدات كافٍ!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // BUTTON STATES
    // =========================================================

    private fun updateHideButtonState(
        used: Boolean
    ) {

        binding.hide.isSelected =
            used

        binding.hide.isEnabled =
            !used

        binding.hide.alpha =
            if (used) 0.5f else 1f
    }

    private fun updateLetterButtonState(
        used: Boolean
    ) {

        binding.letter.isEnabled =
            !used

        binding.letter.alpha =
            if (used) 0.5f else 1f
    }

    private fun updatePlayerButtonState(
        used: Boolean
    ) {

        binding.player.isSelected =
            used

        binding.player.alpha =
            if (used) 0.5f else 1f
    }

    // =========================================================
    // COMPLETED
    // =========================================================

    private fun showCompletedLayout(
        logo: LogoEntity
    ) {

        binding.leftHints.visibility =
            View.GONE

        binding.rightHints.visibility =
            View.GONE

        binding.ballsGrid.visibility =
            View.GONE

        binding.completedLayout.visibility =
            View.VISIBLE

        binding.loName.text =
            logo.lo_name

        binding.points.text =
            "${logo.lo_points} Pt"
    }

    // =========================================================
    // NEXT
    // =========================================================

    private fun navigateToNextLogo() {

        val logos =
            logoViewModel.logos.value

        if (
            logos.isEmpty()
        ) {
            return
        }

        val index =
            logos.indexOfFirst {
                it._loid ==
                        currentLogoId
            }

        if (
            index != -1 &&
            index < logos.size - 1
        ) {

            updateActivityForNewLogo(
                logos[index + 1]
                    ._loid ?: 0
            )

        } else {

            Toast.makeText(
                this,
                "لقد وصلت لآخر شعار في هذا المستوى!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // PREVIOUS
    // =========================================================

    private fun navigateToPrevLogo() {

        val logos =
            logoViewModel.logos.value

        if (
            logos.isEmpty()
        ) {
            return
        }

        val index =
            logos.indexOfFirst {
                it._loid ==
                        currentLogoId
            }

        if (
            index > 0
        ) {

            updateActivityForNewLogo(
                logos[index - 1]
                    ._loid ?: 0
            )

        } else {

            Toast.makeText(
                this,
                "هذا هو الشعار الأول في المستوى!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // CHANGE LOGO
    // =========================================================

    private fun updateActivityForNewLogo(
        newLogoId: Int
    ) {

        currentLogoId =
            newLogoId

        isSelectingSlotForLetterHint =
            false

        isWhistlePlayedForCurrentLogo =
            false

        binding.whistle.visibility =
            View.GONE

        binding.wrong.visibility =
            View.GONE

        binding.infoPopup.visibility =
            View.GONE

        binding.playerPopup.visibility =
            View.GONE

        binding.infoText.text =
            ""

        binding.playerName.text =
            ""

        updateHideButtonState(
            false
        )

        updateLetterButtonState(
            false
        )

        val logos =
            logoViewModel.logos.value

        currentLogo =
            logos.find {
                it._loid ==
                        currentLogoId
            }

        currentLogo?.let { logo ->

            val resId =
                resources.getIdentifier(
                    logo.lo_image,
                    "drawable",
                    packageName
                )

            if (
                resId != 0
            ) {

                binding.logo
                    .setImageResource(
                        resId
                    )
            }

            if (
                logo.lo_completed ==
                "1"
            ) {

                showCompletedLayout(
                    logo
                )

            } else {

                binding.completedLayout
                    .visibility =
                    View.GONE

                playWhistleAnimationAndStartGame(
                    logo
                )
            }
        }

        logoHintViewModel
            .loadHintStateForLogo(
                currentLogoId
            )
    }

    // =========================================================
    // PLAYER
    // =========================================================

    private fun showPlayerPopup() {

        binding.playerName.text =
            currentLogo?.lo_player
                ?: "لا يتوفر لاعب لهذا النادي"

        binding.playerPopup.visibility =
            View.VISIBLE
    }

    // =========================================================
    // INFO DIALOG
    // =========================================================

    fun showBlackCustomDialog(
        message: String,
        imageResId: Int
    ) {

        val builder =
            AlertDialog.Builder(
                this
            )

        val dialogView =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.dialog_hint_info,
                    null
                )

        builder.setView(
            dialogView
        )

        val dialog =
            builder.create()

        val icon =
            dialogView.findViewById<ImageView>(
                R.id.ivDialogIcon
            )

        val messageView =
            dialogView.findViewById<TextView>(
                R.id.tvDialogMessage
            )

        val ok =
            dialogView.findViewById<Button>(
                R.id.btnOk
            )

        icon.setImageResource(
            imageResId
        )

        messageView.text =
            message

        ok.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(
            ColorDrawable(
                Color.TRANSPARENT
            )
        )

        dialog.show()
    }
}