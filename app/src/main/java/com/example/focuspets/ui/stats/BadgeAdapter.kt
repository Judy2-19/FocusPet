package com.example.focuspets.ui.stats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.focuspets.R
import com.example.focuspets.badge.BadgeCelebration
import com.example.focuspets.databinding.ItemBadgeBinding
import com.example.focuspets.model.AchievementBadge

/** 连续打卡成就徽章的网格适配器（3 列） */
class BadgeAdapter : RecyclerView.Adapter<BadgeAdapter.BadgeVH>() {

    private var items = emptyList<AchievementBadge>()

    fun submit(list: List<AchievementBadge>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeVH {
        val binding = ItemBadgeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BadgeVH(binding)
    }

    override fun onBindViewHolder(holder: BadgeVH, position: Int) {
        holder.bind(items[position])
    }

    class BadgeVH(private val b: ItemBadgeBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: AchievementBadge) {
            b.tvEmoji.text = if (item.unlocked) item.emoji else "\uD83D\uDD12"
            b.tvTitle.text = item.title
            b.tvDesc.text = if (item.unlocked) item.desc else "连续 ${item.target} 天解锁"

            val pct = (item.progress.toFloat() / item.target.coerceAtLeast(1))
                .coerceIn(0f, 1f)
            b.progress.progress = (pct * 100).toInt()
            b.tvProgress.text = "${(pct * 100).toInt()}%"

            if (item.unlocked) {
                b.cardBg.setBackgroundResource(R.drawable.bg_badge_unlocked)
                b.tvTitle.setTextColor(0xFF212121.toInt())
                b.tvProgress.setTextColor(0xFFF57F17.toInt())
            } else {
                b.cardBg.setBackgroundResource(R.drawable.bg_badge_locked)
                b.tvTitle.setTextColor(0xFFBDBDBD.toInt())
                b.tvProgress.setTextColor(0xFF9E9E9E.toInt())
            }

            // 已解锁的徽章：点击即可随时重新生成海报并分享
            b.root.setOnClickListener {
                if (item.unlocked) BadgeCelebration.showPosterForBadge(b.root.context, item)
            }
        }
    }
}
