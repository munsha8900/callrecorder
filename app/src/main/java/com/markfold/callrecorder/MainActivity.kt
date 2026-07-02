package com.markfold.callrecorder

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState

data class Rec(val uri: Uri, val name: String)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = brandColors()) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppScreen()
                }
            }
        }
    }
}

private fun brandColors() = lightColorScheme(
    primary = Color(0xFF0F4F4A),
    onPrimary = Color(0xFFF4EEE2),
    secondary = Color(0xFFF4C84A),
    background = Color(0xFFF4EEE2),
    surface = Color(0xFFFFFFFF)
)

@Composable
private fun AppScreen() {
    val context = LocalContext.current
    val isRecording by RecordingService.recordingState.collectAsState()

    val prefs = remember { context.getSharedPreferences("cfg", Context.MODE_PRIVATE) }
    var auto by remember { mutableStateOf(prefs.getBoolean("auto", false)) }
    var recordings by remember { mutableStateOf(loadRecordings(context)) }

    // Refresh the list whenever a recording finishes.
    LaunchedEffect(isRecording) {
        if (!isRecording) recordings = loadRecordings(context)
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        permLauncher.launch(requiredPermissions())
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Call Recorder",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "For both sides of a call, use speakerphone.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(28.dp))

        // Record / stop button
        val bg = if (isRecording) Color(0xFFB3261E) else MaterialTheme.colorScheme.primary
        Surface(
            shape = CircleShape,
            color = bg,
            modifier = Modifier.size(140.dp),
            onClick = {
                val intent = Intent(context, RecordingService::class.java).apply {
                    action = if (isRecording) RecordingService.ACTION_STOP
                    else RecordingService.ACTION_START
                }
                ContextCompat.startForegroundService(context, intent)
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (isRecording) "Stop" else "Record",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (isRecording) "Recording..." else "Tap to record",
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        // Auto-record toggle
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Auto-record phone calls", fontWeight = FontWeight.Medium)
                Text(
                    "Regular calls only, not WhatsApp",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Switch(
                checked = auto,
                onCheckedChange = {
                    auto = it
                    prefs.edit().putBoolean("auto", it).apply()
                }
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                context.startActivity(
                    Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable WhatsApp call detection")
        }
        Text(
            "Grant notification access so WhatsApp calls auto-record. Keep calls on speaker to capture both sides.",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(Modifier.height(20.dp))
        Divider()
        Spacer(Modifier.height(12.dp))

        Text(
            "Recordings",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(8.dp))

        if (recordings.isEmpty()) {
            Text("No recordings yet.", color = Color.Gray)
        } else {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(recordings, key = { it.uri.toString() }) { rec ->
                    RecRow(
                        rec = rec,
                        onPlay = { play(context, rec.uri) },
                        onShare = { share(context, rec.uri) },
                        onDelete = {
                            context.contentResolver.delete(rec.uri, null, null)
                            recordings = loadRecordings(context)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecRow(rec: Rec, onPlay: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                rec.name,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                maxLines = 1
            )
            IconButton(onClick = onPlay) {
                Icon(Icons.Filled.PlayArrow, "Play")
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Filled.Share, "Share")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete", tint = Color(0xFFB3261E))
            }
        }
    }
}

private fun requiredPermissions(): Array<String> {
    val perms = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        perms.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    return perms.toTypedArray()
}

private fun loadRecordings(context: Context): List<Rec> {
    val out = mutableListOf<Rec>()
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME
    )
    val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
    val args = arrayOf("Music/CallRecorder%")
    val sort = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

    context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection, selection, args, sort
    )?.use { c ->
        val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        while (c.moveToNext()) {
            val id = c.getLong(idCol)
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
            )
            out.add(Rec(uri, c.getString(nameCol)))
        }
    }
    return out
}

private fun play(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "audio/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Play with"))
}

private fun share(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share recording"))
}
