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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.focuspets.databinding.DialogCatWardrobeBinding
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.db.PetRepository
import com.example.focuspets.model.CatWardrobe
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/** 妆扮变动通知 key：首页收到后重新加载猫咪立绘 */
const val WARDROBE_CHANGED_KEY = "wardrobe_changed"

class CatWardrobeViewModel(private val repository: PetRepository) : ViewModel() {

    val availablePoints: LiveData<Int> = repository.getAvailablePoints()
    val ownedIds: LiveData<List<String>> = repository.getWardrobeOwnedIds()

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    fun buy(itemId: String, itemType: String, cost: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val ok = repository.buyWardrobeItem(itemId, itemType, cost)
            if (ok) {
                _message.value = "🎉 购买成功！-$cost 积分"
                onSuccess()
            } else {
                _message.value = "积分不足，再专注一会儿吧"
            }
        }
    }

    fun consumeMessage() { _message.value = null }
}

class CatWardrobeViewModelFactory(private val repository: PetRepository) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CatWardrobeViewModel(repository) as T
}

/**
 * 小猫咪装扮面板：皮肤颜色 / 裙子 / 王冠。
 * 规则：灰猫默认拥有；皇冠必须先购买裙子才能买。
 */
class CatWardrobeBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogCatWardrobeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CatWardrobeViewModel by viewModels {
        val db = AppDatabase.getInstance(requireActivity().applicationContext)
        CatWardrobeViewModelFactory(PetRepository(db))
    }

    private var points = 0
    private var owned: Set<String> = emptySet()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogCatWardrobeBinding.inflate(inflater, container, false)
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
        buildColors()
        buildDress()
        buildCrowns()
    }

    // ---------------- 皮肤颜色 ----------------

    private fun buildColors() {
        binding.gridColors.removeAllViews()
        val current = CatWardrobe.getEquippedColor(requireContext())
        for (color in CatWardrobe.COLORS) {
            val isDefault = color == CatWardrobe.DEFAULT_COLOR
            val isOwned = isDefault || owned.contains(CatWardrobe.colorItemId(color))
            val isEquipped = color == current
            val label = CatWardrobe.colorLabel(color)
            val text = when {
                isEquipped -> "✓ $label"
                isOwned -> label
                else -> "$label ${CatWardrobe.COLOR_COST}分"
            }
            val btn = makeChip(text, isEquipped)
            btn.setOnClickListener {
                if (isOwned) equipColor(color)
                else viewModel.buy(
                    CatWardrobe.colorItemId(color), "color", CatWardrobe.COLOR_COST
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
        val ctx = requireContext()
        CatWardrobe.setEquippedColor(ctx, color)
        // 换色后若当前皇冠在新颜色下没有立绘，自动摘掉
        val crown = CatWardrobe.getEquippedCrown(ctx)
        val available = CatWardrobe.CROWN_OPTIONS_BY_COLOR[color] ?: emptyList()
        if (crown != null && crown !in available) CatWardrobe.setEquippedCrown(ctx, null)
        notifyChanged()
        render()
    }

    // ---------------- 裙子 ----------------

    private fun buildDress() {
        binding.containerDress.removeAllViews()
        val isOwned = owned.contains(CatWardrobe.DRESS_ITEM_ID)
        val isEquipped = CatWardrobe.isDressEquipped(requireContext()) && isOwned
        val text = when {
            isEquipped -> "✓ 红裙（点击脱下）"
            isOwned -> "红裙（点击穿上）"
            else -> "红裙 ${CatWardrobe.DRESS_COST}分"
        }
        val btn = makeChip(text, isEquipped)
        btn.setOnClickListener {
            if (isOwned) {
                if (isEquipped) {
                    // 脱裙子时皇冠一并摘掉（皇冠图都基于裙子）
                    CatWardrobe.setDressEquipped(requireContext(), false)
                    CatWardrobe.setEquippedCrown(requireContext(), null)
                } else {
                    CatWardrobe.setDressEquipped(requireContext(), true)
                }
                notifyChanged()
                render()
            } else {
                viewModel.buy(CatWardrobe.DRESS_ITEM_ID, "dress", CatWardrobe.DRESS_COST) {
                    CatWardrobe.setDressEquipped(requireContext(), true)
                    notifyChanged()
                    render()
                }
            }
        }
        binding.containerDress.addView(
            btn,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { setMargins(4, 4, 4, 4) }
        )
    }

    // ---------------- 王冠 ----------------

    private fun buildCrowns() {
        binding.containerCrown.removeAllViews()
        val ctx = requireContext()
        val color = CatWardrobe.getEquippedColor(ctx)
        val options = CatWardrobe.CROWN_OPTIONS_BY_COLOR[color] ?: emptyList()
        val dressOwned = owned.contains(CatWardrobe.DRESS_ITEM_ID)

        if (options.isEmpty()) {
            binding.tvCrownHint.visibility = View.VISIBLE
            return
        }
        binding.tvCrownHint.visibility = if (dressOwned) View.GONE else View.VISIBLE
        binding.tvCrownHint.text =
            if (dressOwned) "" else "需要先购买裙子才能买王冠"

        val equippedCrown = CatWardrobe.getEquippedCrown(ctx)
        for (crown in options) {
            val isOwned = owned.contains(CatWardrobe.crownItemId(crown))
            val isEquipped = crown == equippedCrown && isOwned
            val label = CatWardrobe.crownLabel(crown)
            val text = when {
                isEquipped -> "✓ $label"
                isOwned -> label
                else -> "$label ${CatWardrobe.CROWN_COST}分"
            }
            val btn = makeChip(text, isEquipped)
            btn.isEnabled = dressOwned || isOwned
            btn.setOnClickListener {
                if (isOwned) {
                    // 皇冠必须配裙子，戴上时自动确保裙子已穿
                    if (isEquipped) {
                        CatWardrobe.setEquippedCrown(ctx, null)
                    } else {
                        CatWardrobe.setDressEquipped(ctx, true)
                        CatWardrobe.setEquippedCrown(ctx, crown)
                    }
                    notifyChanged()
                    render()
                } else if (dressOwned) {
                    viewModel.buy(
                        CatWardrobe.crownItemId(crown), "crown", CatWardrobe.CROWN_COST
                    ) {
                        CatWardrobe.setDressEquipped(ctx, true)
                        CatWardrobe.setEquippedCrown(ctx, crown)
                        notifyChanged()
                        render()
                    }
                }
            }
            binding.containerCrown.addView(
                btn,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { setMargins(4, 4, 4, 4) }
            )
        }
    }

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
