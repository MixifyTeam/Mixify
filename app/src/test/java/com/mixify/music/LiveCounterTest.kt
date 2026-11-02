package com.mixify.music

import com.mixify.music.data.settings.AppSettings
import com.mixify.music.playback.LiveCounter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveCounterTest {

    private lateinit var testScope: TestScope
    private val requests = mutableListOf<Request>()
    
    private val mockClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            requests.add(chain.request())
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("".toResponseBody(null))
                .build()
        }
        .build()

    @Before
    fun setup() {
        testScope = TestScope(UnconfinedTestDispatcher())
        requests.clear()
        AppSettings.shareLiveListeningStatus.value = true
    }

    @Test
    fun `starts heartbeat immediately when playing`() = testScope.runTest {
        val counter = LiveCounter(backgroundScope, mockClient, UnconfinedTestDispatcher(testScheduler), "http://localhost", false)
        counter.onIsPlayingChanged(true)
        
        // Wait for first execution
        advanceTimeBy(100)
        assertTrue("Expected a heartbeat", requests.any { it.url.toString().endsWith("/api/listening/heartbeat") })
        
        counter.onServiceDestroyed()
    }

    @Test
    fun `sends repeated heartbeats every 60 seconds`() = testScope.runTest {
        val counter = LiveCounter(backgroundScope, mockClient, UnconfinedTestDispatcher(testScheduler), "http://localhost", false)
        counter.onIsPlayingChanged(true)
        
        advanceTimeBy(125_000)
        
        val heartbeats = requests.filter { it.url.toString().endsWith("/api/listening/heartbeat") }
        println("Heartbeats size: ${heartbeats.size}")
        assertTrue("Expected at least 3 heartbeats but got ${heartbeats.size}", heartbeats.size >= 3) // t=0, t=60, t=120
        
        counter.onServiceDestroyed()
    }

    @Test
    fun `sends stop event on pause`() = testScope.runTest {
        val counter = LiveCounter(backgroundScope, mockClient, UnconfinedTestDispatcher(testScheduler), "http://localhost", false)
        counter.onIsPlayingChanged(true)
        advanceTimeBy(100)
        requests.clear()
        
        counter.onIsPlayingChanged(false)
        advanceTimeBy(100)
        
        assertTrue("Expected a stop event", requests.any { it.url.toString().endsWith("/api/listening/stop") })
    }

    @Test
    fun `sends stop event on service destroyed`() = testScope.runTest {
        val counter = LiveCounter(backgroundScope, mockClient, UnconfinedTestDispatcher(testScheduler), "http://localhost", false)
        counter.onIsPlayingChanged(true)
        advanceTimeBy(100)
        requests.clear()
        
        counter.onServiceDestroyed()
        advanceTimeBy(100)
        
        assertTrue("Expected a stop event", requests.any { it.url.toString().endsWith("/api/listening/stop") })
    }
    
    @Test
    fun `respects share setting toggle`() = testScope.runTest {
        AppSettings.shareLiveListeningStatus.value = false
        val counter = LiveCounter(backgroundScope, mockClient, UnconfinedTestDispatcher(testScheduler), "http://localhost", false)
        
        counter.onIsPlayingChanged(true)
        advanceTimeBy(100_000)
        
        // No heartbeats should be sent if disabled
        assertTrue(requests.none { it.url.toString().endsWith("/api/listening/heartbeat") })
        
        AppSettings.shareLiveListeningStatus.value = true
        advanceTimeBy(100)
        // Now it should send a heartbeat since it was playing
        assertTrue(requests.any { it.url.toString().endsWith("/api/listening/heartbeat") })
        
        counter.onServiceDestroyed()
    }
}
