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

    /** App-internal focus-completion receiver: display achievement posters for newly unlocked badges. */
    private val badgeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == FocusService.BROADCAST_FINISHED) {
                BadgeCelebration.onFocusFinished(this@MainActivity)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display the saved crash trace first, allowing diagnosis without Logcat.
        // Do not immediately run normal initialization, which could crash again and hide the trace.
        FocusPetsApp.consumeLastCrash(this)?.let { trace ->
            AlertDialog.Builder(this)
                .setTitle(getString(com.example.focuspets.R.string.core_message_44))
                .setMessage(trace)
                .setCancelable(false)
                .setPositiveButton(getString(com.example.focuspets.R.string.core_message_17)) { _, _ -> finish() }
                .show()
            return
        }

        // Open onboarding on first launch; complete it before returning to the app.
        if (!SettingsManager.isOnboardingSeen(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Test branch: unlock everything and top up to one million points once on first launch.
        // Both a Debug build and IS_TEST_BUILD=true are required.
        // main defaults to IS_TEST_BUILD=false, so it injects no test data, matching release behavior.
        // For test builds: ./gradlew assembleDebug -PisTestBuild=true
        // BuildConfig.DEBUG is automatically false in release builds as a second safeguard.
        if (BuildConfig.DEBUG && BuildConfig.IS_TEST_BUILD) {
            DebugHelper.ensureTestSetup(this)
        }

        // Apply the selected background color to the window; fragment roots also apply it.
        val bg = Backgrounds.colorInt(this)
        window.decorView.setBackgroundColor(bg)
        // Match status/navigation bars to the selected background; use dark status icons on light backgrounds.
        window.statusBarColor = bg
        window.navigationBarColor = bg
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            selectTab(item.itemId)
        }

        // Check for newly unlocked badges after focus completion and show posters via an internal broadcast.
        ContextCompat.registerReceiver(
            this, badgeReceiver,
            IntentFilter(FocusService.BROADCAST_FINISHED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_home   // Home: pet interactions.
        }
    }

    private fun selectTab(itemId: Int): Boolean {
        // Lock mode uses a separate full-screen Activity with screen pinning; do not replace the current fragment.
        if (itemId == R.id.nav_lock) {
            // Disable bottom navigation during focus to prevent timer disruption and impossible-to-exit states.
            if (FocusService.isRunning) {
                Toast.makeText(
                    this, getString(com.example.focuspets.R.string.core_message_45), Toast.LENGTH_SHORT
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

    /** Open the leaderboard fragment from the statistics screen entry point. */
    fun showLeaderboard() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LeaderboardFragment())
            .commit()
    }

    /** Navigate to Collection from completion celebrations or the Home collection button. */
    fun navigateToCollection() {
        binding.bottomNav.selectedItemId = R.id.nav_collection
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(badgeReceiver) }
    }
}
