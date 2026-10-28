package com.example.focuspets.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.focuspets.R
import com.example.focuspets.cloud.CloudSyncManager
import com.example.focuspets.databinding.ActivitySettingsBinding
import com.example.focuspets.model.Backgrounds
import com.example.focuspets.model.SettingsManager
import com.example.focuspets.ui.onboarding.OnboardingActivity
import com.example.focuspets.user.UserDataManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Settings for nickname, sign-out, progress reset, and onboarding. Confirm sign-out/reset: sign-out
 * removes the anonymous cloud identity but retains local progress; reset clears records, collection,
 * wardrobe, mood, and identity to prevent cloud restoration. Finish back to Home so Room LiveData
 * displays initial state.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    /** Select custom alert audio and save its persistently granted URI. */
    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            // On cancellation, fall back to vibration if no custom audio was previously selected.
            if (SettingsManager.getAlertRingtoneUri(this).isNullOrEmpty()) {
                SettingsManager.setAlertMode(this, SettingsManager.ALERT_VIBRATE)
                syncAlertToggle()
            }
            return@registerForActivityResult
        }
        runCatching {
            // Request persistent read access to keep the URI usable after reopening.
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        SettingsManager.setAlertRingtoneUri(this, uri.toString())
        SettingsManager.setAlertMode(this, SettingsManager.ALERT_SOUND_CUSTOM)
        syncAlertToggle()
        Toast.makeText(this, "已设置自定义提醒音频", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Backgrounds.apply(this, binding.settingsRoot)
        window.decorView.setBackgroundColor(Backgrounds.colorInt(this))

        binding.btnBack.setOnClickListener { finish() }

        binding.tvVersion.text = "版本 ${appVersionName()}"

        binding.btnEditName.setOnClickListener { showRenameDialog() }
        binding.btnLogout.setOnClickListener { confirmLogout() }
        binding.btnReset.setOnClickListener { confirmReset() }
        binding.btnOnboarding.setOnClickListener { replayOnboarding() }

        setupThemeToggle()
        setupScreenPinToggle()
        setupAlertToggle()

        refreshAccountInfo()
    }

    override fun onResume() {
        super.onResume()
        // Refresh account details on return because the nickname may have changed elsewhere.
        refreshAccountInfo()
    }

    // ---------------- Account details ----------------

    private fun refreshAccountInfo() {
        binding.tvNickname.text = CloudSyncManager.currentName()
        val uid = CloudSyncManager.currentUid()
        binding.tvUid.text = if (uid.isNullOrBlank()) {
            "未登录（离线或同步未初始化）"
        } else {
            "匿名账号 ····${uid.takeLast(6)}"
        }
    }

    /** Change leaderboard nickname. */
    private fun showRenameDialog() {
        val input = EditText(this).apply {
            setText(CloudSyncManager.currentName())
            setSelection(text.length)
            hint = "2-12 个字符"
            filters = arrayOf(InputFilter.LengthFilter(12))
        }
        val container = FrameLayout(this).apply {
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(input, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("修改昵称")
            .setView(container)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val name = input.text.toString().trim()
                if (name.length < 2) {
                    Toast.makeText(this, "昵称至少 2 个字符", Toast.LENGTH_SHORT).show()
                } else {
                    CloudSyncManager.rename(name)
                    refreshAccountInfo()
                    Toast.makeText(this, "昵称已更新：$name", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    // ---------------- Sign out ----------------

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("注销云端账号？")
            .setMessage(
                "会清除本机绑定的匿名云端身份，下次启动将生成一个新的匿名账号。\n\n" +
                    "本机进度（专注记录、图鉴、妆扮）不会被删除。"
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("确认注销") { _, _ ->
                UserDataManager.logout(this) {
                    refreshAccountInfo()
                    Toast.makeText(this, "已注销，下次启动以新匿名账号同步", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .show()
    }

    // ---------------- Reset progress ----------------

    private fun confirmReset() {
        MaterialAlertDialogBuilder(this)
            .setTitle("重置全部进度？")
            .setMessage(
                "将清空：\n" +
                    "· 全部专注记录与积分\n" +
                    "· 已解锁的宠物图鉴（仅保留初始那只）\n" +
                    "· 猫咪妆扮购买记录\n" +
                    "· 宠物心情状态\n\n" +
                    "同时会注销云端账号，避免旧数据被同步回来。\n此操作不可撤销。"
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("确认重置") { _, _ ->
                UserDataManager.resetProgress(this) {
                    Toast.makeText(this, "进度已重置，回到初始状态", Toast.LENGTH_SHORT).show()
                    // Return to Home and let Room LiveData reload the initial state.
                    finish()
                }
            }
            .show()
    }

    // ---------------- Reopen onboarding ----------------

    /**
     * Clear the onboarding flag and open OnboardingActivity. finishAffinity also closes MainActivity
     * underneath to avoid two Home screens afterward.
     */
    private fun replayOnboarding() {
        SettingsManager.resetOnboarding(this)
        startActivity(Intent(this, OnboardingActivity::class.java))
        finishAffinity()
    }

    private fun appVersionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
    } catch (_: Exception) {
        "1.0"
    }

    // ---------------- Theme: light, dark, or system ----------------

    /** Initialize the theme segmented control before registering its listener to avoid unwanted recreation. */
    private fun setupThemeToggle() {
        val mode = SettingsManager.getThemeMode(this)
        // Set the initial selection before registering the listener, preventing unnecessary recreation.
        binding.themeToggle.check(
            when (mode) {
                SettingsManager.THEME_LIGHT -> R.id.theme_light
                SettingsManager.THEME_DARK -> R.id.theme_dark
                else -> R.id.theme_system
            }
        )
        // Then register the listener for user changes.
        binding.themeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener   // Ignore deselection events.
            val newMode = when (checkedId) {
                R.id.theme_light -> SettingsManager.THEME_LIGHT
                R.id.theme_dark -> SettingsManager.THEME_DARK
                else -> SettingsManager.THEME_SYSTEM
            }
            SettingsManager.setThemeMode(this, newMode)
            // Apply and recreate this page immediately so Settings also changes theme.
            AppCompatDelegate.setDefaultNightMode(
                SettingsManager.themeModeToNightMode(newMode)
            )
        }
    }

    // ---------------- Lock screen-pinning switch ----------------

    /** Screen pinning is enabled by default; disabling it prevents the system app-pinned prompt. */
    private fun setupScreenPinToggle() {
        binding.switchScreenPin.isChecked = SettingsManager.isScreenPinEnabled(this)
        binding.switchScreenPin.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setScreenPinEnabled(this, isChecked)
        }
    }

    // ---------------- Completion alerts: sound, vibration, custom audio ----------------

    private fun setupAlertToggle() {
        syncAlertToggle()
        binding.alertToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.alert_sound_default -> SettingsManager.ALERT_SOUND_DEFAULT
                R.id.alert_sound_custom -> SettingsManager.ALERT_SOUND_CUSTOM
                R.id.alert_none -> SettingsManager.ALERT_NONE
                else -> SettingsManager.ALERT_VIBRATE
            }
            if (mode == SettingsManager.ALERT_SOUND_CUSTOM &&
                SettingsManager.getAlertRingtoneUri(this).isNullOrEmpty()
            ) {
                // If custom audio is selected without a file, open the picker immediately.
                openAudioPicker()
            } else {
                SettingsManager.setAlertMode(this, mode)
            }
            syncAlertToggle()
        }
        binding.btnPickAudio.setOnClickListener { openAudioPicker() }
    }

    /** Refresh selection highlighting and decide whether to show the audio picker entry. */
    private fun syncAlertToggle() {
        val mode = SettingsManager.getAlertMode(this)
        val checkedId = when (mode) {
            SettingsManager.ALERT_SOUND_DEFAULT -> R.id.alert_sound_default
            SettingsManager.ALERT_SOUND_CUSTOM -> R.id.alert_sound_custom
            SettingsManager.ALERT_NONE -> R.id.alert_none
            else -> R.id.alert_vibrate
        }
        binding.alertToggle.check(checkedId)
        val showPick = mode == SettingsManager.ALERT_SOUND_CUSTOM
        binding.layoutPickAudio.visibility = if (showPick) View.VISIBLE else View.GONE
        val hasUri = !SettingsManager.getAlertRingtoneUri(this).isNullOrEmpty()
        binding.tvAudioName.text = if (showPick) {
            if (hasUri) "已选择自定义音频" else "尚未选择文件"
        } else ""
    }

    private fun openAudioPicker() {
        pickAudioLauncher.launch(arrayOf("audio/*"))
    }
}
