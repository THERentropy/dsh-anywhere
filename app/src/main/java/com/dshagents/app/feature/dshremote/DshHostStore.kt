package com.dshagents.app.feature.dshremote

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/** A DSH Web host this device paired with through the dsh-remote-web-ui plugin. */
data class DshHost(
    val id: String,
    val name: String,
    val baseUrl: String,
    val lastUsedAt: Long,
)

/** What the DSH WebView container should load when it opens. */
sealed interface DshLaunchRequest {
    /** Fresh pairing link (`<base>/pair-accept?pair=<token>`) from a QR scan or paste. */
    data class PairUrl(val url: String) : DshLaunchRequest

    /** Re-open an already paired host; the container loads `<baseUrl>/pair-app`. */
    data class ResumeHost(val hostId: String) : DshLaunchRequest
}

/** Parsed result of a candidate dsh-remote-web-ui pairing link. */
data class DshPairingLink(
    val url: String,
    val baseUrl: String,
    val hostName: String,
)

/**
 * Validates and parses a pairing link minted by the dsh-remote-web-ui plugin.
 * The QR code content is `<base>/pair-accept?pair=<token>`; the plugin also
 * accepts the same link pasted by hand on the blocking page.
 */
fun parseDshPairingLink(raw: String): DshPairingLink? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") return null
    if (uri.host.isNullOrBlank()) return null
    val path = uri.path.orEmpty()
    if (!path.equals("/pair-accept", ignoreCase = true)) return null
    if (uri.getQueryParameter("pair").isNullOrBlank()) return null
    val port = uri.port
    val baseUrl = buildString {
        append(scheme).append("://").append(uri.host)
        if (port > 0 && port != defaultPort(scheme)) append(':').append(port)
    }
    val hostName = if (port > 0 && port != defaultPort(scheme)) {
        "${uri.host}:$port"
    } else {
        uri.host.orEmpty()
    }
    return DshPairingLink(url = trimmed, baseUrl = baseUrl, hostName = hostName)
}

/** Extracts the origin (`scheme://host[:port]`) of an already paired page URL. */
fun dshOriginOf(url: String): String? {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") return null
    val host = uri.host ?: return null
    val port = uri.port
    return buildString {
        append(scheme).append("://").append(host)
        if (port > 0 && port != defaultPort(scheme)) append(':').append(port)
    }
}

private fun defaultPort(scheme: String): Int = if (scheme == "https") 443 else 80

/** Persists the roster of paired DSH hosts in SharedPreferences as JSON. */
class DshHostStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun list(): List<DshHost> {
        val raw = preferences.getString(KEY_HOSTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val entry = array.optJSONObject(index) ?: continue
                    val id = entry.optString(KEY_ID).orEmpty()
                    val baseUrl = entry.optString(KEY_BASE_URL).orEmpty()
                    if (id.isBlank() || baseUrl.isBlank()) continue
                    add(
                        DshHost(
                            id = id,
                            name = entry.optString(KEY_NAME).ifBlank { baseUrl },
                            baseUrl = baseUrl,
                            lastUsedAt = entry.optLong(KEY_LAST_USED_AT, 0L),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList()).sortedByDescending { it.lastUsedAt }
    }

    fun find(id: String): DshHost? = list().firstOrNull { it.id == id }

    /** Inserts or updates a host keyed by its base URL, refreshing lastUsedAt. */
    fun upsert(baseUrl: String, name: String, lastUsedAt: Long = System.currentTimeMillis()): DshHost {
        val hosts = list().toMutableList()
        val existingIndex = hosts.indexOfFirst { it.baseUrl.equals(baseUrl, ignoreCase = true) }
        val host = if (existingIndex >= 0) {
            hosts[existingIndex].copy(
                name = name.ifBlank { hosts[existingIndex].name },
                lastUsedAt = lastUsedAt,
            )
        } else {
            DshHost(
                id = baseUrl.lowercase(),
                name = name.ifBlank { baseUrl },
                baseUrl = baseUrl,
                lastUsedAt = lastUsedAt,
            )
        }
        if (existingIndex >= 0) hosts[existingIndex] = host else hosts.add(host)
        save(hosts)
        return host
    }

    fun touch(id: String) {
        val hosts = list().map { host ->
            if (host.id == id) host.copy(lastUsedAt = System.currentTimeMillis()) else host
        }
        save(hosts)
    }

    fun remove(id: String) {
        save(list().filterNot { it.id == id })
    }

    private fun save(hosts: List<DshHost>) {
        val array = JSONArray()
        hosts.forEach { host ->
            array.put(
                JSONObject()
                    .put(KEY_ID, host.id)
                    .put(KEY_NAME, host.name)
                    .put(KEY_BASE_URL, host.baseUrl)
                    .put(KEY_LAST_USED_AT, host.lastUsedAt),
            )
        }
        preferences.edit().putString(KEY_HOSTS, array.toString()).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "dsh_remote_hosts"
        const val KEY_HOSTS = "hosts"
        const val KEY_ID = "id"
        const val KEY_NAME = "name"
        const val KEY_BASE_URL = "baseUrl"
        const val KEY_LAST_USED_AT = "lastUsedAt"
    }
}
