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
 * Pet card icons: unlocked cats/dogs use equipped artwork; locked ones use grayscale cat/gray.png or
 * dog/long_gray.png. IDs 3-9 render emoji, grayscale when locked. Artwork may exceed the roughly 1 MB
 * Binder limit after base64 encoding, so downsample and compress data URIs before
 * WebView.evaluateJavascript injection.
 */
object PetCardIcon {

    /** PNG byte limit for WebView injection; base64 adds roughly 1.37x, leaving Binder headroom. */
    private const val MAX_PNG_BYTES = 200 * 1024

    /** Maximum decoded image edge, sufficient for grid thumbnails and card textures. */
    private const val DECODE_MAX_DIM = 768

    /** Build a single pet's JSON for setupCard in card.html. */
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

    /** Create a clear size-pixel Bitmap for the collection grid. */
    fun bitmap(context: Context, pet: PetEntity, unlocked: Boolean, size: Int = 256): Bitmap? {
        val src = rawBitmap(context, pet, unlocked) ?: return null
        return scaleToMax(src, size)
    }

    // ---- Internals ----

    /** Pair of kind and compact data URI for WebView injection. */
    private fun buildIcon(context: Context, pet: PetEntity, unlocked: Boolean): Pair<String, String?> {
        val kind = when (pet.id) {
            CatWardrobe.CAT_PET_ID, DogWardrobe.DOG_PET_ID -> "sprite"
            else -> "emoji"
        }
        return kind to webDataUri(context, pet, unlocked)
    }

    /** Downsampled cat/dog artwork or emoji bitmap for IDs 3-9, optionally grayscale. */
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

    /** Adaptively shrink artwork into a data URI that fits the Binder limit after base64 encoding. */
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

    /** Decode with inSampleSize instead of loading the full 1.5 MB image into memory. */
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

    /** Preserve aspect ratio and cap the longest edge at maxDim; return unchanged if already small. */
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

    /** Remove development notes about missing artwork, leaving only the player-facing description. */
    private fun cleanDesc(s: String): String =
        s.replace(Regex("（[^）]*(暂未实装|未实装|暂无立绘|待实装)[^）]*）"), "")
            .replace(Regex("\\([^)]*(暂未实装|未实装|暂无立绘|待实装)[^)]*\\)"), "")
            .trim()

    private fun jstr(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}
