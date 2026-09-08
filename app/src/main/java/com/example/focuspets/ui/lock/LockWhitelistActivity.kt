package com.example.focuspets.ui.lock

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.focuspets.R
import com.example.focuspets.databinding.ActivityLockWhitelistBinding
import com.example.focuspets.model.SettingsManager

/** 单条可勾选应用 */
data class AppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    var checked: Boolean
)

/**
 * 锁机白名单管理：列出本机所有「可启动」的应用（有 Launcher 入口），
 * 用户勾选锁机期间允许使用的 app，保存进 SettingsManager。
 */
class LockWhitelistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockWhitelistBinding
    private val items = mutableListOf<AppItem>()
    private val whitelist = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockWhitelistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        whitelist.addAll(SettingsManager.getLockWhitelist(this))

        binding.tvBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { saveAndExit() }

        loadApps()
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = AppAdapter()
    }

    private fun loadApps() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolve = pm.queryIntentActivities(intent, 0)
        items.clear()
        val seen = mutableSetOf<String>()
        for (ri in resolve) {
            val pkg = ri.activityInfo.packageName
            if (pkg == packageName) continue            // 排除本应用（锁机页本身永远放行）
            if (!seen.add(pkg)) continue               // 同一包多入口去重
            val appInfo = ri.activityInfo.applicationInfo
            val name = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            items.add(AppItem(pkg, name, icon, whitelist.contains(pkg)))
        }
        items.sortBy { it.appName.lowercase() }
    }

    private fun saveAndExit() {
        val selected = items.filter { it.checked }.map { it.packageName }.toSet()
        SettingsManager.setLockWhitelist(this, selected)
        finish()
    }

    private inner class AppAdapter : RecyclerView.Adapter<AppViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app_check, parent, false)
            return AppViewHolder(v)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            val item = items[position]
            holder.icon.setImageDrawable(item.icon)
            holder.name.text = item.appName
            holder.pkg.text = item.packageName
            holder.checkbox.isChecked = item.checked
            holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
                item.checked = isChecked
            }
            // 点击整行切换勾选（CheckBox 自身设为不可点，避免双击冲突）
            holder.itemView.setOnClickListener {
                val newState = !holder.checkbox.isChecked
                holder.checkbox.isChecked = newState
                item.checked = newState
            }
        }
    }

    private class AppViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.iv_icon)
        val name: TextView = v.findViewById(R.id.tv_name)
        val pkg: TextView = v.findViewById(R.id.tv_pkg)
        val checkbox: CheckBox = v.findViewById(R.id.cb_check)
    }
}
