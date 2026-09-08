package com.example.focuspets.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 专注预设：用户「本次专注内容 + 自定义时长」的组合，保存后可一键复用。
 * 例如：「写代码 / 50 分钟」「背单词 / 25 分钟」。
 * 用 SharedPreferences + JSON 存储（不引入额外依赖），最多保留 8 条，新的靠前。
 */
object FocusPresetManager {

    private const val PREFS = "focus_presets"
    private const val KEY = "presets"
    private const val MAX = 8

    data class Preset(val content: String, val minutes: Int)

    fun getPresets(context: Context): List<Preset> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        if (raw.isNullOrEmpty()) return emptyList()
        val arr = JSONArray(raw)
        val list = mutableListOf<Preset>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(Preset(o.getString("c"), o.getInt("m")))
        }
        list
    }.getOrDefault(emptyList())

    /** 保存一条预设（内容去空；相同内容则更新其时长并置顶），返回保存后的完整列表 */
    fun savePreset(context: Context, content: String, minutes: Int): List<Preset> {
        val c = content.trim()
        val m = minutes.coerceIn(1, 180)
        if (c.isEmpty()) return getPresets(context)
        val existing = getPresets(context).toMutableList()
        existing.removeAll { it.content == c }
        existing.add(0, Preset(c, m))
        val trimmed = existing.take(MAX)
        persist(context, trimmed)
        return trimmed
    }

    /** 删除一条预设 */
    fun removePreset(context: Context, content: String) {
        val remaining = getPresets(context).filter { it.content != content }
        persist(context, remaining)
    }

    private fun persist(context: Context, list: List<Preset>) {
        val arr = JSONArray()
        for (p in list) {
            arr.put(JSONObject().apply {
                put("c", p.content)
                put("m", p.minutes)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}
