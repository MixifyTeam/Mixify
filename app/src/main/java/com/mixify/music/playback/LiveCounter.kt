package com.mixify.music.playback

import android.util.Log
import com.mixify.music.BuildConfig
import com.mixify.music.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

/**
 * Manages the "Listening Now" heartbeat.
 */
class LiveCounter(
    private val scope: CoroutineScope,
    private val client: OkHttpClient,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
    private val url: String = BuildConfig.LIVE_COUNTER_URL,
    private val isDebug: Boolean = BuildConfig.DEBUG
) {
    private var heartbeatJob: Job? = null
    private var sessionId = UUID.randomUUID().toString()
    private var isPlaying = false
    private var isEnabled = AppSettings.shareLiveListeningStatus.value

    init {
        scope.launch {
            AppSettings.shareLiveListeningStatus.collectLatest { enabled ->
                isEnabled = enabled
                if (!enabled) {
                    stopHeartbeatAndSendInactive()
                    // Rotate session ID so if it's turned back on, it's a new anonymous session.
                    sessionId = UUID.randomUUID().toString()
                } else {
                    if (isPlaying) {
                        startHeartbeat()
                    }
                }
            }
        }
    }

    fun onIsPlayingChanged(playing: Boolean) {
        isPlaying = playing
        if (playing && isEnabled) {
            startHeartbeat()
        } else {
            stopHeartbeatAndSendInactive()
        }
    }

    fun onServiceDestroyed() {
        stopHeartbeatAndSendInactive()
    }

    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = scope.launch(ioDispatcher) {
            while (isActive) {
                sendHeartbeat()
                delay(60_000)
            }
        }
    }

    private fun stopHeartbeatAndSendInactive() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        scope.launch(ioDispatcher) {
            sendStop()
        }
    }

    private fun sendHeartbeat() {
        if (url.isBlank()) return
        if (isDebug) return
        
        try {
            val json = """{"sessionId":"$sessionId"}"""
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$url/api/listening/heartbeat")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d("LiveCounter", "Heartbeat failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.d("LiveCounter", "Heartbeat error: ${e.message}")
        }
    }

    private fun sendStop() {
        if (url.isBlank()) return
        if (isDebug) return
        
        try {
            val json = """{"sessionId":"$sessionId"}"""
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$url/api/listening/stop")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d("LiveCounter", "Stop failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.d("LiveCounter", "Stop error: ${e.message}")
        }
    }
}
