package com.example.focuspets

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.focuspets.FocusPetsApp
import com.example.focuspets.databinding.ActivityMainBinding
import com.example.focuspets.debug.DebugHelper
import com.example.focuspets.model.Backgrounds
import com.example.focuspets.model.SettingsManager
import com.example.focuspets.ui.collection.FragmentCollection
import com.example.focuspets.ui.leaderboard.LeaderboardFragment
import com.example.focuspets.ui.focus.FragmentFocus
import com.example.focuspets.ui.home.HomeFragment
import com.example.focuspets.ui.lock.LockActivity
import com.example.focuspets.ui.onboarding.OnboardingActivity
import com.example.focuspets.ui.stats.FragmentStats
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.example.focuspets.badge.BadgeCelebration
import com.example.focuspets.service.FocusService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** 专注完成广播：检测到新解锁徽章时弹出成就海报（仅应用内广播） */
    private val badgeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == FocusService.BROADCAST_FINISHED) {
                BadgeCelebration.onFocusFinished(this@MainActivity)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 若上次崩溃留下堆栈文件，优先弹窗展示（方便无 Logcat 时把异常贴给开发者），
        // 并直接结束——避免继续往下走又触发同样的崩溃导致看不到堆栈。
        FocusPetsApp.consumeLastCrash(this)?.let { trace ->
            AlertDialog.Builder(this)
                .setTitle("上次崩溃信息（请截图发我）")
                .setMessage(trace)
                .setCancelable(false)
                .setPositiveButton("知道了") { _, _ -> finish() }
                .show()
            return
        }

        // 首次启动（含清数据后重装）：先走新手引导，引导结束会自己回到这里
        if (!SettingsManager.isOnboardingSeen(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // test 分支：首次启动自动满配 + 积分补到 100 万（仅一次）
        DebugHelper.ensureTestSetup(this)

        // 统一窗口背景为用户选中的背景色（各 Fragment 根布局也会各自应用）
        val bg = Backgrounds.colorInt(this)
        window.decorView.setBackgroundColor(bg)
        // 状态栏 / 导航栏跟随选中背景色，消除顶部紫色边框；浅底用深色状态栏图标
        window.statusBarColor = bg
        window.navigationBarColor = bg
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            selectTab(item.itemId)
        }

        // 专注完成后检测新解锁徽章，弹成就海报（仅应用内广播）
        ContextCompat.registerReceiver(
            this, badgeReceiver,
            IntentFilter(FocusService.BROADCAST_FINISHED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_home   // 首页：宠物互动
        }
    }

    private fun selectTab(itemId: Int): Boolean {
        // 锁机模式是独立全屏 Activity（屏幕固定），不替换容器里的 Fragment
        if (itemId == R.id.nav_lock) {
            // 专注进行中不允许进入锁机：避免两套计时叠加，也避免「点了能进但能退出」的歧义
            if (FocusService.isRunning) {
                Toast.makeText(
                    this, "请先结束当前专注，再进入锁机模式", Toast.LENGTH_SHORT
                ).show()
                return true
            }
            startActivity(Intent(this, LockActivity::class.java))
            return true
        }
        val fragment = when (itemId) {
            R.id.nav_focus -> FragmentFocus()
                R.id.nav_collection -> FragmentCollection()
                R.id.nav_stats -> FragmentStats()
            else -> HomeFragment()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        return true
    }

    /** 统计页的「查看全球排行榜」入口：直接把排行榜 Fragment 放进容器 */
    fun showLeaderboard() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LeaderboardFragment())
            .commit()
    }

    /** 供专注完成后的庆祝弹窗 / 首页图鉴按钮调用：一键跳到图鉴页 */
    fun navigateToCollection() {
        binding.bottomNav.selectedItemId = R.id.nav_collection
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(badgeReceiver) }
    }
}
