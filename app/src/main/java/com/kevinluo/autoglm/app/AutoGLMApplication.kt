package com.kevinluo.autoglm.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.kevinluo.autoglm.config.SystemPrompts
import com.kevinluo.autoglm.settings.SettingsManager
import com.kevinluo.autoglm.ui.FloatingWindowService
import com.kevinluo.autoglm.util.Logger

/**
 * Application class that manages app-wide lifecycle events.
 *
 * This class is responsible for:
 * - Tracking activity lifecycle to determine foreground/background state
 * - Managing floating window visibility based on app state
 * - Loading custom system prompts from settings on startup
 *
 * The floating window is automatically hidden when the app is in the foreground
 * and shown when the app goes to the background, providing a seamless user experience.
 *
 * Requirements: 1.1, 2.1, 2.2
 */
class AutoGLMApplication : Application() {

    /**
     * Counter tracking the number of started (visible) activities.
     * When this reaches 0, the app is considered to be in the background.
     */
    private var activityCount = 0

    override fun onCreate() {
        super.onCreate()

        // Load custom system prompts if set
        loadCustomSystemPrompts()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // No action needed
            }

            override fun onActivityStarted(activity: Activity) {
                activityCount++
                Logger.d(TAG, "Activity started: ${activity.localClassName}, count: $activityCount")

                // App came to foreground - hide floating window
                if (activityCount == 1) {
                    Logger.d(TAG, "App in foreground - hiding floating window")
                    FloatingWindowService.getInstance()?.hide()
                }
            }

            override fun onActivityResumed(activity: Activity) {
                // No action needed
            }

            override fun onActivityPaused(activity: Activity) {
                // No action needed
            }

            override fun onActivityStopped(activity: Activity) {
                activityCount--
                Logger.d(TAG, "Activity stopped: ${activity.localClassName}, count: $activityCount")

                // App went to background - show floating window
                if (activityCount == 0) {
                    Logger.d(TAG, "App in background - showing floating window")
                    FloatingWindowService.getInstance()?.show()
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                // No action needed
            }

            override fun onActivityDestroyed(activity: Activity) {
                // No action needed
            }
        })
    }

    /**
     * Loads custom system prompts from settings if they exist.
     *
     * Checks for custom Chinese and English system prompts stored in settings
     * and applies them to the SystemPrompts configuration.
     */
    private fun loadCustomSystemPrompts() {
        val settingsManager = SettingsManager(this)

        settingsManager.getCustomSystemPrompt("cn")?.let { prompt ->
            SystemPrompts.setCustomChinesePrompt(prompt)
            Logger.d(TAG, "Loaded custom Chinese system prompt")
        }

        settingsManager.getCustomSystemPrompt("en")?.let { prompt ->
            SystemPrompts.setCustomEnglishPrompt(prompt)
            Logger.d(TAG, "Loaded custom English system prompt")
        }
    }

    companion object {
        private const val TAG = "AutoGLMApplication"
    }
}
