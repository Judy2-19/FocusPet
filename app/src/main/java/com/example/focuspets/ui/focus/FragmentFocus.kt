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

    /** Background grace period: returning within five seconds is allowed; longer means failure. */
    private val bgHandler = Handler(Looper.getMainLooper())
    private val failRunnable = Runnable { notifyServiceFail() }

    private var selectedMinutes = 25
    private var totalMillisAtStart = 0L

    /** Local repository for content history, shared with the ViewModel. */
    private val repository by lazy {
        PetRepository(AppDatabase.getInstance(requireActivity().applicationContext))
    }

    /** Queue celebration events received in the background until the foreground resumes. */
    private var pendingCelebration: List<PetEntity>? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startFocus() else {
            Toast.makeText(
                requireContext(), getString(com.example.focuspets.R.string.core_message_3), Toast.LENGTH_SHORT
            ).show()
            startFocus()   // Timing still works without notification permission; only the notification is hidden.
        }
    }

    /** Receive timer broadcasts: tick, completed, failed, or user-cancelled. */
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

        // Apply the selected background color.
        Backgrounds.apply(requireContext(), binding.root)

        // Top-right records entry opens date-filtered records and the pie chart.
        binding.btnOpenRecords.setOnClickListener {
            startActivity(Intent(requireContext(), FocusRecordsActivity::class.java))
        }

        // Load historical content for reuse.
        loadContentHistory()

        // Load reusable content/duration presets.
        loadPresets()

        // Save the current content and duration as a preset.
        binding.btnSavePreset.setOnClickListener { saveCurrentAsPreset() }

        // Single-choice duration selection with MaterialButtonToggleGroup.
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

        // Custom duration.
        binding.btnCustom.setOnClickListener { applyCustomMinutes() }
        binding.etCustomMinutes.setOnEditorActionListener { _, _, _ ->
            applyCustomMinutes()
            true
        }

        // One button switches between Start and Cancel based on state.
        binding.btnStart.setOnClickListener { tryStartFocus() }

        // Room LiveData points update automatically when a session is inserted.
        viewModel.availablePoints.observe(viewLifecycleOwner) {
            binding.tvPointsFocus.text = getString(com.example.focuspets.R.string.core_message_4, it)
        }

        // New-pet celebration event.
        viewModel.celebrationEvent.observe(viewLifecycleOwner) { pets ->
            pets ?: return@observe
            showCelebration(pets)
        }

        // Restore state after recreation, such as screen rotation.
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
        // API 33+ requires explicit RECEIVER_NOT_EXPORTED for app-targeted broadcasts.
        ContextCompat.registerReceiver(
            requireContext(), timerReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(timerReceiver)
    }

    // ---------------- Five-second background failure check ----------------

    /** On losing focus, start a five-second timer and notify the service of failure if the user does not return. */
    override fun onPause() {
        super.onPause()
        // Skip failure while lock mode owns the session and keeps the user pinned inside the app.
        if (FocusService.isRunning && !FocusService.lockActive) {
            bgHandler.postDelayed(failRunnable, BACKGROUND_TOLERANCE_MS)
        }
    }

    /** On regaining focus, cancel the pending failure check. */
    override fun onResume() {
        super.onResume()
        bgHandler.removeCallbacks(failRunnable)
        // Display celebrations queued while backgrounded.
        pendingCelebration?.let { showCelebration(it); pendingCelebration = null }
    }

    private fun notifyServiceFail() {
        val context = context ?: return
        // The foreground service gives the process foreground priority, allowing startService even while the app is backgrounded.
        context.startService(
            Intent(context, FocusService::class.java).setAction(FocusService.ACTION_FAIL)
        )
    }

    // ---------------- Timer control ----------------

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
            Toast.makeText(requireContext(), getString(com.example.focuspets.R.string.core_message_5), Toast.LENGTH_SHORT).show()
            return
        }
        selectedMinutes = minutes
        binding.tvRemaining.text = formatMillis(selectedMinutes * 60_000L)
        binding.toggleDuration.clearChecked()
    }

    // ---------------- Reusable content/duration presets ----------------

    /** Save current content and selected duration as a preset. */
    private fun saveCurrentAsPreset() {
        if (FocusService.isRunning) return
        val content = binding.etFocusContent.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(requireContext(), getString(com.example.focuspets.R.string.core_message_6), Toast.LENGTH_SHORT).show()
            return
        }
        FocusPresetManager.savePreset(requireContext(), content, selectedMinutes)
        loadPresets()
        Toast.makeText(
            requireContext(), getString(com.example.focuspets.R.string.core_message_7, content, selectedMinutes), Toast.LENGTH_SHORT
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
                text = getString(com.example.focuspets.R.string.core_message_8, p.content, p.minutes)
                isCheckable = false
                isCloseIconVisible = true
                closeIconContentDescription = getString(com.example.focuspets.R.string.core_message_9, p.content)
                setOnClickListener { applyPreset(p) }
                setOnCloseIconClickListener {
                    FocusPresetManager.removePreset(requireContext(), p.content)
                    loadPresets()
                }
            }
            binding.chipPresets.addView(chip)
        }
    }

    /** Apply a preset by filling the content field and selecting its duration. */
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
        // Read optional session content; blank stays empty and appears as unnamed in the chart.
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
            .setTitle(getString(com.example.focuspets.R.string.core_message_10))
            .setMessage(getString(com.example.focuspets.R.string.core_message_11))
            .setPositiveButton(getString(com.example.focuspets.R.string.core_message_12)) { _, _ ->
                requireContext().startService(
                    Intent(requireContext(), FocusService::class.java)
                        .setAction(FocusService.ACTION_STOP)
                )
            }
            .setNegativeButton(getString(com.example.focuspets.R.string.core_message_13), null)
            .show()
    }

    // ---------------- Result handling ----------------

    private fun onFinished(minutes: Int) {
        renderIdle()
        refreshPetMood()
        com.example.focuspets.util.AlertPlayer.play(requireContext())
        Toast.makeText(
            requireContext(), getString(com.example.focuspets.R.string.core_message_14, minutes), Toast.LENGTH_SHORT
        ).show()
        // After settling points, check for newly affordable pets using the ViewModel query logic.
        viewModel.checkNewlyAffordablePets()
    }

    private fun onFailed() {
        renderIdle()
        refreshPetMood()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(com.example.focuspets.R.string.core_message_15))
            .setMessage(getString(com.example.focuspets.R.string.core_message_16))
            .setPositiveButton(getString(com.example.focuspets.R.string.core_message_17), null)
            .show()
    }

    /** Celebrate newly affordable pets and direct users to Collection to unlock them. */
    private fun showCelebration(pets: List<PetEntity>) {
        if (!isResumed) {           // Queue while backgrounded; display in onResume.
            pendingCelebration = pets
            return
        }
        val lines = pets.joinToString("\n") { getString(com.example.focuspets.R.string.core_message_18, it.emoji, com.example.focuspets.model.UiStrings.petName(it.id, it.name), it.unlockCost) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(com.example.focuspets.R.string.core_message_19))
            .setMessage(getString(com.example.focuspets.R.string.core_message_20, lines))
            .setPositiveButton(getString(com.example.focuspets.R.string.core_message_21)) { _, _ ->
                (activity as? MainActivity)?.navigateToCollection()
            }
            .setNegativeButton(getString(com.example.focuspets.R.string.ui_106), null)
            .show()
        viewModel.consumeCelebration()
    }

    // ---------------- Rendering ----------------

    private fun renderRunning(remaining: Long?) {
        binding.btnStart.text = getString(com.example.focuspets.R.string.core_message_22)
        binding.toggleDuration.isEnabled = false
        binding.etCustomMinutes.isEnabled = false
        binding.btnCustom.isEnabled = false
        binding.etFocusContent.isEnabled = false
        binding.tvSubtitle.text = getString(com.example.focuspets.R.string.core_message_23)
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
        binding.btnStart.text = getString(com.example.focuspets.R.string.ui_067)
        binding.toggleDuration.isEnabled = true
        binding.etCustomMinutes.isEnabled = true
        binding.btnCustom.isEnabled = true
        binding.etFocusContent.isEnabled = true
        binding.tvSubtitle.text = getString(com.example.focuspets.R.string.ui_128)
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

    // ---------------- Reusing session content ----------------

    /** Prefill the latest content and show frequent-content chips for one-tap reuse. */
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

    /** Render history chips: tap to fill content; tap the close icon to delete all records with that content. */
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
                closeIconContentDescription = getString(com.example.focuspets.R.string.core_message_24, text)
                setOnClickListener {
                    binding.etFocusContent.setText(text)
                    binding.etFocusContent.setSelection(text.length)
                }
                setOnCloseIconClickListener { confirmDeleteContent(text) }
            }
            binding.chipHistory.addView(chip)
        }
    }

    /** Confirm deletion of all records for a recent content value, then refresh. */
    private fun confirmDeleteContent(content: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(com.example.focuspets.R.string.core_message_25))
            .setMessage(getString(com.example.focuspets.R.string.core_message_26, content))
            .setPositiveButton(getString(com.example.focuspets.R.string.core_message_27)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching { repository.deleteRecordsByContent(content) }
                    withContext(Dispatchers.Main) { loadContentHistory() }
                }
            }
            .setNegativeButton(getString(com.example.focuspets.R.string.core_message_28), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bgHandler.removeCallbacks(failRunnable)
        _binding = null
    }

    companion object {
        private const val BACKGROUND_TOLERANCE_MS = 5_000L   // Five-second background grace period.
    }
}

/**
 * Focus ViewModel: availablePoints observes Room updates after service inserts;
 * checkNewlyAffordablePets emits celebrations for affordable locked pets after settlement.
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
