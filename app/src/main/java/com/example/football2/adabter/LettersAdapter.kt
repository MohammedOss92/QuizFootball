package com.example.football2.adabter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.football2.R

class LettersAdapter(
    private val context: Context,
    private var letters: List<Char>,
    private val onLetterClick: (position: Int, letter: Char) -> Unit
) : BaseAdapter() {

    private val hiddenPositions = HashSet<Int>()
    private val animatingPositions = HashSet<Int>()

    override fun getCount(): Int = letters.size
    override fun getItem(position: Int): Char = letters[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_letter_ball, parent, false)
            viewHolder = ViewHolder(view.findViewById(R.id.tvLetter))
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = convertView.tag as ViewHolder
        }

        val letter = getItem(position)
        viewHolder.tvLetter.text = letter.toString()

        val shape = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(android.graphics.Color.parseColor("#7CB342"))
            cornerRadius = 12f
        }
        viewHolder.tvLetter.background = shape

        // 🛑 1. إلغاء أي أنيميشن قديم ومترسب من عمليات Recycle السابقة فوراً
        view.clearAnimation()

        // 🛑 2. تطبيق حالة الرؤية بدقة لمنع ظهور الحروف المخفية نهائياً
        if (hiddenPositions.contains(position)) {

            // إذا كان هذا الموضع يحتاج أنيميشن (تم النقر على زر التلميح للتو)
            if (animatingPositions.contains(position)) {
                animatingPositions.remove(position)

                // التأكد من رؤية العنصر أثناء تشغيل الأنيميشن
                view.visibility = View.VISIBLE

                // تشغيل الأنيميشن في الدورة القادمة للـ UI لمنع الفلشر
                view.post {
                    val animBlink = AnimationUtils.loadAnimation(context, R.anim.blink)
                    animBlink.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                        override fun onAnimationStart(animation: android.view.animation.Animation?) {}

                        override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                            // فور انتهاء الأنيميشن نقوم بالإخفاء التام
                            view.visibility = View.INVISIBLE
                        }

                        override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                    })
                    view.startAnimation(animBlink)
                }
            } else {
                // إذا كان مخفياً سابقاً (بدون أنيميشن) يُخفى فوراً وبشكل قاطع
                view.visibility = View.INVISIBLE
            }
        } else {
            // الحروف العادية النشطة
            view.visibility = View.VISIBLE
        }

        view.setOnClickListener {
            if (!hiddenPositions.contains(position)) {
                onLetterClick(position, letter)
            }
        }

        return view
    }

    fun hideLetter(position: Int) {
        hiddenPositions.add(position)
        notifyDataSetChanged()
    }

    fun showLetter(position: Int) {
        hiddenPositions.remove(position)
        animatingPositions.remove(position)
        notifyDataSetChanged()
    }

    fun updateLetters(newLetters: List<Char>) {
        this.letters = newLetters
        hiddenPositions.clear()
        animatingPositions.clear()
        notifyDataSetChanged()
    }

    fun findPositionOfLetter(letter: Char): Int {
        for (i in letters.indices) {
            if (letters[i] == letter && !hiddenPositions.contains(i)) {
                return i
            }
        }
        return -1
    }

    private class ViewHolder(val tvLetter: TextView)

    /**
     * دالة التلميح عند الضغط على زر الإخفاء: تُفعل الأنيميشن لمرة واحدة فقط
     */
    fun removeW2rongLetters(correctAnswer: String) {
        val upperAnswer = correctAnswer.uppercase()
        val validLetters = upperAnswer.filter { it != ' ' }.toSet()

        for (i in letters.indices) {
            val letterChar = letters[i].uppercaseChar()

            if (!validLetters.contains(letterChar) && !hiddenPositions.contains(i)) {
                hiddenPositions.add(i)
                animatingPositions.add(i) // إضافة للأنيميشن
            }
        }
        notifyDataSetChanged()
    }
    fun removeWrongLetters(correctAnswer: String) {
        // 1. حساب تكرار كل حرف مطلوب في الإجابة الصحيحة
        val requiredLettersCount = mutableMapOf<Char, Int>()
        for (char in correctAnswer.uppercase()) {
            if (char != ' ') {
                requiredLettersCount[char] = requiredLettersCount.getOrDefault(char, 0) + 1
            }
        }

        // 2. المرور على حروف الشبكة وتحديد الزائد/الخاطئ منها
        for (i in letters.indices) {
            val letterChar = letters[i].uppercaseChar()
            val countNeeded = requiredLettersCount.getOrDefault(letterChar, 0)

            if (countNeeded > 0) {
                // الحرف مطلوب، نستهلك نسخة واحدة منه ونتركه ظاهراً
                requiredLettersCount[letterChar] = countNeeded - 1
            } else {
                // الحرف زائد أو خاطئ -> نقوم بإخفائه
                if (!hiddenPositions.contains(i)) {
                    hiddenPositions.add(i)
                    animatingPositions.add(i) // تفعيل الأنيميشن
                }
            }
        }
        notifyDataSetChanged()
    }

    /**
     * دالة التلميح عند العودة للشاشة: إخفاء بدون أنيميشن
     */
    fun applyHideHi2ntWithoutAnimation(correctAnswer: String) {
        val upperAnswer = correctAnswer.uppercase()
        val validLetters = upperAnswer.filter { it != ' ' }.toSet()

        for (i in letters.indices) {
            val letterChar = letters[i].uppercaseChar()

            if (!validLetters.contains(letterChar)) {
                hiddenPositions.add(i)
            }
        }
        animatingPositions.clear() // تفريغ قائمة الأنيميشن
        notifyDataSetChanged()
    }
    fun applyHideHintWithoutAnimation(correctAnswer: String) {
        val requiredLettersCount = mutableMapOf<Char, Int>()
        for (char in correctAnswer.uppercase()) {
            if (char != ' ') {
                requiredLettersCount[char] = requiredLettersCount.getOrDefault(char, 0) + 1
            }
        }

        for (i in letters.indices) {
            val letterChar = letters[i].uppercaseChar()
            val countNeeded = requiredLettersCount.getOrDefault(letterChar, 0)

            if (countNeeded > 0) {
                requiredLettersCount[letterChar] = countNeeded - 1
            } else {
                hiddenPositions.add(i)
            }
        }
        animatingPositions.clear()
        notifyDataSetChanged()
    }

    fun getLetterAt(position: Int): Char {
        return letters[position]
    }

    fun getItemCountSize(): Int {
        return letters.size
    }
}