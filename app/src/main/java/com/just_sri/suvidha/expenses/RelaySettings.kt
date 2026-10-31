package com.just_sri.suvidha.expenses

import android.content.Context
import java.net.URI

/** The user's Nostr relay list. An empty custom list means "use the app defaults". */
internal class RelaySettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("expense_settings", Context.MODE_PRIVATE)

    fun urls(): List<String> = custom() ?: DEFAULT_URLS

    fun isCustom(): Boolean = custom() != null

    /** Returns an error message, or null when the relay was added. */
    fun add(input: String): String? {
        val url: String = normalize(input) ?: return "Enter a relay address like wss://relay.example.com."
        val current: List<String> = urls()
        if (url in current) return "This relay is already in your list."
        if (current.size >= MAX_RELAYS) return "You can use up to $MAX_RELAYS relays."
        save(current + url)
        return null
    }

    fun remove(url: String): String? {
        val remaining: List<String> = urls() - url
        if (remaining.isEmpty()) return "Keep at least one relay."
        save(remaining)
        return null
    }

    fun reset() {
        prefs.edit().remove(KEY).apply()
    }

    private fun custom(): List<String>? =
        prefs.getString(KEY, null)?.split('\n')?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }

    private fun save(urls: List<String>) {
        prefs.edit().putString(KEY, urls.joinToString("\n")).apply()
    }

    companion object {
        const val MAX_RELAYS = 5
        private const val KEY = "relay_urls"
        val DEFAULT_URLS: List<String> = listOf("wss://nos.lol", "wss://relay.primal.net")

        /** Accepts only encrypted websocket addresses; returns the canonical form or null. */
        fun normalize(input: String): String? {
            val trimmed: String = input.trim().trimEnd('/')
            if (!trimmed.startsWith("wss://", ignoreCase = true) || trimmed.length > 200) return null
            val uri: URI = runCatching { URI(trimmed) }.getOrNull() ?: return null
            val host: String = uri.host?.lowercase() ?: return null
            if (!host.contains('.') || uri.userInfo != null || uri.fragment != null || uri.query != null) return null
            val port: String = if (uri.port == -1) "" else ":${uri.port}"
            val path: String = uri.rawPath.orEmpty().trimEnd('/')
            return "wss://$host$port$path"
        }
    }
}
