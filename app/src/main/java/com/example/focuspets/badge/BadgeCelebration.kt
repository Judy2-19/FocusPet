package com.example.focuspets.badge

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.core.content.FileProvider
import com.example.focuspets.R
import com.example.focuspets.cloud.CloudSyncManager
import com.example.focuspets.db.AppDatabase
import com.example.focuspets.db.PetRepository
import com.example.focuspets.model.AchievementBadge
import com.example.focuspets.model.PetCatalog
import com.example.focuspets.model.SettingsManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log

/**
 * 成就徽章庆祝：专注完成后检测「新解锁」的徽章，生成成就海报并弹出，支持分享。
 *
 * 设计要点：
 * - 用 SharedPreferences 记录「已庆祝」的徽章 id，保证每个徽章只弹一次（幂等），
 *   避免统计页每次刷新都重复弹窗。
 * - migrateExisting() 在应用启动(首跑)时，把当前已解锁的徽章标记为已庆祝，
 *   防止老用户升级后一次性弹出一堆历史成就。
 * - onFocusFinished() 在专注完成广播后调用，仅当本次有新解锁徽章时才生成海报并弹出。
 * - 海报由 Canvas 绘制成 Bitmap，经 FileProvider 暴露为 content:// Uri 供系统分享。
 */
object BadgeCelebration {

