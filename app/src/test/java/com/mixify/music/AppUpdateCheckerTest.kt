package com.mixify.music

import com.mixify.music.data.AppUpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun `rejects draft releases`() {
        val json = """
            {
              "tag_name": "v1.0.8",
              "html_url": "https://github.com/MixifyTeam/Mixify/releases/tag/v1.0.8",
              "draft": true,
              "prerelease": false,
              "assets": [
                {
                  "name": "Mixify-1.0.8.apk",
                  "state": "uploaded",
                  "browser_download_url": "https://github.com/MixifyTeam/Mixify/releases/download/v1.0.8/Mixify-1.0.8.apk"
                }
              ]
            }
        """.trimIndent()

        val info = AppUpdateChecker.parseReleasePayload(json, currentVersion = "1.0.7", apkPrefix = "Mixify-", isDevOrDebug = false)
        assertNull(info)
    }

    @Test
    fun `rejects prereleases for stable users`() {
        val json = """
            {
              "tag_name": "v1.0.8-beta1",
              "html_url": "https://github.com/MixifyTeam/Mixify/releases/tag/v1.0.8-beta1",
              "draft": false,
              "prerelease": true,
              "assets": [
                {
                  "name": "Mixify-1.0.8-beta1.apk",
                  "state": "uploaded",
                  "browser_download_url": "https://github.com/MixifyTeam/Mixify/releases/download/v1.0.8-beta1/Mixify-1.0.8-beta1.apk"
                }
              ]
            }
        """.trimIndent()

        // Stable current version
        val infoStable = AppUpdateChecker.parseReleasePayload(json, currentVersion = "1.0.7", apkPrefix = "Mixify-", isDevOrDebug = false)
        assertNull(infoStable)

        // Pre-release current version
        val infoPre = AppUpdateChecker.parseReleasePayload(json, currentVersion = "1.0.7-beta1", apkPrefix = "Mixify-", isDevOrDebug = false)
        assertNotNull(infoPre)
        assertEquals("1.0.8-beta1", infoPre?.version)
    }

    @Test
    fun `excludes TV Wear and dev debug APKs from phone updates`() {
        val json = """
            {
              "tag_name": "v1.0.8",
              "html_url": "https://github.com/MixifyTeam/Mixify/releases/tag/v1.0.8",
              "draft": false,
              "prerelease": false,
              "assets": [
                {
                  "name": "MixifyTV-1.0.8.apk",
                  "state": "uploaded",
                  "browser_download_url": "https://github.com/MixifyTeam/Mixify/releases/download/v1.0.8/MixifyTV-1.0.8.apk"
                },
                {
                  "name": "MixifyWear-1.0.8.apk",
                  "state": "uploaded",
                  "browser_download_url": "https://github.com/MixifyTeam/Mixify/releases/download/v1.0.8/MixifyWear-1.0.8.apk"
                },
                {
                  "name": "Mixify-1.0.8-dev.apk",
                  "state": "uploaded",
                  "browser_download_url": "https://github.com/MixifyTeam/Mixify/releases/download/v1.0.8/Mixify-1.0.8-dev.apk"
                },
                {
                  "name": "Mixify-1.0.8.apk",
                  "state": "uploaded",
                  "browser_download_url": "https://github.com/MixifyTeam/Mixify/releases/download/v1.0.8/Mixify-1.0.8.apk"
                }
              ]
            }
        """.trimIndent()

        val info = AppUpdateChecker.parseReleasePayload(json, currentVersion = "1.0.7", apkPrefix = "Mixify-", isDevOrDebug = false)
        assertNotNull(info)
        assertEquals("https://github.com/MixifyTeam/Mixify/releases/download/v1.0.8/Mixify-1.0.8.apk", info?.apkUrl)
    }

    @Test
    fun `selects split ABI when universal is missing`() {
        val json = """
            {
              "tag_name": "v1.0.8",
              "html_url": "https://github.com/MixifyTeam/Mixify/releases/tag/v1.0.8",
              "draft": false,
              "prerelease": false,
              "assets": [
                {
                  "name": "Mixify-1.0.8_arm64-v8a.apk",
                  "state": "uploaded",
                  "browser_download_url": "https://github.com/MixifyTeam/Mixify/releases/download/v1.0.8/Mixify-1.0.8_arm64-v8a.apk"
                }
              ]
            }
        """.trimIndent()

        val info = AppUpdateChecker.parseReleasePayload(
            json,
            currentVersion = "1.0.7",
            apkPrefix = "Mixify-",
            isDevOrDebug = false,
            supportedAbis = arrayOf("arm64-v8a")
        )
        assertNotNull(info)
        assertEquals("https://github.com/MixifyTeam/Mixify/releases/download/v1.0.8/Mixify-1.0.8_arm64-v8a.apk", info?.apkUrl)
    }

    @Test
    fun `version comparison logic`() {
        assertTrue(AppUpdateChecker.isNewer("1.0.8", "1.0.7"))
        assertTrue(AppUpdateChecker.isNewer("1.1.0", "1.0.9"))
        assertTrue(AppUpdateChecker.isNewer("1.0.7", "1.0.7-beta1"))
        assertTrue(!AppUpdateChecker.isNewer("1.0.7", "1.0.7"))
        assertTrue(!AppUpdateChecker.isNewer("1.0.6", "1.0.7"))
    }
}
