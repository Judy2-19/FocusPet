package com.example.focuspets.ui.collection

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.example.focuspets.R
import com.example.focuspets.databinding.SheetPetDetailBinding
import com.example.focuspets.db.Rarity
import com.example.focuspets.db.entity.PetEntity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** 宠物详情底部弹窗：大图 emoji + 名称 + 稀有度 + 故事文案 */
class PetDetailBottomSheet : BottomSheetDialogFragment() {

    @Suppress("DEPRECATION")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = SheetPetDetailBinding.inflate(inflater, container, false)
        val pet = arguments?.getParcelable<PetEntity>(ARG_PET) ?: error("缺少 pet 参数")

        binding.tvDetailEmoji.text = pet.emoji
        binding.tvDetailEmoji.contentDescription = pet.name
        binding.tvDetailName.text = pet.name
        binding.tvDetailDesc.text = pet.description

        val (label, colorRes) = when (pet.rarity) {
            Rarity.COMMON   -> "普通" to R.color.rarity_common
            Rarity.RARE     -> "稀有" to R.color.rarity_rare
            Rarity.LEGENDARY -> "传说" to R.color.rarity_legend
        }
        binding.tvDetailRarity.text = label
        binding.tvDetailRarity.setBackgroundColor(
            ContextCompat.getColor(requireContext(), colorRes)
        )
        binding.tvDetailRarity.setTextColor(
            if (pet.rarity == Rarity.LEGENDARY) Color.parseColor("#4A2F00") else Color.WHITE
        )

        return binding.root
    }

    companion object {
        const val TAG = "PetDetailBottomSheet"
        private const val ARG_PET = "arg_pet"

        fun newInstance(pet: PetEntity) = PetDetailBottomSheet().apply {
            arguments = bundleOf(ARG_PET to pet)
        }
    }
}
