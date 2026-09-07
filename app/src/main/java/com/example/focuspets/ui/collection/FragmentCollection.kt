package com.example.focuspets.ui.collection

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FragmentCollection : Fragment() {

    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CollectionViewModel by viewModels {
        val db = AppDatabase.getInstance(requireActivity().applicationContext)
        CollectionViewModelFactory(PetRepository(db))
    }

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

        val petAdapter = PetAdapter(::handlePetClick)
        binding.rvPokedex.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)   // 每行 3 列
            adapter = petAdapter
        }

        viewModel.availablePoints.observe(viewLifecycleOwner) {
            binding.tvPoints.text = "可用积分 $it"
        }
        viewModel.uiState.observe(viewLifecycleOwner) { list ->
            petAdapter.submitList(list)
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

    /** 点击分发：已解锁 → 详情；够分 → 确认解锁；不够 → 差多少 */
    private fun handlePetClick(item: PetDisplay) {
        when {
            item.isUnlocked -> PetDetailBottomSheet.newInstance(item.pet)
                .show(childFragmentManager, PetDetailBottomSheet.TAG)

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
        super.onDestroyView()
        _binding = null
    }
}
