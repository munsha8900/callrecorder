package com.markfold.callrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow

class RecordingService : Service() {

    companion object {
        const val ACTION_START = "com.markfold.callrecorder.START"
        const val ACTION_STOP = "com.markfold.callrecorder.STOP"
        private const val CHANNEL_ID = "recording_channel"
        private const val NOTIF_ID = 1

        /** UI observes this to reflect current recording state. */
        val recordingState = MutableStateFlow(false)
    }

    private lateinit var recorder: Recorder

    override fun onCreate() {
        super.onCreate()
        recorder = Recorder(applicationContext)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        if (recordingState.value) return
        startAsForeground()
        if (recorder.start()) {
            recordingState.value = true
        } else {
            recordingState.value = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopRecording() {
        recorder.stop()
        recordingState.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startAsForeground() {
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle("Recording in progress")
            .setContentText("Put the call on speaker to capture both sides.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Stop", stopPending)
            .build()
    }

    private fun createChannel() {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Call recording",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shown while a recording is active"
        }
        mgr.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
