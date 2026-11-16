package com.mdevz.sp.core.config

import android.net.Uri
import android.util.Base64
import com.mdevz.sp.core.model.SshProfile
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** Importer for common V2Ray/Xray share links and native Xray JSON profiles. */
object XrayProfileUriCodec {
    fun decode(value: String): SshProfile {
        val text = value.trim()
        require(text.isNotEmpty()) { "Profile is empty" }
        if (text.startsWith("{")) return fromJson(text)
        val scheme = text.substringBefore(":").lowercase()
        return when (scheme) {
            "vmess" -> fromVmess(text)
            "vless", "trojan", "ss", "socks", "socks5", "http", "https" -> fromUri(text, scheme)
            else -> throw IllegalArgumentException("Unsupported Xray profile format")
        }
    }

    private fun fromVmess(link: String): SshProfile {
        val encoded = link.substringAfter("://", "")
        require(encoded.isNotBlank()) { "Invalid VMess link" }
        val json = JSONObject(String(Base64.decode(pad(encoded), Base64.DEFAULT), StandardCharsets.UTF_8))
        val sni = json.optString("sni", "")
        val onering = parseOnering(sni)
        return SshProfile(
            name = json.optString("ps", json.optString("add", "VMess")),
            connectionType = "XRAY", sshHost = "", username = "", password = "", xrayProtocol = "vmess",
            xrayAddress = json.optString("add"), xrayPort = json.optString("port", "443").toIntOrNull() ?: 443,
            xrayUuid = json.optString("id"), xrayAlterId = json.optInt("aid", 0),
            xraySecurity = json.optString("scy", "auto"), xrayNetwork = json.optString("net", "tcp"),
            xrayPath = json.optString("path", "/"), xrayHost = json.optString("host", ""),
            xrayTls = json.optString("tls", "").equals("tls", true), xraySni = sni,
            oneringEnabled = onering != null, oneringOriginDomain = onering?.first.orEmpty(), oneringBugDomain = onering?.second.orEmpty()
        )
    }

    private fun fromUri(link: String, scheme: String): SshProfile {
        val uri = Uri.parse(link)
        val userInfo = uri.encodedUserInfo.orEmpty()
        val user = decodePart(userInfo.substringBefore(':'))
        val pass = decodePart(userInfo.substringAfter(':', ""))
        val query = uri.queryParameterNames.associateWith { uri.getQueryParameter(it).orEmpty() }
        val rawSni = query["sni"] ?: query["peer"] ?: ""
        val onering = parseOnering(rawSni)
        val protocol = when (scheme) {
            "socks5" -> "socks"
            "https" -> "http"
            "ss" -> "shadowsocks"
            else -> scheme
        }
        val uuid = if (protocol == "vless") user else ""
        val secret = when (protocol) { "trojan" -> user; "ss" -> decodeShadowsocksUserInfo(userInfo).second; else -> pass }
        val method = if (protocol == "ss") decodeShadowsocksUserInfo(userInfo).first else "aes-128-gcm"
        return SshProfile(
            name = uri.fragment?.let { decodePart(it) }?.takeIf { it.isNotBlank() } ?: uri.host.orEmpty(),
            connectionType = "XRAY", sshHost = "", username = "", password = "", xrayProtocol = protocol,
            xrayAddress = uri.host.orEmpty(), xrayPort = uri.port.takeIf { it > 0 } ?: if (query["security"] == "tls") 443 else 80,
            xrayUuid = uuid, xrayPassword = secret,
            xrayMethod = method, xraySecurity = query["security"] ?: "auto",
            xrayNetwork = query["type"] ?: "tcp", xrayPath = query["path"] ?: "/",
            xrayHost = query["host"] ?: "", xrayTls = query["security"] == "tls" || scheme == "https",
            xraySni = rawSni, xrayFlow = query["flow"] ?: "",
            oneringEnabled = onering != null, oneringOriginDomain = onering?.first.orEmpty(), oneringBugDomain = onering?.second.orEmpty()
        )
    }

    private fun fromJson(text: String): SshProfile {
        val root = JSONObject(text)
        val outbound = root.optJSONArray("outbounds")?.let { arr -> (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.firstOrNull { it.optString("tag") != "direct" && it.optString("protocol") != "freedom" } }
            ?: throw IllegalArgumentException("JSON has no Xray outbound")
        val protocol = outbound.optString("protocol", "json")
        val settings = outbound.optJSONObject("settings") ?: JSONObject()
        val vnext = settings.optJSONArray("vnext")?.optJSONObject(0)
        val server = settings.optJSONArray("servers")?.optJSONObject(0)
        val stream = outbound.optJSONObject("streamSettings") ?: JSONObject()
        val ws = stream.optJSONObject("wsSettings") ?: JSONObject()
        val headers = ws.optJSONObject("headers") ?: JSONObject()
        val tls = stream.optJSONObject("tlsSettings") ?: JSONObject()
        val user = vnext?.optJSONArray("users")?.optJSONObject(0) ?: server
        val serverName = tls.optString("serverName", "")
        val onering = parseOnering(serverName)
        return SshProfile(
            name = root.optString("remarks", root.optString("name", "Imported Xray JSON")),
            connectionType = "XRAY", sshHost = "", username = "", password = "", xrayProtocol = protocol,
            xrayAddress = vnext?.optString("address") ?: server?.optString("address").orEmpty(),
            xrayPort = vnext?.optInt("port", 443) ?: server?.optInt("port", 443) ?: 443,
            xrayUuid = user?.optString("id").orEmpty(), xrayPassword = user?.optString("password").orEmpty(),
            xrayAlterId = user?.optInt("alterId", 0) ?: 0,
            xraySecurity = user?.optString("security", "auto") ?: "auto",
            xrayNetwork = stream.optString("network", "tcp"), xrayPath = ws.optString("path", "/"),
            xrayHost = headers.optString("Host", ""), xrayTls = tls.length() > 0,
            xraySni = serverName, xrayFlow = user?.optString("flow", "").orEmpty(),
            xrayMethod = server?.optString("method", "aes-128-gcm") ?: "aes-128-gcm",
            oneringEnabled = onering != null,
            oneringOriginDomain = onering?.first.orEmpty(),
            oneringBugDomain = onering?.second.orEmpty(),
            xrayRawJson = text
        )
    }

    private fun parseOnering(value: String): Pair<String, String>? {
        if (!value.startsWith("onering:", true)) return null
        val parts = value.split(':', limit = 3)
        if (parts.size != 3 || parts[1].isBlank() || parts[2].isBlank()) return null
        return parts[1] to parts[2]
    }

    private fun decodeShadowsocksUserInfo(info: String): Pair<String, String> {
        val decoded = runCatching { String(Base64.decode(pad(info.substringBefore('@')), Base64.DEFAULT), StandardCharsets.UTF_8) }.getOrDefault("")
        return decoded.substringBefore(':') to decoded.substringAfter(':', "")
    }

    private fun pad(value: String) = value + "=".repeat((4 - value.length % 4) % 4)
    private fun decodePart(value: String) = URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}
