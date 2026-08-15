package com.example.football2.adabter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.TextView
import com.example.football2.R

class LettersAdapter(
    private val context: Context,
    private val letters: List<Char>,
    private val onLetterClick: (position: Int, letter: Char) -> Unit
) : BaseAdapter() {

    private val hiddenPositions = HashSet<Int>()

    override fun getCount(): Int = letters.size

    override fun getItem(position: Int): Any = letters[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_letter_ball, parent, false)
        val tvLetter = view.findViewById<TextView>(R.id.tvLetter)

        val letter = letters[position]
        tvLetter.text = letter.toString()

        if (hiddenPositions.contains(position)) {
            view.visibility = View.INVISIBLE
        } else {
            view.visibility = View.VISIBLE
            view.setOnClickListener {
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
        notifyDataSetChanged()
    }

    fun hideLetterByChar(targetChar: Char) {
        val index = letters.indices.firstOrNull { letters[it] == targetChar && !hiddenPositions.contains(it) }
        index?.let {
            hideLetter(it)
        }
    }


    // 🌟 دالة الإخفاء مع الأنيميشن الخاصة بـ GridView
    fun removeWrongLettersWithAnimation(
        correctAnswer: String,
        gridView: GridView,
        onComplete: (() -> Unit)? = null
    ) {
        val cleanAnswer = correctAnswer.replace(" ", "").uppercase()
        val remainingCorrectChars = cleanAnswer.toMutableList()

        // 1. تحديد المواقع التي تحتوي على أحرف صحيحة
        val keepIndices = mutableSetOf<Int>()
        for (i in letters.indices) {
            val letter = letters[i]
            if (remainingCorrectChars.contains(letter)) {
                keepIndices.add(i)
                remainingCorrectChars.remove(letter)
            }
        }

        // 2. جمع المواقع الخاطئة التي سيتم إخفاؤها
        val positionsToAnimate = mutableListOf<Int>()
        for (i in letters.indices) {
            if (!keepIndices.contains(i) && !hiddenPositions.contains(i)) {
                positionsToAnimate.add(i)
            }
        }

        if (positionsToAnimate.isEmpty()) {
            onComplete?.invoke()
            return
        }

        val fadeOutAnim = AnimationUtils.loadAnimation(context, R.anim.blink)
        var animCount = 0
        val totalToAnimate = positionsToAnimate.size

        fadeOutAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                animCount++
                if (animCount >= totalToAnimate) {
                    // بعد انتهاء الأنيميشن، نضيف المواقع إلى المجموعه المخفية ونحدث الشبكة
                    hiddenPositions.addAll(positionsToAnimate)
                    notifyDataSetChanged()
                    onComplete?.invoke()
                }
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        // 3. تشغيل الأنيميشن على الـ Views داخل الـ GridView
        val firstVisible = gridView.firstVisiblePosition
        for (pos in positionsToAnimate) {
            val childIndex = pos - firstVisible
            val childView = gridView.getChildAt(childIndex)
            if (childView != null) {
                childView.startAnimation(fadeOutAnim)
            } else {
                // إذا كان العنصر غير ظاهر حالياً، نقوم بحسبته مباشرة
                animCount++
            }
        }
    }

    fun removeWron2gLetters(correctAnswer: String) {
        val cleanAnswer = correctAnswer.replace(" ", "").uppercase()
        val requiredCountMap = cleanAnswer.groupingBy { it }.eachCount().toMutableMap()

        for (i in letters.indices) {
            val char = letters[i]
            val needed = requiredCountMap[char] ?: 0
            if (needed > 0) {
                requiredCountMap[char] = needed - 1
            } else {
                hiddenPositions.add(i)
            }
        }
        notifyDataSetChanged()
    }

    fun removeWrongLetters(correctAnswer: String) {

        val requiredLetters = mutableMapOf<Char, Int>()

        for (char in correctAnswer.uppercase()) {
            if (char != ' ') {
                requiredLetters[char] =
                    requiredLetters.getOrDefault(char, 0) + 1
            }
        }

        val wrongPositions = mutableListOf<Int>()

        for (i in letters.indices) {

            val letter = letters[i].uppercaseChar()

            val needed =
                requiredLetters.getOrDefault(letter, 0)

            if (needed > 0) {

                requiredLetters[letter] = needed - 1

            } else {

                wrongPositions.add(i)
            }
        }

        // نخزن الحروف الخاطئة
        hiddenPositions.addAll(wrongPositions)

        // نعيد الرسم
        notifyDataSetChanged()

        // بعد أن GridView يعرض العناصر الجديدة
        // نشغل Animation على الخلايا الخاطئة
    }

    // إظهار حرف معين مجدداً في الشبكة
    fun showLetterByChar(targetChar: Char) {
        val index = letters.indices.firstOrNull { letters[it] == targetChar && hiddenPositions.contains(it) }
        index?.let {
            showLetter(it)
        }
    }
//
    fun applyHideHintWithoutAnimation(correctAnswer: String) {
        removeWrongLetters(correctAnswer)
    }
}