package com.markfold.callrecorder

import android.content.ContentValues
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Records the device microphone to a temp .m4a file, then publishes it into
 * shared storage under Music/CallRecorder so it shows up in file managers
 * and music players.
 *
 * NOTE: This captures the microphone only. To capture BOTH sides of a call,
 * put the call on speakerphone -- the far party's audio then reaches the mic.
 */
class Recorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var tempFile: File? = null
    var isRecording = false
        private set

    fun start(): Boolean {
        if (isRecording) return false
        val dir = File(context.cacheDir, "rec").apply { mkdirs() }
        val f = File(dir, "tmp_${System.currentTimeMillis()}.m4a")

        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(128_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            tempFile = f
            isRecording = true
            true
        } catch (e: Exception) {
            try { r.release() } catch (_: Exception) {}
            f.delete()
            false
        }
    }

    /** Stops recording and returns the shared-storage Uri of the saved file, or null. */
    fun stop(): Uri? {
        val r = recorder ?: return null
        val src = tempFile
        return try {
            r.stop()
            r.release()
            if (src != null) saveToMediaStore(src) else null
        } catch (e: Exception) {
            null
        } finally {
            recorder = null
            src?.delete()
            tempFile = null
            isRecording = false
        }
    }

    private fun saveToMediaStore(src: File): Uri? {
        val name = "Call_" +
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".m4a"

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, name)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/CallRecorder")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values
        ) ?: return null

        resolver.openOutputStream(uri)?.use { out ->
            src.inputStream().use { it.copyTo(out) }
        }

        values.clear()
        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }
}