    private const val PREFS = "badge_celebration"
    private const val KEY_CELEBRATED = "celebrated"
    private const val KEY_MIGRATED = "migrated_v1"

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, t ->
            Log.w("BadgeCelebration", "non-fatal: ${t.message}")
        }
    )

    /** 应用启动时调用一次：把当前已解锁的徽章标记为"已庆祝"，避免老用户更新后一次性弹出一堆 */
    suspend fun migrateExisting(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_MIGRATED, false)) return
        runCatching {
            val repo = PetRepository(AppDatabase.getInstance(context.applicationContext))
            val snaps = repo.getAchievementsSnapshot()
            val set = prefs.getStringSet(KEY_CELEBRATED, emptySet())?.toMutableSet()
                ?: mutableSetOf()
            snaps.filter { it.unlocked }.forEach { set.add(it.id) }
            prefs.edit().putStringSet(KEY_CELEBRATED, set).apply()
        }
        prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
    }

    /** 取回"本次新解锁、且尚未庆祝过"的徽章，并把它们标记为已庆祝（幂等） */
    private fun takeNewlyUnlocked(context: Context, current: List<AchievementBadge>): List<AchievementBadge> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_CELEBRATED, emptySet())?.toMutableSet()
            ?: mutableSetOf()
        val newly = current.filter { it.unlocked && !set.contains(it.id) }
        if (newly.isNotEmpty()) {
            newly.forEach { set.add(it.id) }
            prefs.edit().putStringSet(KEY_CELEBRATED, set).apply()
        }
        return newly
    }

    /** 专注完成后调用：检测新解锁徽章，生成海报并依次弹出（无新成就则安静返回） */
    fun onFocusFinished(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            runCatching {
                val repo = PetRepository(AppDatabase.getInstance(appContext))
                val snaps = repo.getAchievementsSnapshot()
                val newly = takeNewlyUnlocked(appContext, snaps)
                if (newly.isEmpty()) return@launch

                val nickname = CloudSyncManager.currentName()
                val petEmoji = PetCatalog.emoji(SettingsManager.getCurrentPetId(appContext))
                val pairs = newly.mapNotNull { badge ->
                    val bmp = generatePoster(badge, nickname, petEmoji)
                    val uri = saveToCache(appContext, bmp)
                    if (uri != null) bmp to uri else null
                }
                withContext(Dispatchers.Main) { showQueue(context, pairs) }
            }
        }
    }

    /**
     * 手动分享：统计页再次点击「已解锁」徽章时调用，生成该徽章的海报并弹出（带分享按钮）。
     * 与 onFocusFinished 不同，这里不写入「已庆祝」集合，可随时重复分享。
     */
    fun showPosterForBadge(context: Context, badge: AchievementBadge) {
        if (!badge.unlocked) return
        val appContext = context.applicationContext
        scope.launch {
            runCatching {
                val nickname = CloudSyncManager.currentName()
                val petEmoji = PetCatalog.emoji(SettingsManager.getCurrentPetId(appContext))
                val bmp = generatePoster(badge, nickname, petEmoji)
                val uri = saveToCache(appContext, bmp) ?: return@runCatching
                withContext(Dispatchers.Main) {
                    if (context is Activity && (context.isFinishing || context.isDestroyed)) return@withContext
                    showPosterDialog(context, bmp, uri) {}
                }
            }
        }
    }

    /** 依次弹出多张海报（通常一次只解锁 1 个，顺序弹更稳） */
    private fun showQueue(context: Context, pairs: List<Pair<Bitmap, Uri>>) {
        if (pairs.isEmpty()) return
        // 弹窗需要 Activity 上下文；页面已销毁则不弹
        if (context is Activity && (context.isFinishing || context.isDestroyed)) return
        var index = 0
        fun showNext() {
            if (index >= pairs.size) return
            val (bmp, uri) = pairs[index]; index++
            showPosterDialog(context, bmp, uri) { showNext() }
        }
        showNext()
    }

    /** 成就海报弹窗：展示海报图 + 分享 / 收下 两个按钮 */
    private fun showPosterDialog(context: Context, bmp: Bitmap, uri: Uri, onDismiss: () -> Unit) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_badge_poster, null)
        view.findViewById<ImageView>(R.id.iv_poster).setImageBitmap(bmp)
        view.findViewById<View>(R.id.btn_share).setOnClickListener {
            sharePoster(context, uri)
        }
        view.findViewById<View>(R.id.btn_close).setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener { onDismiss() }
        dialog.setContentView(view)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            val w = (context.resources.displayMetrics.widthPixels * 0.92).toInt()
            setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    /** 通过系统分享面板分享海报图片 */
    private fun sharePoster(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, "我的专注成就")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享成就海报"))
    }

    /** 把海报 Bitmap 存到 cache 目录，返回 FileProvider 的 content:// Uri */
    private fun saveToCache(context: Context, bmp: Bitmap): Uri? = runCatching {
        val dir = File(context.cacheDir, "posters")
        dir.mkdirs()
        val file = File(dir, "badge_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }.getOrNull()

    /**
     * 绘制成就海报（1080×1620）。内容：
     * 渐变背景 + 装饰圆 → 白色卡片 → 应用名/副标题 → 徽章大圆+emoji → 标题/描述
     * → 分隔线 → 昵称达成语 → 宠物陪伴语 → 日期/标语页脚。
     */
    private fun generatePoster(
        badge: AchievementBadge,
        nickname: String,
        petEmoji: String
    ): Bitmap {
        val w = 1080
        val h = 1620
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // 背景渐变
        val bg = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                Color.parseColor("#FFB300"), Color.parseColor("#FFF3D6"),
                Shader.TileMode.CLAMP
            )
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)

        // 装饰半透明圆
        val deco = Paint().apply { color = Color.WHITE; alpha = 28 }
        c.drawCircle(w * 0.86f, h * 0.08f, 170f, deco)
        c.drawCircle(w * 0.10f, h * 0.94f, 220f, deco)

        // 中央白色卡片 + 轻投影
        val cardL = 80f; val cardT = 210f; val cardR = w - 80f; val cardB = h - 210f
        c.drawRoundRect(
            cardL + 10f, cardT + 16f, cardR + 10f, cardB + 16f, 44f, 44f,
            Paint().apply { color = Color.argb(40, 0, 0, 0) }
        )
        c.drawRoundRect(
            cardL, cardT, cardR, cardB, 44f, 44f,
            Paint().apply { color = Color.WHITE; isAntiAlias = true }
        )

        val cx = w / 2f
        drawCenter(c, "专注养宠", cx, cardT + 96f, 46f, Color.parseColor("#FFB300"), bold = true)
        drawCenter(c, "FOCUS · 成就达成", cx, cardT + 146f, 30f, Color.parseColor("#BDBDBD"), bold = false)

        // 徽章大圆 + emoji
        val cy = cardT + 380f
        val r = 150f
        c.drawCircle(cx, cy, r, Paint().apply { color = Color.parseColor("#FFF3D6"); isAntiAlias = true })
        drawCenter(c, badge.emoji, cx, cy + 4f, 150f, Color.BLACK, bold = false, emoji = true)

        drawCenter(c, badge.title, cx, cy + r + 96f, 76f, Color.parseColor("#212121"), bold = true)
        drawCenter(c, badge.desc, cx, cy + r + 156f, 40f, Color.parseColor("#757575"), bold = false)

        // 分隔线 + 昵称/宠物语
        val divY = cy + r + 220f
        c.drawLine(cx - 170f, divY, cx + 170f, divY,
            Paint().apply { color = Color.parseColor("#EEEEEE"); strokeWidth = 3f })
        val nick = if (nickname.isNotBlank()) nickname else "专注者"
        drawCenter(c, "$nick 达成了这个成就", cx, divY + 84f, 46f, Color.parseColor("#212121"), bold = true)
        drawCenter(c, "$petEmoji 和它的专注伙伴一起成长", cx, divY + 146f, 40f, Color.parseColor("#9E9E9E"), bold = false)

        // 页脚
        val dateStr = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())
        drawCenter(c, dateStr, cx, cardB - 76f, 30f, Color.parseColor("#BDBDBD"), bold = false)
        drawCenter(c, "每一次专注，都在陪它长大", cx, cardB - 36f, 30f, Color.parseColor("#BDBDBD"), bold = false)

        return bmp
    }

    /** 居中绘制文字：emoji 时按字形垂直居中，普通文字时 y 当作基线 */
    private fun drawCenter(
        c: Canvas, text: String, cx: Float, y: Float, size: Float,
        color: Int, bold: Boolean, emoji: Boolean = false
    ) {
        val p = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = size
            this.color = color
            if (bold) isFakeBoldText = true
        }
        val fm = p.fontMetrics
        val baseline = if (emoji) y - (fm.ascent + fm.descent) / 2f else y
        c.drawText(text, cx, baseline, p)
    }
}
