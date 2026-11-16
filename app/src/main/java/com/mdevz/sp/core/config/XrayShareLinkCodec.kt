package com.mdevz.sp.core.config

import android.net.Uri
import android.util.Base64
import com.mdevz.sp.core.model.SshProfile
import org.json.JSONObject

/**
 * Imports standard VLESS and VMess share links.
 *
 * Supported examples:
 * vless://uuid@host:port?type=ws&security=tls&host=...&path=...&sni=...
 * vmess://base64(JSON)
 */
object XrayShareLinkCodec {
    fun isXrayUri(value: String?): Boolean {
        val scheme = runCatching { Uri.parse(value?.trim().orEmpty()).scheme }.getOrNull()
        return scheme.equals("vless", true) || scheme.equals("vmess", true)
    }

    fun decode(value: String): SshProfile {
        val raw = value.trim()
        require(raw.length <= 128 * 1024) { "Xray share link is too large" }
        val uri = Uri.parse(raw)
        return when (uri.scheme?.lowercase()) {
            "vless" -> decodeVless(uri)
            "vmess" -> decodeVmess(uri)
            else -> throw IllegalArgumentException("Only VLESS and VMess links are supported")
        }.also { profile ->
            val errors = ProfileValidation.validate(profile)
            require(errors.isEmpty()) {
                errors.joinToString("\n") { "${it.field}: ${it.message}" }
            }
        }
    }

    private fun decodeVless(uri: Uri): SshProfile {
        val uuid = uri.userInfo?.substringBefore(':').orEmpty()
        val host = uri.host.orEmpty()
        val port = uri.port
        require(uuid.isNotBlank()) { "VLESS UUID is missing" }
        require(host.isNotBlank()) { "VLESS server host is missing" }
        require(port in 1..65535) { "VLESS server port is invalid" }

        val security = uri.getQueryParameter("security").orEmpty()
            .ifBlank { "none" }.lowercase()
        val network = uri.getQueryParameter("type").orEmpty()
            .ifBlank { "tcp" }.lowercase()
        val name = uri.fragment?.let { Uri.decode(it) }
            ?.ifBlank { null } ?: "VLESS-$host-$port"

        return SshProfile(
            name = name,
            sshHost = "",
            username = "",
            password = "",
            connectionType = "XRAY",
            xrayProtocol = "VLESS",
            xrayServer = host,
            xrayPort = port,
            xrayUuid = uuid,
            xraySecurity = security,
            xrayNetwork = network,
            xrayHost = uri.getQueryParameter("host").orEmpty(),
            xrayPath = uri.getQueryParameter("path").orEmpty().ifBlank { "/" },
            xrayServerName = uri.getQueryParameter("sni").orEmpty(),
            xrayFingerprint = uri.getQueryParameter("fp").orEmpty().ifBlank { "chrome" },
            xrayAllowInsecure = uri.getQueryParameter("allowInsecure") in setOf("1", "true"),
            localSocksPort = 10808
        )
    }

    private fun decodeVmess(uri: Uri): SshProfile {
        val encoded = uri.toString().substringAfter("://", "").substringBefore('#').trim()
        require(encoded.isNotBlank()) { "VMess payload is empty" }
        val normalized = encoded.replace("\\s".toRegex(), "")
        val bytes = runCatching {
            Base64.decode(normalized, Base64.DEFAULT or Base64.NO_WRAP)
        }.recoverCatching {
            Base64.decode(normalized, Base64.URL_SAFE or Base64.NO_WRAP)
        }.getOrElse { throw IllegalArgumentException("VMess link has invalid Base64 data", it) }
        val json = try {
            JSONObject(bytes.toString(Charsets.UTF_8))
        } finally {
            bytes.fill(0)
        }

        val host = json.optString("add").trim()
        val port = json.optString("port").toIntOrNull() ?: json.optInt("port", 0)
        val uuid = json.optString("id").trim()
        require(host.isNotBlank()) { "VMess server host is missing" }
        require(port in 1..65535) { "VMess server port is invalid" }
        require(uuid.isNotBlank()) { "VMess UUID is missing" }

        val network = json.optString("net", "tcp").lowercase()
        val security = if (json.optString("tls").equals("tls", true)) "tls" else "none"
        val name = uri.fragment?.let { Uri.decode(it) }
            ?.ifBlank { null } ?: json.optString("ps").ifBlank { "VMess-$host-$port" }

        return SshProfile(
            name = name,
            sshHost = "",
            username = "",
            password = "",
            connectionType = "XRAY",
            xrayProtocol = "VMess",
            xrayServer = host,
            xrayPort = port,
            xrayUuid = uuid,
            xraySecurity = security,
            xrayNetwork = network,
            xrayHost = json.optString("host", ""),
            xrayPath = json.optString("path", "/").ifBlank { "/" },
            xrayServerName = json.optString("sni", ""),
            xrayFingerprint = json.optString("fp", ""),
            localSocksPort = 10808
        )
    }
}
