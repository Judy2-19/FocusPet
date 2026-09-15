package com.example.focuspets.ui.collection

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.focuspets.databinding.FragmentCollectionBinding
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.db.PetRepository
import com.example.focuspets.BuildConfig
import com.example.focuspets.debug.DebugHelper
import com.example.focuspets.model.Backgrounds
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FragmentCollection : Fragment() {

    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CollectionViewModel by viewModels {
        val db = AppDatabase.getInstance(requireActivity().applicationContext)
        CollectionViewModelFactory(PetRepository(db))
    }

    /** 调试（仅 test 分支）：当前是否已全部解锁，决定满配按钮的行为 */
    private var allUnlocked = false

    private lateinit var adapter: PetAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 应用用户选中的背景色
        Backgrounds.apply(requireContext(), binding.root)

        // 图鉴网格：每只是清晰的平面卡牌缩略图，点进去才是单只 3D 卡牌
        binding.rvPets.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = PetAdapter { item -> handlePetClick(item) }
        binding.rvPets.adapter = adapter

        viewModel.availablePoints.observe(viewLifecycleOwner) {
            binding.tvPoints.text = "可用积分 $it"
        }
        viewModel.uiState.observe(viewLifecycleOwner) { list ->
            // 调试按钮：根据「是否已全部解锁」切换文案
            allUnlocked = list.isNotEmpty() && list.all { it.isUnlocked }
            binding.btnDebugUnlock.text = if (allUnlocked) "🛠 重置进度" else "🛠 一键满配"
            adapter.submitList(list)
        }

        // 调试：满配 / 重置进度
        binding.btnDebugUnlock.setOnClickListener {
            if (allUnlocked) DebugHelper.resetToInitial(requireContext())
            else DebugHelper.unlockAll(requireContext())
        }

        // 调试：把可用积分补到 100 万
        binding.btnDebugPoints.setOnClickListener {
            DebugHelper.grantPoints(requireContext())
            Toast.makeText(requireContext(), "💰 积分已补到 100 万", Toast.LENGTH_SHORT).show()
        }
        viewModel.unlockEvent.observe(viewLifecycleOwner) { pet ->
            pet ?: return@observe
            Toast.makeText(
                requireContext(),
                "🎉 解锁成功：${pet.name}！",
                Toast.LENGTH_SHORT
            ).show()
            viewModel.consumeUnlockEvent()
        }
    }

    /** 点击分发：已解锁 → 单只 3D 卡牌；够分 → 确认解锁；不够 → 差多少 */
    private fun handlePetClick(item: PetDisplay) {
        when {
            item.isUnlocked -> {
                val intent = Intent(requireContext(), PetCardActivity::class.java)
                intent.putExtra(PetCardActivity.EXTRA_PET, item.pet)
                intent.putExtra(PetCardActivity.EXTRA_UNLOCKED, true)
                startActivity(intent)
            }

            item.enoughPoints -> MaterialAlertDialogBuilder(requireContext())
                .setTitle("解锁「${item.pet.name}」？")
                .setMessage(
                    "是否花费 ${item.pet.unlockCost} 积分解锁？\n" +
                        "当前可用积分：${item.availablePoints}"
                )
                .setPositiveButton("解锁") { _, _ -> viewModel.unlockPet(item.pet) }
                .setNegativeButton("再想想", null)
                .show()

            else -> MaterialAlertDialogBuilder(requireContext())
                .setTitle("尚未解锁")
                .setMessage(
                    "还差 ${item.pointsNeeded} 积分即可解锁「${item.pet.name}」，" +
                        "继续专注吧！"
                )
                .setPositiveButton("加油", null)
                .show()
        }
    }

    override fun onDestroyView() {
        binding.rvPets.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
