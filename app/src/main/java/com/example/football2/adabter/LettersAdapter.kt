package com.example.football2.adabter


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.football.R

class LettersAdapter(
    private val context: Context,
    private var letters: List<Char>,
    private val onLetterClick: (position: Int, letter: Char) -> Unit
) : BaseAdapter() {

    private val hiddenPositions = HashSet<Int>()

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

        // 🟢 إضافة خلفية خضراء للمربعات بالأسفل لمنع اختفائها 🟢
        // يمكنك تعديل اللون #7CB342 إلى أي درجة لون تفضلها
        val shape = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(android.graphics.Color.parseColor("#7CB342")) // اللون الأخضر للوحة
            cornerRadius = 12f // درجة انحناء الزوايا للمربع
        }
        viewHolder.tvLetter.background = shape

        if (hiddenPositions.contains(position)) {
            view.visibility = View.INVISIBLE
        } else {
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
        notifyDataSetChanged()
    }

    fun updateLetters(newLetters: List<Char>) {
        this.letters = newLetters
        hiddenPositions.clear()
        notifyDataSetChanged()
    }

    private class ViewHolder(val tvLetter: TextView)

    /**
     * دالة القنبلة: تقوم بإخفاء الحروف الخاطئة من لوحة الحروف السفلية
     * @param correctAnswer الإجابة الصحيحة القادمة من قاعدة البيانات (lo_name)
     */
    /**
     * دالة القنبلة: تقوم بإخفاء الحروف الخاطئة من لوحة الحروف السفلية
     * @param correctAnswer الإجابة الصحيحة القادمة من قاعدة البيانات (lo_name)
     */
    fun removeWrongLetters(correctAnswer: String) {
        // 1. تحويل الإجابة إلى حروف كبيرة لتجنب الاختلافات بين الحروف الكبيرة والصغيرة
        val upperAnswer = correctAnswer.uppercase()

        // 2. استخراج الحروف الصحيحة فقط بدون المسافات
        val validLetters = upperAnswer.filter { it != ' ' }.toSet()

        // 3. المرور على قائمة الحروف الحالية (letters) الممررة للـ Adapter
        for (i in letters.indices) {
            val letterChar = letters[i].uppercaseChar()

            // إذا كان الحرف المعروض غير موجود في حروف الإجابة الصحيحة
            if (!validLetters.contains(letterChar)) {
                // نقوم بإضافته إلى قائمة العناصر المخفية (hiddenPositions)
                hiddenPositions.add(i)
            }
        }

        // 4. تحديث الـ GridView لإعادة رسم الحروف وإخفاء الخاطئة فوراً
        notifyDataSetChanged()
    }
}