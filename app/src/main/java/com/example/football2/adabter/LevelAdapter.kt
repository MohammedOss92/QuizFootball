package com.example.football2.adabter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.football2.databinding.ItemLevelBinding
import com.example.football2.entity.LevelWithStats

class LevelAdapter(
    private val onLevelClick: (LevelWithStats) -> Unit
) : ListAdapter<LevelWithStats, LevelAdapter.LevelViewHolder>(LevelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
        val binding = ItemLevelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LevelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class LevelViewHolder(private val binding: ItemLevelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LevelWithStats, position: Int) {
            val context = binding.root.context

            // 🟢 1. التحقق من حالة القفل: هل المستوى مفتوح في قاعدة البيانات أو شرط المستوى الأول/السابق
            val isUnlocked = item.level.leOpen == 1 || position == 0 || if (position > 0) {
                currentList[position - 1].completed_logos_count >= 3
            } else false

            val countryName = item.level.leCountry ?: "LEVEL ${item.level.leid}"
            binding.tvLevelName.text = countryName.uppercase().trim()

            if (isUnlocked) {
                binding.unlockedArea.visibility = View.VISIBLE
                binding.lock.visibility = View.GONE
                binding.root.alpha = 1.0f

                binding.tvLevelScore.text = "Score : ${item.level_score ?: 0}"
                binding.tvLevelProgress.text = "${item.completed_logos_count} / ${item.logos_count}"

                binding.logosProgress.max = item.logos_count
                binding.logosProgress.progress = item.completed_logos_count

                val flagImageName = item.level.leFlag
                val assetPath = "file:///android_asset/levels/$flagImageName"

                Glide.with(context)
                    .load(assetPath)
                    .into(binding.ivLevelIcon)

            } else {
                binding.unlockedArea.visibility = View.GONE
                binding.lock.visibility = View.VISIBLE
                binding.root.alpha = 0.6f

                binding.ivLevelIcon.setImageDrawable(null)
            }

            // 🟢 2. عند الضغط إرسال العنصر المباشر
            binding.root.setOnClickListener {
                onLevelClick(item)
            }
        }
    }

    class LevelDiffCallback : DiffUtil.ItemCallback<LevelWithStats>() {
        override fun areItemsTheSame(oldItem: LevelWithStats, newItem: LevelWithStats): Boolean {
            return oldItem.level.leid == newItem.level.leid
        }

        override fun areContentsTheSame(oldItem: LevelWithStats, newItem: LevelWithStats): Boolean {
            return oldItem == newItem
        }
    }
}