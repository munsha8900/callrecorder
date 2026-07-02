package com.markfold.callrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * Auto-start/stop recording around regular phone calls, IF the user enabled
 * "auto record" in the app.
 *
 * Limitations (be aware):
 *  - Only fires for the regular Phone/Dialer app. WhatsApp / other VoIP apps
 *    do NOT broadcast PHONE_STATE, so those must be started manually.
 *  - On Android 12+, starting a foreground service from the background can be
 *    blocked by the OS; auto-record is therefore best-effort. Manual recording
 *    from the app is the reliable path.
 */
class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("cfg", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("auto", false)) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val svc = Intent(context, RecordingService::class.java)

        try {
            when (state) {
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    svc.action = RecordingService.ACTION_START
                    ContextCompat.startForegroundService(context, svc)
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    svc.action = RecordingService.ACTION_STOP
                    ContextCompat.startForegroundService(context, svc)
                }
            }
        } catch (e: Exception) {
            // Background FGS start blocked (Android 12+) -- ignore, user can record manually.
        }
    }
}
