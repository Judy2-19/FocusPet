package com.example.focuspets.ui.leaderboard

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focuspets.R
import com.example.focuspets.cloud.CloudSyncManager
import com.example.focuspets.databinding.FragmentLeaderboardBinding
import com.example.focuspets.model.Backgrounds

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

    private val vm: LeaderboardViewModel by viewModels()
    private lateinit var adapter: LeaderboardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Backgrounds.apply(requireContext(), binding.root)

        adapter = LeaderboardAdapter()
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLeaderboard.adapter = adapter

        binding.tvMyName.text = "昵称：${CloudSyncManager.currentName()}"

        binding.btnRefresh.setOnClickListener { vm.refresh() }
        binding.btnRename.setOnClickListener { showRenameDialog() }

        vm.board.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            adapter.submit(result.list)
            binding.tvMyRank.text =
                if (result.me?.rank != null) "我的排名：#${result.me.rank}" else "我的排名：未上榜"
            binding.tvMyPoints.text = "我的积分：${result.me?.totalPoints ?: 0}"
        }
        vm.status.observe(viewLifecycleOwner) { binding.tvStatus.text = it }
    }

    private fun showRenameDialog() {
        val edit = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(CloudSyncManager.currentName())
        }
        AlertDialog.Builder(requireContext())
            .setTitle("修改昵称")
            .setView(edit)
            .setPositiveButton("确定") { _, _ ->
                val name = edit.text.toString().trim()
                if (name.isNotEmpty()) vm.rename(name)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
