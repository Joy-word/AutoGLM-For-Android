package com.kevinluo.autoglm.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.kevinluo.autoglm.task.TaskExecutionManager
import com.kevinluo.autoglm.util.Logger

/**
 * NotificationListenerService that monitors all system notifications.
 *
 * When a notification arrives from an app that matches an enabled [NotificationTriggerRule],
 * this service attempts to start the configured task via [TaskExecutionManager].
 * If another task is already running, the trigger is silently discarded to avoid conflicts.
 *
 * The user must grant notification listener permission manually via system settings.
 */
class AutoGLMNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Logger.i(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Logger.w(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return

        val manager = NotificationTriggerManager.getInstance(applicationContext)
        val rule = manager.findMatchingRule(packageName) ?: return

        Logger.d(TAG, "Notification from $packageName matched rule '${rule.appLabel}'")

        if (TaskExecutionManager.isTaskRunning()) {
            Logger.w(TAG, "A task is already running, skipping notification trigger for $packageName")
            return
        }

        val blockReason = TaskExecutionManager.getStartTaskBlockReason()
        if (blockReason != TaskExecutionManager.StartTaskBlockReason.NONE) {
            Logger.w(TAG, "Cannot start task, block reason: $blockReason")
            return
        }

        Logger.i(TAG, "Triggering task for notification from ${rule.appLabel}: ${rule.taskPrompt.take(50)}")
        TaskExecutionManager.startTask(rule.taskPrompt)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Not needed for this feature
    }
}
