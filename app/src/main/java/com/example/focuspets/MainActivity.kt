package com.example.focuspets

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.focuspets.databinding.ActivityMainBinding
import com.example.focuspets.ui.collection.FragmentCollection
import com.example.focuspets.ui.focus.FragmentFocus
import com.example.focuspets.ui.home.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            selectTab(item.itemId)
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_home   // 首页：宠物互动
        }
    }

    private fun selectTab(itemId: Int): Boolean {
        val fragment = when (itemId) {
            R.id.nav_focus -> FragmentFocus()
            R.id.nav_collection -> FragmentCollection()
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
