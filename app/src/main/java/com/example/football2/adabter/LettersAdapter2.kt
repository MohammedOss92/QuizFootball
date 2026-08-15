package com.example.football2.adabter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.football2.R

class LettersAdapter2(
    private var letters: List<Char>,
    private val onLetterClick: (position: Int, letter: Char) -> Unit
) : BaseAdapter() {

    // الحروف المخفية نهائياً
    private val hiddenPositions = HashSet<Int>()

    // الحروف التي يجب أن تعمل Animation الآن
    private val animationPositions = HashSet<Int>()

    override fun getCount(): Int {
        return letters.size
    }

    override fun getItem(position: Int): Char {
        return letters[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?
    ): View {

        val view = convertView
            ?: LayoutInflater.from(parent?.context)
                .inflate(
                    R.layout.item_letter_ball,
                    parent,
                    false
                )

        val tvLetter =
            view.findViewById<TextView>(R.id.tvLetter)

        val letter = letters[position]

        tvLetter.text = letter.toString()

        // Background
        val shape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#7CB342"))
            cornerRadius = 12f
        }

        tvLetter.background = shape

        // مهم جداً
        view.clearAnimation()

        // =========================================
        // الحرف مخفي
        // =========================================

        if (hiddenPositions.contains(position)) {

            // إذا كان هذا الحرف مطلوب له Animation
            if (animationPositions.contains(position)) {

                // أولاً يبقى ظاهر
                view.visibility = View.VISIBLE

                val animation =
                    AnimationUtils.loadAnimation(
                        view.context,
                        R.anim.blink
                    )

                animation.setAnimationListener(
                    object : Animation.AnimationListener {

                        override fun onAnimationStart(
                            animation: Animation?
                        ) {
                        }

                        override fun onAnimationRepeat(
                            animation: Animation?
                        ) {
                        }

                        override fun onAnimationEnd(
                            animation: Animation?
                        ) {

                            // بعد انتهاء Animation
                            // يصبح الحرف مخفياً
                            view.visibility = View.INVISIBLE

                            animationPositions.remove(position)
                        }
                    }
                )

                view.startAnimation(animation)

            } else {

                // إذا كان مخفياً مسبقاً
                // لا Animation عند الرجوع
                view.visibility = View.INVISIBLE
            }

        } else {

            view.visibility = View.VISIBLE
        }

        // =========================================
        // Click
        // =========================================

        view.setOnClickListener {

            if (!hiddenPositions.contains(position)) {

                onLetterClick(
                    position,
                    letter
                )
            }
        }

        return view
    }

    // =========================================
    // إخفاء حرف يدوي
    // =========================================

    fun hideLetter(position: Int) {

        if (position !in letters.indices) return

        hiddenPositions.add(position)

        // لا Animation للحرف الذي اختاره المستخدم
        animationPositions.remove(position)

        notifyDataSetChanged()
    }

    // =========================================
    // إظهار حرف
    // =========================================

    fun showLetter(position: Int) {

        if (position !in letters.indices) return

        hiddenPositions.remove(position)
        animationPositions.remove(position)

        notifyDataSetChanged()
    }

    // =========================================
    // Hide Hint
    // =========================================

    fun removeWrongLetters(
        correctAnswer: String
    ) {

        val requiredLetters =
            mutableMapOf<Char, Int>()

        for (char in correctAnswer.uppercase()) {

            if (char != ' ') {

                requiredLetters[char] =
                    requiredLetters.getOrDefault(
                        char,
                        0
                    ) + 1
            }
        }

        for (i in letters.indices) {

            val letter =
                letters[i].uppercaseChar()

            val needed =
                requiredLetters.getOrDefault(
                    letter,
                    0
                )

            if (needed > 0) {

                // الحرف مطلوب
                requiredLetters[letter] =
                    needed - 1

            } else {

                // =================================
                // هذا حرف خاطئ
                // =================================

                hiddenPositions.add(i)

                // نريد Animation
                animationPositions.add(i)
            }
        }

        // مهم:
        // هنا GridView يعيد رسم الخلايا
        // getView() سيشغل Animation
        notifyDataSetChanged()
    }

    // =========================================
    // تطبيق Hide عند الرجوع للشعار
    // بدون Animation
    // =========================================

    fun applyHideHintWithoutAnimation(
        correctAnswer: String
    ) {

        val requiredLetters =
            mutableMapOf<Char, Int>()

        for (char in correctAnswer.uppercase()) {

            if (char != ' ') {

                requiredLetters[char] =
                    requiredLetters.getOrDefault(
                        char,
                        0
                    ) + 1
            }
        }

        for (i in letters.indices) {

            val letter =
                letters[i].uppercaseChar()

            val needed =
                requiredLetters.getOrDefault(
                    letter,
                    0
                )

            if (needed > 0) {

                requiredLetters[letter] =
                    needed - 1

            } else {

                hiddenPositions.add(i)
            }
        }

        // مهم جداً
        // عند الرجوع لا يوجد Animation
        animationPositions.clear()

        notifyDataSetChanged()
    }

    // =========================================
    // تحديث السؤال
    // =========================================

    fun updateLetters(
        newLetters: List<Char>
    ) {

        letters = newLetters

        hiddenPositions.clear()
        animationPositions.clear()

        notifyDataSetChanged()
    }

    // =========================================
    // الحصول على حرف
    // =========================================

    fun getLetterAt(
        position: Int
    ): Char {

        return letters[position]
    }

    // =========================================
    // عدد الحروف
    // =========================================

    fun getItemCountSize(): Int {

        return letters.size
    }

    // =========================================
    // البحث عن حرف غير مخفي
    // =========================================

    fun findPositionOfLetter(
        letter: Char
    ): Int {

        for (i in letters.indices) {

            if (
                letters[i].uppercaseChar() ==
                letter.uppercaseChar() &&
                !hiddenPositions.contains(i)
            ) {

                return i
            }
        }

        return -1
    }
}