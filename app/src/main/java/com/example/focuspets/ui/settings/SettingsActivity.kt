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
 * 设置页：昵称 / 注销 / 重置进度 / 重新查看引导。
 *
 * 两个破坏性操作（注销、重置进度）都要求二次确认：
 * - 注销：清掉本机绑定的匿名云端身份，本地进度保留，下次启动生成新匿名账号。
 * - 重置进度：清空专注记录 / 图鉴 / 妆扮 / 心情，并顺带注销（否则云端会把旧数据合并回来）。
 *
 * 重置后本页直接 finish 回到主页，首页的 Room LiveData 会自动刷新为初始状态。
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    /** 选择自定义提醒音频：取持久授权 URI 后保存 */
    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            // 用户取消：若之前没有自定义音频，则回退到「仅震动」
            if (SettingsManager.getAlertRingtoneUri(this).isNullOrEmpty()) {
                SettingsManager.setAlertMode(this, SettingsManager.ALERT_VIBRATE)
                syncAlertToggle()
            }
            return@registerForActivityResult
        }
        runCatching {
            // 申请长期读取该 URI 的权限（重开后仍可用）
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
        // 从其它页面返回时同步一次账号信息（昵称可能在别处改过）
        refreshAccountInfo()
    }

    // ---------------- 账号信息 ----------------

    private fun refreshAccountInfo() {
        binding.tvNickname.text = CloudSyncManager.currentName()
        val uid = CloudSyncManager.currentUid()
        binding.tvUid.text = if (uid.isNullOrBlank()) {
            "未登录（离线或同步未初始化）"
        } else {
            "匿名账号 ····${uid.takeLast(6)}"
        }
    }

    /** 修改排行榜昵称 */
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

    // ---------------- 注销 ----------------

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

    // ---------------- 重置进度 ----------------

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
                    // 回到主页，让 Room LiveData 重新拉取初始数据
                    finish()
                }
            }
            .show()
    }

    // ---------------- 重新查看引导 ----------------

    /**
     * 重新查看引导：清掉「已看过」标记后进入 OnboardingActivity。
     * finishAffinity() 同时关掉下方的 MainActivity，避免引导结束后栈里出现两个主页。
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

    // ---------------- 主题模式（浅色 / 深色 / 跟随系统）----------------

    /** 初始化深色模式分段开关：先设初始选中（不触发监听），再注册切换监听 */
    private fun setupThemeToggle() {
        val mode = SettingsManager.getThemeMode(this)
        // 先设初始选中，此时尚未注册监听，避免触发一次无谓的重建
        binding.themeToggle.check(
            when (mode) {
                SettingsManager.THEME_LIGHT -> R.id.theme_light
                SettingsManager.THEME_DARK -> R.id.theme_dark
                else -> R.id.theme_system
            }
        )
        // 再注册监听，处理用户手动切换
        binding.themeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener   // 忽略「取消选中」事件
            val newMode = when (checkedId) {
                R.id.theme_light -> SettingsManager.THEME_LIGHT
                R.id.theme_dark -> SettingsManager.THEME_DARK
                else -> SettingsManager.THEME_SYSTEM
            }
            SettingsManager.setThemeMode(this, newMode)
            // 应用并立即重建当前页面，使设置页本身也即时切换深浅
            AppCompatDelegate.setDefaultNightMode(
                SettingsManager.themeModeToNightMode(newMode)
            )
        }
    }

    // ---------------- 锁机屏幕固定开关 ----------------

    /** 锁机「系统屏幕固定」开关：默认开启；关闭后锁机不再触发系统「应用已固定」弹窗 */
    private fun setupScreenPinToggle() {
        binding.switchScreenPin.isChecked = SettingsManager.isScreenPinEnabled(this)
        binding.switchScreenPin.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setScreenPinEnabled(this, isChecked)
        }
    }

    // ---------------- 结束提醒（铃声 / 震动 / 自定义音频）----------------

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
                // 选了自定义但还没选文件 → 直接打开文件选择器
                openAudioPicker()
            } else {
                SettingsManager.setAlertMode(this, mode)
            }
            syncAlertToggle()
        }
        binding.btnPickAudio.setOnClickListener { openAudioPicker() }
    }

    /** 根据当前设置刷新选项高亮，并决定是否显示「选择音频」入口 */
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
