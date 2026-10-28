package com.example.focuspets.ui.wardrobe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.focuspets.databinding.DialogDogWardrobeBinding
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.db.PetRepository
import com.example.focuspets.model.DogWardrobe
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

/**
 * Dog wardrobe: free tails, gray free/other colors 50, tie 120 → outfit 150 → cap 180. Reuse the cat's
 * generic points/purchased-ID ViewModel and wardrobe_purchases table. Accessories require preceding
 * purchases. Disable unavailable artwork combinations such as multicolor outfits/caps or pink short-
 * tail suits.
 */
class DogWardrobeBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogDogWardrobeBinding? = null
    private val binding get() = _binding!!

    // Reuse the cat wardrobe's generic ViewModel from the same package.
    private val viewModel: CatWardrobeViewModel by viewModels {
        val db = AppDatabase.getInstance(requireActivity().applicationContext)
        CatWardrobeViewModelFactory(PetRepository(db))
    }

    private var points = 0
    private var owned: Set<String> = emptySet()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogDogWardrobeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.availablePoints.observe(viewLifecycleOwner) { points = it; render() }
        viewModel.ownedIds.observe(viewLifecycleOwner) { owned = it.toSet(); render() }
        viewModel.message.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.consumeMessage()
            }
        }

        binding.btnClose.setOnClickListener { dismiss() }
    }

    private fun render() {
        if (_binding == null) return
        binding.tvPoints.text = "可用积分 $points"
        buildTails()
        buildColors()
        buildOutfit()
    }

    // ---------------- Tail: free selection ----------------

    private fun buildTails() {
        binding.gridTails.removeAllViews()
        val current = DogWardrobe.getEquippedTail(requireContext())
        for (tail in DogWardrobe.TAILS) {
            val isEquipped = tail == current
            val btn = makeChip(
                if (isEquipped) "✓ ${DogWardrobe.tailLabel(tail)}" else DogWardrobe.tailLabel(tail),
                isEquipped
            )
            btn.setOnClickListener {
                DogWardrobe.setEquippedTail(requireContext(), tail)
                DogWardrobe.pruneInvalid(requireContext())
                notifyChanged()
                render()
            }
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(4, 4, 4, 4)
            }
            binding.gridTails.addView(btn, lp)
        }
    }

    // ---------------- Skin colors ----------------

    private fun buildColors() {
        binding.gridColors.removeAllViews()
        val current = DogWardrobe.getEquippedColor(requireContext())
        for (color in DogWardrobe.COLORS) {
            val isDefault = color == DogWardrobe.DEFAULT_COLOR
            val isOwned = isDefault || owned.contains(DogWardrobe.colorItemId(color))
            val isEquipped = color == current
            val label = DogWardrobe.colorLabel(color)
            val text = when {
                isEquipped -> "✓ $label"
                isOwned -> label
                else -> "$label ${DogWardrobe.COLOR_COST}分"
            }
            val btn = makeChip(text, isEquipped)
            btn.setOnClickListener {
                if (isOwned) equipColor(color)
                else viewModel.buy(
                    DogWardrobe.colorItemId(color), "dog_color", DogWardrobe.COLOR_COST
                ) { equipColor(color) }
            }
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(4, 4, 4, 4)
            }
            binding.gridColors.addView(btn, lp)
        }
    }

    private fun equipColor(color: String) {
        DogWardrobe.setEquippedColor(requireContext(), color)
        DogWardrobe.pruneInvalid(requireContext())
        notifyChanged()
        render()
    }

    // ---------------- Accessories: tie → suit/dress → graduation cap ----------------

    private fun buildOutfit() {
        binding.containerOutfit.removeAllViews()
        val ctx = requireContext()
        val tail = DogWardrobe.getEquippedTail(ctx)
        val color = DogWardrobe.getEquippedColor(ctx)
        val outfitName = if (tail == "long") "礼服" else "西服"

        val tieOwned = owned.contains(DogWardrobe.TIE_ITEM_ID)
        val outfitOwned = owned.contains(DogWardrobe.OUTFIT_ITEM_ID)
        val capOwned = owned.contains(DogWardrobe.CAP_ITEM_ID)

        val tieOn = DogWardrobe.isTieEquipped(ctx)
        val outfitOn = DogWardrobe.isOutfitEquipped(ctx) && outfitOwned
        val capOn = DogWardrobe.isCapEquipped(ctx) && capOwned

        // Tie.
        val tieBtn = makeChip(
            when {
                tieOn -> "✓ 红领带（点击脱下）"
                tieOwned -> "红领带（点击穿上）"
                else -> "红领带 ${DogWardrobe.TIE_COST}分"
            }, tieOn
        )
        tieBtn.setOnClickListener {
            if (tieOwned) {
                if (tieOn) {
                    // Removing the tie also removes the dependent outfit and cap.
                    DogWardrobe.setTieEquipped(ctx, false)
                    DogWardrobe.setOutfitEquipped(ctx, false)
                    DogWardrobe.setCapEquipped(ctx, false)
                } else {
                    DogWardrobe.setTieEquipped(ctx, true)
                }
                notifyChanged()
                render()
            } else {
                viewModel.buy(DogWardrobe.TIE_ITEM_ID, "dog_tie", DogWardrobe.TIE_COST) {
                    DogWardrobe.setTieEquipped(ctx, true)
                    notifyChanged()
                    render()
                }
            }
        }
        binding.containerOutfit.addView(tieBtn, rowLp())

        // Suit / dress.
        val outfitEnabled = tieOwned && DogWardrobe.outfitChangesImage(ctx, tail, color)
        val outfitBtn = makeChip(
            when {
                outfitOn -> "✓ $outfitName（点击脱下）"
                outfitOwned -> "$outfitName（点击穿上）"
                else -> "$outfitName ${DogWardrobe.OUTFIT_COST}分"
            }, outfitOn
        )
        outfitBtn.isEnabled = outfitEnabled || outfitOwned
        outfitBtn.setOnClickListener {
            if (outfitOwned) {
                if (outfitOn) {
                    DogWardrobe.setOutfitEquipped(ctx, false)
                    DogWardrobe.setCapEquipped(ctx, false)
                } else {
                    DogWardrobe.setTieEquipped(ctx, true)
                    DogWardrobe.setOutfitEquipped(ctx, true)
                    // Gray/white short tails lack standalone suit art; equipping the suit also includes the cap.
                    if (!DogWardrobe.hasIndependentSuit(ctx, tail, color)) {
                        DogWardrobe.setCapEquipped(ctx, true)
                    }
                }
                notifyChanged()
                render()
            } else if (outfitEnabled) {
                viewModel.buy(DogWardrobe.OUTFIT_ITEM_ID, "dog_outfit", DogWardrobe.OUTFIT_COST) {
                    DogWardrobe.setTieEquipped(ctx, true)
                    DogWardrobe.setOutfitEquipped(ctx, true)
                    if (!DogWardrobe.hasIndependentSuit(ctx, tail, color)) {
                        DogWardrobe.setCapEquipped(ctx, true)
                    }
                    notifyChanged()
                    render()
                }
            }
        }
        binding.containerOutfit.addView(outfitBtn, rowLp())

        // Graduation cap requires the red tie and usually an outfit; asset gaps may unlock the cap-with-outfit combination directly.
        val capChanges = DogWardrobe.capChangesImage(ctx, tail, color)
        val outfitChanges = DogWardrobe.outfitChangesImage(ctx, tail, color)
        val capEnabled = tieOwned && capChanges && (outfitOwned || !outfitChanges)
        binding.tvCapHint.visibility = if (capEnabled || capOwned) View.GONE else View.VISIBLE
        binding.tvCapHint.text = when {
            !tieOwned -> "需要先购买红领带"
            outfitChanges && !outfitOwned -> "需要先购买${outfitName}才能买学士帽"
            !capChanges -> "当前颜色暂无学士帽立绘"
            else -> ""
        }
        val capBtn = makeChip(
            when {
                capOn -> "✓ 学士帽（点击脱下）"
                capOwned -> "学士帽（点击穿上）"
                else -> "学士帽 ${DogWardrobe.CAP_COST}分"
            }, capOn
        )
        capBtn.isEnabled = capEnabled || capOwned
        capBtn.setOnClickListener {
            if (capOwned) {
                if (capOn) {
                    // For gray/white short-tail suit-and-cap artwork, the cap is not visually independent.
                    // Removing only the cap has no effect, so remove the suit too to keep state consistent with artwork.
                    if (!DogWardrobe.hasIndependentSuit(ctx, tail, color)) {
                        DogWardrobe.setOutfitEquipped(ctx, false)
                    }
                    DogWardrobe.setCapEquipped(ctx, false)
                } else {
                    DogWardrobe.setTieEquipped(ctx, true)
                    DogWardrobe.setOutfitEquipped(ctx, true)
                    DogWardrobe.setCapEquipped(ctx, true)
                }
                notifyChanged()
                render()
            } else if (capEnabled) {
                viewModel.buy(DogWardrobe.CAP_ITEM_ID, "dog_cap", DogWardrobe.CAP_COST) {
                    DogWardrobe.setTieEquipped(ctx, true)
                    DogWardrobe.setOutfitEquipped(ctx, true)
                    DogWardrobe.setCapEquipped(ctx, true)
                    notifyChanged()
                    render()
                }
            }
        }
        binding.containerOutfit.addView(capBtn, rowLp())
    }

    private fun rowLp() = LinearLayout.LayoutParams(
        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
    ).apply { setMargins(4, 4, 4, 4) }

    private fun makeChip(text: String, selected: Boolean): MaterialButton =
        MaterialButton(
            requireContext(), null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            this.text = text
            this.textSize = 12f
            cornerRadius = 24
            insetTop = 0
            insetBottom = 0
            if (selected) {
                setBackgroundColor(android.graphics.Color.parseColor("#FFB300"))
                setTextColor(android.graphics.Color.WHITE)
            }
        }

    private fun notifyChanged() {
        parentFragmentManager.setFragmentResult(WARDROBE_CHANGED_KEY, bundleOf())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
