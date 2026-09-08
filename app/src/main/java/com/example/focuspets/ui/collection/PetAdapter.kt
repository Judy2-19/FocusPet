package com.example.focuspets.ui.collection

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.focuspets.R
import com.example.focuspets.databinding.ItemPetBinding
import com.example.focuspets.db.Rarity

/** 图鉴网格适配器：已解锁/未解锁双分支渲染 */
class PetAdapter(private val onClick: (PetDisplay) -> Unit) :
    RecyclerView.Adapter<PetAdapter.PetViewHolder>() {

    private val items = mutableListOf<PetDisplay>()

    fun submitList(newItems: List<PetDisplay>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()   // 固定 9 项，无需 DiffUtil
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder =
        PetViewHolder(
            ItemPetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) =
        holder.bind(items[position])

    inner class PetViewHolder(private val binding: ItemPetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PetDisplay) = with(binding) {
            val context = root.context
            val rarityColor = ContextCompat.getColor(
                context,
                when (item.pet.rarity) {
                    Rarity.COMMON   -> R.color.rarity_common
                    Rarity.RARE     -> R.color.rarity_rare
                    Rarity.LEGENDARY -> R.color.rarity_legend
                }
            )

            if (item.isUnlocked) {
                tvEmoji.text = item.pet.emoji
                tvEmoji.alpha = 1f
                tvName.text = item.pet.name
                tvName.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                tvRarityTag.visibility = View.VISIBLE
                tvRarityTag.text = when (item.pet.rarity) {
                    Rarity.COMMON   -> "普通"
                    Rarity.RARE     -> "稀有"
                    Rarity.LEGENDARY -> "传说"
                }
                tvRarityTag.setBackgroundColor(rarityColor)
                // 金色底配深色文字，其余配白字
                tvRarityTag.setTextColor(
                    if (item.pet.rarity == Rarity.LEGENDARY)
                        Color.parseColor("#4A2F00")
                    else Color.WHITE
                )
                tvUnlockHint.visibility = View.GONE
                petCard.strokeColor = rarityColor          // 稀有度彩色边框
                petCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.surface_card)
                )
            } else {
                tvEmoji.text = "❓"
                tvEmoji.alpha = 0.35f
                tvName.text = "未解锁"
                tvName.setTextColor(ContextCompat.getColor(context, R.color.text_hint))
                tvRarityTag.visibility = View.GONE
                tvUnlockHint.visibility = View.VISIBLE
                tvUnlockHint.text = "需要 ${item.pet.unlockCost} 积分解锁"
                petCard.strokeColor = ContextCompat.getColor(context, R.color.stroke_hint)
                petCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.surface_card_locked)
                )
            }
            val rarityLabel = when (item.pet.rarity) {
                Rarity.COMMON -> "普通"
                Rarity.RARE -> "稀有"
                Rarity.LEGENDARY -> "传说"
            }
            petCard.contentDescription = if (item.isUnlocked)
                "${item.pet.name}，$rarityLabel，已解锁"
            else "${item.pet.name}，$rarityLabel，未解锁，需要 ${item.pet.unlockCost} 积分解锁"
            petCard.setOnClickListener { onClick(item) }
        }
    }
}
