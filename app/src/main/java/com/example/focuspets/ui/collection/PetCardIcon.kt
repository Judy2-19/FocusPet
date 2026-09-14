package com.example.focuspets.ui.collection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Base64
import com.example.focuspets.db.entity.PetEntity
import com.example.focuspets.model.CatWardrobe
import com.example.focuspets.model.DogWardrobe
import com.example.focuspets.model.PetCatalog
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 宠物卡牌图标生成器。
 *
 * 图标规则（按用户要求）：
 * - 猫(1) / 狗(2)：
 *     · 已解锁 → 当前装扮立绘（CatWardrobe.assetPath / DogWardrobe.assetPath，如「粉色小猫穿裙子」）；
 *     · 未解锁 → 初始颜色基础立绘（猫=cat/gray.png，狗=dog/long_gray.png）并置灰；
 * - 3~9 号：emoji 渲染成图，未解锁置灰。
 *
 * ⚠️ 关键：猫咪立绘最大 ~1.5MB、狗 ~1MB，base64 后约 2MB，而
 * `WebView.evaluateJavascript` 走 Binder（上限约 1MB）会直接失败 → 卡片拿不到立绘。
 * 所以注入 WebView 前必须**降采样 + 压缩**，把 data URI 压到安全线内。
 */
object PetCardIcon {

    /** 注入 WebView 的 PNG 字节上限（base64 ≈ 1.37×；留足 Binder 余量） */
    private const val MAX_PNG_BYTES = 200 * 1024

    /** 解码时就地采样，最大边不超过该值（够网格缩略图 + 卡片贴图用） */
    private const val DECODE_MAX_DIM = 768

    /** 生成单只宠物的卡牌 JSON（供 card.html 的 setupCard 使用） */
    fun singlePayload(context: Context, pet: PetEntity, unlocked: Boolean): String {
        val (kind, uri) = buildIcon(context, pet, unlocked)
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"id\":").append(pet.id).append(",")
        sb.append("\"name\":").append(jstr(pet.name)).append(",")
        sb.append("\"rarity\":").append(jstr(pet.rarity.name)).append(",")
        sb.append("\"locked\":").append(if (unlocked) "false" else "true").append(",")
        sb.append("\"unlockCost\":").append(pet.unlockCost).append(",")
        sb.append("\"kind\":").append(jstr(kind)).append(",")
        sb.append("\"desc\":").append(jstr(cleanDesc(pet.description))).append(",")
        sb.append("\"imageDataUri\":").append(jstr(uri ?: ""))
        sb.append("}")
        return sb.toString()
    }

    /** 生成图鉴网格用的 Bitmap（清晰缩略图，size 像素） */
    fun bitmap(context: Context, pet: PetEntity, unlocked: Boolean, size: Int = 256): Bitmap? {
        val src = rawBitmap(context, pet, unlocked) ?: return null
        return scaleToMax(src, size)
    }

    // ---- 内部 ----

    /** (kind, 注入 WebView 的小体积 data URI) */
    private fun buildIcon(context: Context, pet: PetEntity, unlocked: Boolean): Pair<String, String?> {
        val kind = when (pet.id) {
            CatWardrobe.CAT_PET_ID, DogWardrobe.DOG_PET_ID -> "sprite"
            else -> "emoji"
        }
        return kind to webDataUri(context, pet, unlocked)
    }

    /** 原始立绘（猫/狗）或 emoji 位图（3~9），已按需置灰；已降采样解码 */
    private fun rawBitmap(context: Context, pet: PetEntity, unlocked: Boolean): Bitmap? =
        when (pet.id) {
            CatWardrobe.CAT_PET_ID -> {
                val path = if (unlocked) CatWardrobe.assetPath(context) else "cat/gray.png"
                decodeAsset(context, path, desaturate = !unlocked)
            }
            DogWardrobe.DOG_PET_ID -> {
                val path = if (unlocked) DogWardrobe.assetPath(context) else "dog/long_gray.png"
                decodeAsset(context, path, desaturate = !unlocked)
            }
            else -> emojiBitmap(PetCatalog.emoji(pet.id), desaturate = !unlocked)
        }

    /** 把立绘压成小体积 data URI（自适应缩小，确保 base64 不超 Binder 限制） */
    private fun webDataUri(context: Context, pet: PetEntity, unlocked: Boolean): String? {
        val src = rawBitmap(context, pet, unlocked) ?: return null
        var maxDim = 512
        while (true) {
            val scaled = scaleToMax(src, maxDim)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 100, baos)
            if (baos.size() <= MAX_PNG_BYTES || maxDim <= 160) {
                return "data:image/png;base64," +
                    Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            }
            maxDim = maxDim * 3 / 4
        }
    }

    /** 分采样解码（inSampleSize），避免把 1.5MB 大图整幅读进内存 */
    private fun decodeAsset(context: Context, assetPath: String, desaturate: Boolean): Bitmap? {
        val bmp = try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            val longest = max(bounds.outWidth, bounds.outHeight)
            while (longest / sample > DECODE_MAX_DIM) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (e: Exception) {
            null
        } ?: return null
        return if (desaturate) grayscale(bmp) else bmp
    }

    /** 等比缩放到最大边 = maxDim（已足够小则原样返回） */
    private fun scaleToMax(src: Bitmap, maxDim: Int): Bitmap {
        val longest = max(src.width, src.height)
        if (longest <= maxDim) return src
        val scale = maxDim.toFloat() / longest
        val w = max(1, (src.width * scale).roundToInt())
        val h = max(1, (src.height * scale).roundToInt())
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private fun emojiBitmap(emoji: String, desaturate: Boolean): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val p = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = size * 0.72f
            color = Color.BLACK
        }
        val fm = p.fontMetrics
        val y = size / 2f - (fm.ascent + fm.descent) / 2f
        cv.drawText(emoji, size / 2f, y, p)
        return if (desaturate) grayscale(bmp) else bmp
    }

    private fun grayscale(src: Bitmap): Bitmap {
        val cfg = src.config ?: Bitmap.Config.ARGB_8888
        val dst = Bitmap.createBitmap(src.width, src.height, cfg)
        val cv = Canvas(dst)
        val p = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        cv.drawBitmap(src, 0f, 0f, p)
        return dst
    }

    /** 去掉「（暂未实装立绘）」这类开发备注，只留给玩家看的简介 */
    private fun cleanDesc(s: String): String =
        s.replace(Regex("（[^）]*(暂未实装|未实装|暂无立绘|待实装)[^）]*）"), "")
            .replace(Regex("\\([^)]*(暂未实装|未实装|暂无立绘|待实装)[^)]*\\)"), "")
            .trim()

    private fun jstr(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}
