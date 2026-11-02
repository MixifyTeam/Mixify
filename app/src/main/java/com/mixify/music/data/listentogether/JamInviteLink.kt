package com.mixify.music.data.listentogether

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import com.mixify.music.BuildConfig

data class ParsedJamInvite(
    val code: String,
    val serverUrl: String? = null,
)

/** Relays a Mixify web or scheme invite from [com.mixify.music.MainActivity] to Compose. */
object JamInviteLink {

    const val ORIGIN = "https://mixify.co.in"

    private const val EXTRA_CONSUMED = "bitchord.jamInviteConsumed"
    private const val HOST = "mixify.co.in"
    private const val CUSTOM_SCHEME = "mixify"
    private const val CUSTOM_HOST = "party"

    private val _pending = MutableStateFlow<ParsedJamInvite?>(null)
    val pending: StateFlow<ParsedJamInvite?> = _pending.asStateFlow()

    /** Reads a web invite from a cold launch or a new intent on the existing task. */
    fun consume(intent: Intent?): Boolean {
        if (
            intent == null ||
            intent.action != Intent.ACTION_VIEW ||
            intent.getBooleanExtra(EXTRA_CONSUMED, false)
        ) return false

        val invite = parseInvite(intent.dataString) ?: return false
        intent.putExtra(EXTRA_CONSUMED, true)
        _pending.value = invite
        return true
    }

    fun handled() {
        _pending.value = null
    }

    /** Returns the normalized party code only for the public invite URL shape. */
    fun parse(value: String?): String? = parseInvite(value)?.code

    /**
     * Parses an incoming invite:
     * 1. bitchord://party/<CODE>?server=<SERVER>
     * 2. https://bitchord.kushagrasingh.in/invite/<CODE>?server=<SERVER>
     */
    fun parseInvite(value: String?): ParsedJamInvite? {
        val uri = runCatching { URI(value ?: return null) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host?.lowercase() ?: return null
        val query = uri.rawQuery
        val serverValues = queryValues(query, "server") ?: return null
        if (serverValues.size > 1) return null
        val server = serverValues.singleOrNull()?.let { sanitizeServerUrl(it) ?: return null }
        if (uri.rawUserInfo != null || uri.port != -1 || uri.rawFragment != null) return null

        // Canonical Mixify website links retain the room's backend in the query.
        if (scheme == "https" && host == HOST && uri.path == "/listen") {
            val codes = (queryValues(query, "code") ?: return null) +
                (queryValues(query, "room") ?: return null)
            if (codes.size != 1) return null
            return ParsedJamInvite(cleanCode(codes.single()) ?: return null, server)
        }

        // 1. Custom scheme: bitchord://party/<CODE> or bitchord://party?code=<CODE>
        if (scheme in setOf(CUSTOM_SCHEME, "bitchord") && host == CUSTOM_HOST) {
            val pathPart = uri.path.orEmpty().trim('/').takeIf { it.isNotBlank() }
            val candidate = pathPart ?: extractQueryParam(query, "code") ?: return null
            val code = cleanCode(candidate) ?: return null
            return ParsedJamInvite(code = code, serverUrl = server)
        }

        // 2. Official web domain: https://bitchord.kushagrasingh.in/invite/<CODE>
        if (scheme == "https" && host in setOf(HOST, "bitchord.kushagrasingh.in")) {
            val match = INVITE_PATH.matchEntire(uri.path.orEmpty()) ?: return null
            val code = match.groupValues[1].uppercase()
            return ParsedJamInvite(code = code, serverUrl = server)
        }

        return null
    }

    fun url(code: String, customServer: String? = null): String {
        val normalizedCode = requireNotNull(cleanCode(code)) { "Invalid room code" }
        val server = customServer?.takeIf { it.isNotBlank() }?.let {
            requireNotNull(sanitizeServerUrl(it)) { "Invalid room server" }
        }
        return buildString {
            append("$ORIGIN/listen?code=$normalizedCode&v=107")
            if (server != null) append("&server=${URLEncoder.encode(server, "UTF-8")}")
            if (BuildConfig.APPLICATION_ID == "com.mixify.app.dev") append("&app=dev")
        }
    }

    fun schemeUrl(code: String, customServer: String? = null): String {
        val normalizedCode = requireNotNull(cleanCode(code)) { "Invalid room code" }
        val base = customServer?.trim()?.trimEnd('/')
        return if (!base.isNullOrBlank()) {
            val encoded = runCatching { URLEncoder.encode(base, "UTF-8") }.getOrDefault(base)
            "mixify://party/$normalizedCode?server=$encoded"
        } else {
            "mixify://party/$normalizedCode"
        }
    }

    private fun cleanCode(raw: String): String? {
        val cleaned = raw.trim().uppercase(Locale.ROOT)
        return cleaned.takeIf { it.matches(Regex("[A-Z0-9]{${ListenTogether.CODE_LENGTH}}")) }
    }

    private fun queryValues(query: String?, name: String): List<String>? = runCatching {
        query.orEmpty().split('&').filter { it.isNotEmpty() }.map { it.split('=', limit = 2) }
            .filter { URLDecoder.decode(it[0], "UTF-8") == name }
            .map { URLDecoder.decode(it.getOrElse(1) { "" }, "UTF-8") }
    }.getOrNull()

    private fun extractQueryParam(query: String?, paramName: String): String? =
        queryValues(query, paramName)?.singleOrNull()

    private fun sanitizeServerUrl(raw: String?): String? {
        val value = raw?.trim()?.trimEnd('/') ?: return null
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank() ||
            uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null) return null
        return value
    }

    private val INVITE_PATH = Regex("""/invite/([A-Za-z0-9]{${ListenTogether.CODE_LENGTH}})""")
}

