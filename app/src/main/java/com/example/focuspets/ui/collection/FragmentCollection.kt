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

    /** Debug/test branch: whether all pets are unlocked, controlling the full-unlock button. */
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

        // Apply the user's selected background color.
        Backgrounds.apply(requireContext(), binding.root)

        // Collection grid uses clear flat thumbnails; open a single 3D card on tap.
        binding.rvPets.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = PetAdapter { item -> handlePetClick(item) }
        binding.rvPets.adapter = adapter

        viewModel.availablePoints.observe(viewLifecycleOwner) {
            binding.tvPoints.text = "可用积分 $it"
        }
        viewModel.uiState.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            // Debug button text, visible only in test builds, depends on whether all pets are unlocked.
            if (BuildConfig.DEBUG && BuildConfig.IS_TEST_BUILD) {
                allUnlocked = list.isNotEmpty() && list.all { it.isUnlocked }
                binding.btnDebugUnlock.text = if (allUnlocked) "🛠 重置进度" else "🛠 一键满配"
            }
        }

        // Debug tools are available only with -PisTestBuild=true; main defaults to GONE,
        // preventing accidental full unlock, point top-up, or progress reset.
        val showDebug = BuildConfig.DEBUG && BuildConfig.IS_TEST_BUILD
        binding.btnDebugUnlock.visibility = if (showDebug) View.VISIBLE else View.GONE
        binding.btnDebugPoints.visibility = if (showDebug) View.VISIBLE else View.GONE
        if (showDebug) {
            // Debug: unlock all or reset progress.
            binding.btnDebugUnlock.setOnClickListener {
                if (allUnlocked) DebugHelper.resetToInitial(requireContext())
                else DebugHelper.unlockAll(requireContext())
            }

            // Debug: top up available points to one million.
            binding.btnDebugPoints.setOnClickListener {
                DebugHelper.grantPoints(requireContext())
                Toast.makeText(requireContext(), "💰 积分已补到 100 万", Toast.LENGTH_SHORT).show()
            }
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

    /** Tap handling: unlocked opens a 3D card; affordable asks for unlock confirmation; otherwise show missing points. */
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
