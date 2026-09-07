package com.example.focuspets

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.focuspets.databinding.ActivityMainBinding
import com.example.focuspets.debug.DebugHelper
import com.example.focuspets.model.Backgrounds
import com.example.focuspets.ui.collection.FragmentCollection
import com.example.focuspets.ui.focus.FragmentFocus
import com.example.focuspets.ui.home.HomeFragment
import com.example.focuspets.ui.lock.LockActivity
import com.example.focuspets.ui.stats.FragmentStats

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // test 分支：首次启动自动满配 + 积分补到 100 万（仅一次）
        DebugHelper.ensureTestSetup(this)

        // 统一窗口背景为用户选中的背景色（各 Fragment 根布局也会各自应用）
        window.decorView.setBackgroundColor(Backgrounds.colorInt(this))

        binding.bottomNav.setOnItemSelectedListener { item ->
            selectTab(item.itemId)
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_home   // 首页：宠物互动
        }
    }

    private fun selectTab(itemId: Int): Boolean {
        // 锁机模式是独立全屏 Activity（屏幕固定），不替换容器里的 Fragment
        if (itemId == R.id.nav_lock) {
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

    /** 供专注完成后的庆祝弹窗 / 首页图鉴按钮调用：一键跳到图鉴页 */
    fun navigateToCollection() {
        binding.bottomNav.selectedItemId = R.id.nav_collection
    }
}
