package com.markfold.callrecorder

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat

/**
 * Auto-detects WhatsApp voice/video calls by watching WhatsApp's ongoing-call
 * notification, and starts/stops recording around it.
 *
 * Requires the user to grant "Notification access" in system settings (there is
 * a button for this in the app).
 *
 * As with everything else in this app: it records the MICROPHONE. To capture the
 * other person on a WhatsApp call, the call must be on SPEAKER -- Android sandboxes
 * WhatsApp's own audio stream and no third-party app can read it directly.
 */
class NotificationCallListener : NotificationListenerService() {

    private var activeCallKey: String? = null

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!autoEnabled()) return
        if (activeCallKey != null) return
        if (!isCallNotification(sbn)) return
        activeCallKey = sbn.key
        sendAction(RecordingService.ACTION_START)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.key == activeCallKey) {
            activeCallKey = null
            sendAction(RecordingService.ACTION_STOP)
        }
    }

    private fun isCallNotification(sbn: StatusBarNotification): Boolean {
        val pkg = sbn.packageName ?: return false
        val isVoip = pkg.startsWith("com.whatsapp")
        if (!isVoip) return false
        if (!sbn.isOngoing) return false  // ignore missed-call / message notifications

        val n = sbn.notification
        val isCallCategory = n.category == Notification.CATEGORY_CALL
        val extras = n.extras
        val text = buildString {
            append(extras?.getCharSequence(Notification.EXTRA_TITLE) ?: "")
            append(" ")
            append(extras?.getCharSequence(Notification.EXTRA_TEXT) ?: "")
        }
        return isCallCategory || text.contains("call", ignoreCase = true)
    }

    private fun autoEnabled(): Boolean =
        getSharedPreferences("cfg", Context.MODE_PRIVATE).getBoolean("auto", false)

    private fun sendAction(action: String) {
        val i = Intent(this, RecordingService::class.java).apply { this.action = action }
        try {
            ContextCompat.startForegroundService(this, i)
        } catch (e: Exception) {
            // Background FGS start can be blocked on Android 12+; best-effort.
        }
    }
}
