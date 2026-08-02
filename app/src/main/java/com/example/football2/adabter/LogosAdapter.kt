package com.example.football2.adabter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.football.R
import com.example.football.databinding.ItemLogoBinding
import com.example.football2.entity.LogoEntity

class LogosAdapter(
    private val onLogoClick: (LogoEntity) -> Unit
) : ListAdapter<LogoEntity, LogosAdapter.LogoViewHolder>(LogoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogoViewHolder {
        val binding = ItemLogoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LogoViewHolder(private val binding: ItemLogoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LogoEntity) {
            val context = binding.root.context

            if (item.lo_completed == "1") {
                // 1. الشعار محلول: إظهار علامة الصح الخضراء
                binding.ivCheckSolved.visibility = View.VISIBLE

                // 2. تنظيف اسم الصورة (تأكد من عدم وجود فراغات أو امتدادات مكررة)
                val imageName = item.lo_image?.trim() ?: ""

                // بناء المسار الصحيح للـ assets
                // إذا كان اسم الصورة في قاعدة البيانات يحتوي على الامتداد مثل (real_madrid.png)، فالمسار جاهز.
                // أما إذا كان اسماً مجرداً بدون امتداد، يمكنك تعديله إلى: "file:///android_asset/logos/$imageName.png"
                val logoPath = "file:///android_asset/logos/$imageName"

                // 3. تحميل الشعار الحقيقي عبر Glide
                Glide.with(context)
                    .load(logoPath)
                    .into(binding.ivLogoImage)

                // Log بسيط لمساعدتك في Logcat للتأكد من أن المسار يُقرأ بشكل سليم
                android.util.Log.d("LogosAdapter", "Loading solved logo path: $logoPath")

            } else {
                // الشعار غير محلول: إخفاء علامة الصح وعرض علامة الاستفهام البرتقالية مباشرة
                binding.ivCheckSolved.visibility = View.GONE
                binding.ivLogoImage.setImageResource(R.drawable.no_image)
            }

            binding.root.setOnClickListener {
                onLogoClick(item)
            }
        }
    }

    class LogoDiffCallback : DiffUtil.ItemCallback<LogoEntity>() {
        override fun areItemsTheSame(oldItem: LogoEntity, newItem: LogoEntity): Boolean {
            return oldItem._loid == newItem._loid
        }

        override fun areContentsTheSame(oldItem: LogoEntity, newItem: LogoEntity): Boolean {
            return oldItem == newItem
        }
    }
}