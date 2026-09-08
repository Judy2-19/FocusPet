package com.example.focuspets.ui.focus

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.focuspets.MainActivity
import com.example.focuspets.R
import com.example.focuspets.databinding.FragmentFocusBinding
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.db.PetRepository
import com.example.focuspets.db.entity.PetEntity
import com.example.focuspets.model.PetCareState
import com.example.focuspets.model.Backgrounds
import com.example.focuspets.model.FocusPresetManager
import com.example.focuspets.service.FocusService
import com.example.focuspets.ui.focus.FocusRecordsActivity
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentFocus : Fragment() {

    private var _binding: FragmentFocusBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FocusViewModel by viewModels {
        val db = AppDatabase.getInstance(requireActivity().applicationContext)
        FocusViewModelFactory(PetRepository(db))
    }

    /** 切后台宽限：5 秒内回来没事，超过即判定失败 */
    private val bgHandler = Handler(Looper.getMainLooper())
    private val failRunnable = Runnable { notifyServiceFail() }

    private var selectedMinutes = 25
    private var totalMillisAtStart = 0L

    /** 本地仓库实例（读取专注内容历史用，与 ViewModel 共用同一 PetRepository） */
    private val repository by lazy {
        PetRepository(AppDatabase.getInstance(requireActivity().applicationContext))
    }

    /** 用户后台期间收到庆祝事件时暂存，回到前台再弹 */
    private var pendingCelebration: List<PetEntity>? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startFocus() else {
            Toast.makeText(
                requireContext(), "没有通知权限，倒计时通知将无法显示", Toast.LENGTH_SHORT
            ).show()
            startFocus()   // 依然允许计时（服务本身不受通知权限影响，只是看不到通知）
        }
    }

    /** 接收服务的计时广播：每秒 tick / 完成 / 失败 / 用户取消 */
    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                FocusService.BROADCAST_TICK -> {
                    val remaining =
                        intent.getLongExtra(FocusService.EXTRA_REMAINING_MILLIS, 0)
                    totalMillisAtStart =
                        intent.getLongExtra(FocusService.EXTRA_TOTAL_MILLIS, totalMillisAtStart)
                    renderRunning(remaining)
                }
                FocusService.BROADCAST_FINISHED ->
                    onFinished(intent.getIntExtra(FocusService.EXTRA_MINUTES_DONE, 0))
                FocusService.BROADCAST_FAILED -> onFailed()
                FocusService.BROADCAST_CANCELLED -> renderIdle()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFocusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 应用用户选中的背景色
        Backgrounds.apply(requireContext(), binding.root)

        // 右上角「专注记录」入口：打开专注记录页（日/周/月/年 + 饼图）
        binding.btnOpenRecords.setOnClickListener {
            startActivity(Intent(requireContext(), FocusRecordsActivity::class.java))
        }

        // 加载历史专注内容（用于复用）
        loadContentHistory()

        // 加载常用预设（内容 + 时长 组合，可一键套用）
        loadPresets()

        // 保存当前「内容 + 时长」为预设，下次直接复用
        binding.btnSavePreset.setOnClickListener { saveCurrentAsPreset() }

        // 时长选择（MaterialButtonToggleGroup 单选）
        binding.toggleDuration.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedMinutes = when (checkedId) {
                    R.id.btn_15 -> 15
                    R.id.btn_45 -> 45
                    else -> 25
                }
                if (!FocusService.isRunning) {
                    binding.tvRemaining.text = formatMillis(selectedMinutes * 60_000L)
                    binding.etCustomMinutes.setText(selectedMinutes.toString())
                }
            }
        }

        // 自定义时长
        binding.btnCustom.setOnClickListener { applyCustomMinutes() }
        binding.etCustomMinutes.setOnEditorActionListener { _, _, _ ->
            applyCustomMinutes()
            true
        }

        // 开始 / 放弃（同一按钮，按状态切换行为）
        binding.btnStart.setOnClickListener { tryStartFocus() }

        // 积分展示（Room LiveData，专注记录一入库自动 +N）
        viewModel.availablePoints.observe(viewLifecycleOwner) {
            binding.tvPointsFocus.text = "可用积分 $it"
        }

        // 「恭喜解锁新宠物」庆祝事件
        viewModel.celebrationEvent.observe(viewLifecycleOwner) { pets ->
            pets ?: return@observe
            showCelebration(pets)
        }

        // 页面重建时恢复状态（如旋转屏幕）
        if (FocusService.isRunning) renderRunning(null) else renderIdle()
        refreshPetMood()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(FocusService.BROADCAST_TICK)
            addAction(FocusService.BROADCAST_FINISHED)
            addAction(FocusService.BROADCAST_FAILED)
            addAction(FocusService.BROADCAST_CANCELLED)
        }
        // API 33+ 必须显式声明 RECEIVER_NOT_EXPORTED（包内定向广播）
        ContextCompat.registerReceiver(
            requireContext(), timerReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(timerReceiver)
    }

    // ---------------- 切后台 5 秒判定（核心技术点） ----------------

    /** 失去焦点：启动 5 秒倒计时，期间不回来就通知服务判定失败 */
    override fun onPause() {
        super.onPause()
        // 锁机模式已接管会话时不判失败（用户在 App 内被屏幕固定锁住）
        if (FocusService.isRunning && !FocusService.lockActive) {
            bgHandler.postDelayed(failRunnable, BACKGROUND_TOLERANCE_MS)
        }
    }

    /** 重新获得焦点：撤销失败判定 */
    override fun onResume() {
        super.onResume()
        bgHandler.removeCallbacks(failRunnable)
        // 后台期间积压的庆祝弹窗，回来再放
        pendingCelebration?.let { showCelebration(it); pendingCelebration = null }
    }

    private fun notifyServiceFail() {
        val context = context ?: return
        // 此时 App 虽在后台，但因持有前台服务，进程具有前台优先级，允许 startService
        context.startService(
            Intent(context, FocusService::class.java).setAction(FocusService.ACTION_FAIL)
        )
    }

    // ---------------- 计时控制 ----------------

    private fun tryStartFocus() {
        if (FocusService.isRunning) {
            confirmCancel()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startFocus()
        }
    }

    private fun applyCustomMinutes() {
        if (FocusService.isRunning) return
        val input = binding.etCustomMinutes.text.toString().trim()
        val minutes = input.toIntOrNull()
        if (minutes == null || minutes < 1 || minutes > 180) {
            Toast.makeText(requireContext(), "请输入 1~180 之间的分钟数", Toast.LENGTH_SHORT).show()
            return
        }
        selectedMinutes = minutes
        binding.tvRemaining.text = formatMillis(selectedMinutes * 60_000L)
        binding.toggleDuration.clearChecked()
    }

    // ---------------- 常用预设（内容 + 时长 组合） ----------------

    /** 把当前「专注内容 + 所选时长」存成预设 */
    private fun saveCurrentAsPreset() {
        if (FocusService.isRunning) return
        val content = binding.etFocusContent.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(requireContext(), "请先填写专注内容再保存预设", Toast.LENGTH_SHORT).show()
            return
        }
        FocusPresetManager.savePreset(requireContext(), content, selectedMinutes)
        loadPresets()
        Toast.makeText(
            requireContext(), "已保存预设：$content · ${selectedMinutes} 分钟", Toast.LENGTH_SHORT
        ).show()
    }

    private fun loadPresets() {
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) return
        lifecycleScope.launch(Dispatchers.IO) {
            val presets = FocusPresetManager.getPresets(requireContext())
            withContext(Dispatchers.Main) {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    populatePresetChips(presets)
                }
            }
        }
    }

    private fun populatePresetChips(presets: List<FocusPresetManager.Preset>) {
        binding.chipPresets.removeAllViews()
        if (presets.isEmpty()) {
            binding.tvPresetHint.visibility = View.GONE
            return
        }
        binding.tvPresetHint.visibility = View.VISIBLE
        for (p in presets) {
            val chip = Chip(requireContext()).apply {
                text = "${p.content} · ${p.minutes}分"
                isCheckable = false
                isCloseIconVisible = true
                closeIconContentDescription = "删除预设 ${p.content}"
                setOnClickListener { applyPreset(p) }
                setOnCloseIconClickListener {
                    FocusPresetManager.removePreset(requireContext(), p.content)
                    loadPresets()
                }
            }
            binding.chipPresets.addView(chip)
        }
    }

    /** 一键套用预设：内容填入输入框、时长设为预设值 */
    private fun applyPreset(p: FocusPresetManager.Preset) {
        if (FocusService.isRunning) return
        selectedMinutes = p.minutes
        binding.etFocusContent.setText(p.content)
        binding.etFocusContent.setSelection(p.content.length)
        binding.etCustomMinutes.setText(p.minutes.toString())
        binding.toggleDuration.clearChecked()
        binding.tvRemaining.text = formatMillis(selectedMinutes * 60_000L)
    }

    private fun startFocus() {
        // 读取本次专注内容（可空，留空则记录为空串，饼图里归为「未命名」）
        val content = binding.etFocusContent.text.toString().trim()
        val intent = Intent(requireContext(), FocusService::class.java).apply {
            action = FocusService.ACTION_START
            putExtra(FocusService.EXTRA_MINUTES, selectedMinutes)
            putExtra(FocusService.EXTRA_CONTENT, content)
        }
        ContextCompat.startForegroundService(requireContext(), intent)
        totalMillisAtStart = selectedMinutes * 60_000L
        renderRunning(null)
    }

    private fun confirmCancel() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("放弃本次专注？")
            .setMessage("已专注的时间不会计入积分")
            .setPositiveButton("放弃") { _, _ ->
                requireContext().startService(
                    Intent(requireContext(), FocusService::class.java)
                        .setAction(FocusService.ACTION_STOP)
                )
            }
            .setNegativeButton("继续专注", null)
            .show()
    }

    // ---------------- 结果处理 ----------------

    private fun onFinished(minutes: Int) {
        renderIdle()
        refreshPetMood()
        com.example.focuspets.util.AlertPlayer.play(requireContext())
        Toast.makeText(
            requireContext(), "🍅 专注完成！+$minutes 积分", Toast.LENGTH_SHORT
        ).show()
        // 积分结算后 → 检查是否有新宠物达标（阶段二 ViewModel 的同款查询逻辑）
        viewModel.checkNewlyAffordablePets()
    }

    private fun onFailed() {
        renderIdle()
        refreshPetMood()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("😿 专注失败")
            .setMessage("你切走超过了 5 秒，本次专注没有完成。\n宠物饿坏了，快完成一次专注哄哄它吧！")
            .setPositiveButton("知道了", null)
            .show()
    }

    /** 庆祝：积分达标的新宠物提示（去图鉴页解锁） */
    private fun showCelebration(pets: List<PetEntity>) {
        if (!isResumed) {           // 后台时先暂存，onResume 再弹
            pendingCelebration = pets
            return
        }
        val lines = pets.joinToString("\n") { "${it.emoji} ${it.name}（${it.unlockCost} 积分）" }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🎉 恭喜！有新宠物可以解锁啦")
            .setMessage("这次专注攒够了积分！快去图鉴领取你的新伙伴：\n\n$lines")
            .setPositiveButton("去图鉴看看") { _, _ ->
                (activity as? MainActivity)?.navigateToCollection()
            }
            .setNegativeButton("稍后再说", null)
            .show()
        viewModel.consumeCelebration()
    }

    // ---------------- 渲染 ----------------

    private fun renderRunning(remaining: Long?) {
        binding.btnStart.text = "放弃专注"
        binding.toggleDuration.isEnabled = false
        binding.etCustomMinutes.isEnabled = false
        binding.btnCustom.isEnabled = false
        binding.etFocusContent.isEnabled = false
        binding.tvSubtitle.text = "专注中…请保持 App 在前台"
        if (remaining != null) {
            binding.tvRemaining.text = formatMillis(remaining)
            val percent = if (totalMillisAtStart > 0)
                (((totalMillisAtStart - remaining) * 100) / totalMillisAtStart).toInt()
            else 0
            binding.progressFocus.setProgress(percent.coerceIn(0, 100))
        } else {
            binding.tvRemaining.text = formatMillis(totalMillisAtStart)
        }
    }

    private fun renderIdle() {
        binding.btnStart.text = "开始专注"
        binding.toggleDuration.isEnabled = true
        binding.etCustomMinutes.isEnabled = true
        binding.btnCustom.isEnabled = true
        binding.etFocusContent.isEnabled = true
        binding.tvSubtitle.text = "选择时长，开始一次专注"
        binding.tvRemaining.text = formatMillis(selectedMinutes * 60_000L)
        binding.progressFocus.setProgress(0)
    }

    private fun refreshPetMood() {
        val hungry = context?.let { PetCareState.isHungry(it) } ?: false
        binding.tvHungerWarning.visibility = if (hungry) View.VISIBLE else View.GONE
    }

    private fun formatMillis(ms: Long): String {
        val totalSeconds = ms / 1000
        return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    // ---------------- 专注内容复用 ----------------

    /** 读取历史专注内容：用最近一次内容预填输入框，并展示高频内容 Chip 供一键复用 */
    private fun loadContentHistory() {
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) return
        lifecycleScope.launch(Dispatchers.IO) {
            val latest = runCatching { repository.getLatestContent() }.getOrNull()
            val frequent = runCatching { repository.getFrequentContents(8) }.getOrNull().orEmpty()
            withContext(Dispatchers.Main) {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    if (!latest.isNullOrEmpty()) binding.etFocusContent.setText(latest)
                    populateChips(frequent)
                }
            }
        }
    }

    /** 用历史内容渲染 Chip，点击即把该内容填回输入框；点 × 可删除该内容的全部记录 */
    private fun populateChips(contents: List<String>) {
        binding.chipHistory.removeAllViews()
        if (contents.isEmpty()) {
            binding.tvHistoryHint.visibility = View.GONE
            return
        }
        binding.tvHistoryHint.visibility = View.VISIBLE
        for (text in contents) {
            val chip = Chip(requireContext()).apply {
                this.text = text
                isCheckable = false
                isCloseIconVisible = true
                closeIconContentDescription = "删除 $text"
                setOnClickListener {
                    binding.etFocusContent.setText(text)
                    binding.etFocusContent.setSelection(text.length)
                }
                setOnCloseIconClickListener { confirmDeleteContent(text) }
            }
            binding.chipHistory.addView(chip)
        }
    }

    /** 删除「最近用过」的某条内容：确认后删除其全部专注记录并刷新 */
    private fun confirmDeleteContent(content: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除专注内容？")
            .setMessage("将删除所有「$content」的专注记录（含对应积分）。此操作不可撤销。")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching { repository.deleteRecordsByContent(content) }
                    withContext(Dispatchers.Main) { loadContentHistory() }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bgHandler.removeCallbacks(failRunnable)
        _binding = null
    }

    companion object {
        private const val BACKGROUND_TOLERANCE_MS = 5_000L   // 切后台 5 秒宽限
    }
}

/**
 * 专注页 ViewModel：
 * - availablePoints 来自 Room LiveData（Service 写入专注记录后自动刷新）
 * - checkNewlyAffordablePets()：积分结算后检查「达标未解锁」的宠物 → 发庆祝事件
 */
class FocusViewModel(private val repository: PetRepository) : ViewModel() {

    val availablePoints: LiveData<Int> = repository.getAvailablePoints()

    private val _celebrationEvent = MutableLiveData<List<PetEntity>?>()
    val celebrationEvent: LiveData<List<PetEntity>?> = _celebrationEvent

    fun checkNewlyAffordablePets() {
        viewModelScope.launch {
            val pets = repository.getAffordableLockedPets()
            if (pets.isNotEmpty()) _celebrationEvent.value = pets
        }
    }

    fun consumeCelebration() {
        _celebrationEvent.value = null
    }
}

class FocusViewModelFactory(private val repository: PetRepository) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FocusViewModel(repository) as T
}
