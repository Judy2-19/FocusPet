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
 * Achievement celebration: detect newly unlocked badges after focus completion, generate shareable
 * posters, and show them once. SharedPreferences stores celebrated badge IDs for idempotence.
 * migrateExisting marks historical badges during upgrades. Posters use Canvas/Bitmap and FileProvider
 * content:// URIs for system sharing.
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

    /** Run once at startup to mark existing unlocked badges as celebrated and avoid an upgrade popup flood. */
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

    /** Return newly unlocked, not-yet-celebrated badges and immediately mark them celebrated for idempotence. */
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

    /** After focus completion, generate and display new achievement posters; return silently if there are none. */
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
     * Manual sharing from Statistics: render an unlocked badge poster with share/save actions. Unlike
     * onFocusFinished, do not update the celebrated set, allowing repeated sharing.
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

    /** Show posters sequentially; usually only one is unlocked at a time. */
    private fun showQueue(context: Context, pairs: List<Pair<Bitmap, Uri>>) {
        if (pairs.isEmpty()) return
        // Dialogs require a live Activity context; skip if the page has been destroyed.
        if (context is Activity && (context.isFinishing || context.isDestroyed)) return
        var index = 0
        fun showNext() {
            if (index >= pairs.size) return
            val (bmp, uri) = pairs[index]; index++
            showPosterDialog(context, bmp, uri) { showNext() }
        }
        showNext()
    }

    /** Achievement poster dialog: preview with Share and Close actions. */
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

    /** Share the poster through the system share sheet. */
    private fun sharePoster(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_106))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_136)))
    }

    /** Save the poster Bitmap in cache and expose it through a FileProvider content:// URI. */
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
     * Draw a 1080x1620 portrait poster: gradient background and circles, white card, app name/subtitle,
     * badge/emoji, title/description, divider, nickname/pet message, progress, and sharing footer.
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

        // Background gradient.
        val bg = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                Color.parseColor("#FFB300"), Color.parseColor("#FFF3D6"),
                Shader.TileMode.CLAMP
            )
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)

        // Decorative translucent circles.
        val deco = Paint().apply { color = Color.WHITE; alpha = 28 }
        c.drawCircle(w * 0.86f, h * 0.08f, 170f, deco)
        c.drawCircle(w * 0.10f, h * 0.94f, 220f, deco)

        // Central white card with a subtle shadow.
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
        drawCenter(c, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_071), cx, cardT + 96f, 46f, Color.parseColor("#FFB300"), bold = true)
        drawCenter(c, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_060), cx, cardT + 146f, 30f, Color.parseColor("#BDBDBD"), bold = false)

        // Large badge circle and emoji.
        val cy = cardT + 380f
        val r = 150f
        c.drawCircle(cx, cy, r, Paint().apply { color = Color.parseColor("#FFF3D6"); isAntiAlias = true })
        drawCenter(c, badge.emoji, cx, cy + 4f, 150f, Color.BLACK, bold = false, emoji = true)

        drawCenter(c, badge.title, cx, cy + r + 96f, 76f, Color.parseColor("#212121"), bold = true)
        drawCenter(c, badge.desc, cx, cy + r + 156f, 40f, Color.parseColor("#757575"), bold = false)

        // Divider, nickname, and pet message.
        val divY = cy + r + 220f
        c.drawLine(cx - 170f, divY, cx + 170f, divY,
            Paint().apply { color = Color.parseColor("#EEEEEE"); strokeWidth = 3f })
        val nick = if (nickname.isNotBlank()) nickname else com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_072)
        drawCenter(c, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_008 ,(nick).toString()), cx, divY + 84f, 46f, Color.parseColor("#212121"), bold = true)
        drawCenter(c, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_012 ,(petEmoji).toString()), cx, divY + 146f, 40f, Color.parseColor("#9E9E9E"), bold = false)

        // Footer.
        val dateStr = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())
        drawCenter(c, dateStr, cx, cardB - 76f, 30f, Color.parseColor("#BDBDBD"), bold = false)
        drawCenter(c, com.example.focuspets.model.UiStrings.text(com.example.focuspets.R.string.dynamic_086), cx, cardB - 36f, 30f, Color.parseColor("#BDBDBD"), bold = false)

        return bmp
    }

    /** Center text horizontally; center emoji vertically by glyph bounds, but treat y as the baseline for ordinary text. */
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
