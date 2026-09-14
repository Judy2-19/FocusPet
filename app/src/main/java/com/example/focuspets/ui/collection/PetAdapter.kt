package com.example.focuspets.ui.collection

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.focuspets.R
import com.example.focuspets.databinding.ItemPetBinding
import com.example.focuspets.db.Rarity

/** 图鉴网格适配器：每只宠物渲染成清晰的平面卡牌缩略图（按当前装扮 / 未解锁灰度） */
class PetAdapter(private val onClick: (PetDisplay) -> Unit) :
    ListAdapter<PetDisplay, PetAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PetDisplay>() {
            override fun areItemsTheSame(a: PetDisplay, b: PetDisplay) = a.pet.id == b.pet.id
            override fun areContentsTheSame(a: PetDisplay, b: PetDisplay) =
                a.isUnlocked == b.isUnlocked && a.pet.unlockCost == b.pet.unlockCost
        }
    }

    class VH(val b: ItemPetBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemPetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val ctx = holder.b.root.context

        val bmp = PetCardIcon.bitmap(ctx, item.pet, item.isUnlocked, 256)
        if (bmp != null) holder.b.ivPet.setImageBitmap(bmp)
        else holder.b.ivPet.setImageResource(android.R.drawable.ic_menu_gallery)

        holder.b.tvName.text = item.pet.name

        val (label, colorRes) = rarityLabel(item.pet.rarity)
        holder.b.tvRarity.text = label
        holder.b.tvRarity.setBackgroundColor(ContextCompat.getColor(ctx, colorRes))

        val locked = !item.isUnlocked
        holder.b.ivLock.visibility = if (locked) View.VISIBLE else View.GONE
        holder.b.vLockMask.visibility = if (locked) View.VISIBLE else View.GONE

        holder.b.root.setOnClickListener { onClick(item) }
    }

    private fun rarityLabel(r: Rarity): Pair<String, Int> = when (r) {
        Rarity.COMMON -> "普通" to R.color.rarity_common
        Rarity.RARE -> "稀有" to R.color.rarity_rare
        Rarity.LEGENDARY -> "传说" to R.color.rarity_legend
    }
}
