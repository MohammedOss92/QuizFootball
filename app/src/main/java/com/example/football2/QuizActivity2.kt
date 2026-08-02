package com.example.football2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.football.R
import com.example.football.databinding.ActivityQuiz2Binding
import com.example.football.databinding.ActivityQuizBinding
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

class QuizActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityQuiz2Binding

    private lateinit var logoViewModel: LogoViewModel
    private lateinit var hintViewModel: HintViewModel
    private lateinit var logoHintViewModel: LogoHintViewModel

    private var currentLogoId: Int = 0
    private var currentLevelId: Int = 0
    private var currentLogo: LogoEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuiz2Binding.inflate(layoutInflater)
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
                        binding.inputContainer.visibility = View.VISIBLE
                        binding.etAnswer.setText("")
                    }
                }
            }
        }

        lifecycleScope.launch {
            logoHintViewModel.currentLogoHintState.collect { hintEntity ->
                if (hintEntity != null) {
                    updateHideButtonState(hintEntity.hide == 1)
                    val letterMask = hintEntity.letter ?: 0
                    updateLetterButtonState(letterMask > 0)
                }
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

        // التحقق عند إدخال الإجابة عبر زر الضغط أو لوحة المفاتيح
        binding.btnSubmitAnswer.setOnClickListener {
            checkAnswer()
        }

        binding.etAnswer.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                checkAnswer()
                true
            } else {
                false
            }
        }

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

        // التلميح المباشر لكشف أول حرف غير موجود
        binding.letter.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Hints")
            builder.setMessage("Show first letter of answer!\nCost : 1 hint")

            builder.setPositiveButton("OK") { dialog, _ ->
                handleHintUsage {
                    revealFirstLetterHint()
                }
                dialog.dismiss()
            }

            builder.setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }

            val dialog = builder.create()
            dialog.show()
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
            dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#9C27B0"))
        }

        // التلميح الخاص بإظهار عدد أحرف كلمة السر
        binding.hide.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Hints")
            builder.setMessage("Show answer length!\nCost : 1 hint")

            builder.setPositiveButton("OK") { dialog, _ ->
                handleHintUsage {
                    val length = currentLogo?.lo_name?.length ?: 0
                    Toast.makeText(this, "طول اسم النادي: $length أحرف", Toast.LENGTH_LONG).show()
                    updateHideButtonState(true)
                    logoHintViewModel.unlockHideHint(currentLogoId)
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

    private fun checkAnswer() {
        val userInput = binding.etAnswer.text.toString().trim()
        val realAnswer = currentLogo?.lo_name?.trim() ?: ""

        if (userInput.isEmpty()) {
            Toast.makeText(this, "يرجى كتابة الإجابة أولاً", Toast.LENGTH_SHORT).show()
            return
        }

        if (userInput.equals(realAnswer, ignoreCase = true)) {
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

    private fun revealFirstLetterHint() {
        val correctAnswer = currentLogo?.lo_name ?: return
        if (correctAnswer.isNotEmpty()) {
            val firstChar = correctAnswer[0]
            val currentText = binding.etAnswer.text.toString()
            if (!currentText.startsWith(firstChar, ignoreCase = true)) {
                binding.etAnswer.setText(firstChar.toString())
                binding.etAnswer.setSelection(binding.etAnswer.text.length)
            }
            updateLetterButtonState(true)
            logoHintViewModel.unlockLetterHintAt(currentLogoId, 0)
        }
    }

    private fun showBlackCustomDialog(message: String, imageResId: Int) {
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
        binding.inputContainer.visibility = View.GONE

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
        binding.hide.isEnabled = !isUsed
        binding.hide.alpha = if (isUsed) 0.5f else 1.0f
    }

    private fun updateLetterButtonState(isUsed: Boolean) {
        binding.letter.isEnabled = !isUsed
        binding.letter.alpha = if (isUsed) 0.5f else 1.0f
    }

    private fun updateActivityForNewLogo(newLogoId: Int) {
        currentLogoId = newLogoId

        binding.whistle.visibility = View.GONE
        binding.wrong.visibility = View.GONE
        binding.infoPopup.visibility = View.GONE
        binding.playerPopup.visibility = View.GONE
        binding.infoText.text = ""
        binding.playerName.text = ""
        binding.etAnswer.setText("")

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
                binding.inputContainer.visibility = View.VISIBLE
            }
        }

        logoHintViewModel.loadHintStateForLogo(currentLogoId)
    }
}