package com.example.focuspets.ui.collection

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.focuspets.databinding.ActivityPetCardBinding
import com.example.focuspets.db.entity.PetEntity

/** 单只宠物的 3D 卡牌页：全屏 immersive WebView 承载 card.html（纯 three.js 单卡） */
class PetCardActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PET = "extra_pet"
        const val EXTRA_UNLOCKED = "extra_unlocked"
    }

    private lateinit var binding: ActivityPetCardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPetCardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemUi()

        @Suppress("DEPRECATION")
        val pet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(EXTRA_PET, PetEntity::class.java)
        else
            intent.getParcelableExtra(EXTRA_PET)

        val unlocked = intent.getBooleanExtra(EXTRA_UNLOCKED, false)
        if (pet == null) { finish(); return }

        val webView = binding.wvCard
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowFileAccessFromFileURLs = true
        webView.settings.allowUniversalAccessFromFileURLs = true
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val json = PetCardIcon.singlePayload(this@PetCardActivity, pet, unlocked)
                webView.evaluateJavascript("setupCard($json)", null)
            }
        }
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun closeCard() = finish()
        }, "Android")
        webView.loadUrl("file:///android_asset/card3d/card.html")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    private fun hideSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { ctrl ->
                ctrl.hide(
                    android.view.WindowInsets.Type.statusBars()
                        or android.view.WindowInsets.Type.navigationBars()
                )
                ctrl.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }

    override fun onDestroy() {
        binding.wvCard.destroy()
        super.onDestroy()
    }
}
