package com.mdevz.sp.core.config

import com.mdevz.sp.core.model.SshProfile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds a minimal Xray JSON document for VMess, VLESS and Trojan profiles.
 * Onering is passed verbatim as TLS serverName: onering:yourdomain:bugdomain.
 */
object XrayConfigBuilder {
    fun build(profile: SshProfile, socksPort: Int = 10808): String {
        require(profile.connectionType == "XRAY") { "This is not an Xray profile" }
        require(profile.xrayServer.isNotBlank()) { "Xray server is required" }
        require(profile.xrayPort in 1..65535) { "Xray port must be 1..65535" }

        val protocol = profile.xrayProtocol.lowercase()
        require(protocol in setOf("vmess", "vless", "trojan")) { "Unsupported Xray protocol" }
        if (protocol != "trojan") require(profile.xrayUuid.isNotBlank()) { "UUID is required" }
        if (protocol == "trojan") require(profile.xrayPassword.isNotBlank()) { "Trojan password is required" }

        val user = JSONObject().apply {
            when (protocol) {
                "vmess" -> {
                    put("id", profile.xrayUuid)
                    put("alterId", 0)
                    put("security", "auto")
                }
                "vless" -> {
                    put("id", profile.xrayUuid)
                    put("encryption", "none")
                }
                "trojan" -> put("password", profile.xrayPassword)
            }
        }
        val stream = JSONObject().apply {
            put("network", profile.xrayNetwork.lowercase())
            put("security", profile.xraySecurity.lowercase())
            if (profile.xrayNetwork.equals("ws", true)) {
                put("wsSettings", JSONObject().apply {
                    put("path", profile.xrayPath.ifBlank { "/" })
                    put("headers", JSONObject().put("Host", profile.xrayHost.ifBlank { profile.xrayServer }))
                })
            }
            if (profile.xraySecurity.equals("tls", true)) {
                put("tlsSettings", JSONObject().apply {
                    put("serverName", profile.xrayServerName.ifBlank { profile.xrayHost.ifBlank { profile.xrayServer } })
                    put("allowInsecure", profile.xrayAllowInsecure)
                })
            }
        }
        val outbound = JSONObject().apply {
            put("protocol", protocol)
            put("settings", JSONObject().apply {
                if (protocol == "trojan") {
                    put("servers", JSONArray().put(JSONObject().apply {
                        put("address", profile.xrayServer)
                        put("port", profile.xrayPort)
                        put("password", profile.xrayPassword)
                    }))
                } else {
                    put("vnext", JSONArray().put(JSONObject().apply {
                        put("address", profile.xrayServer)
                        put("port", profile.xrayPort)
                        put("users", JSONArray().put(user))
                    }))
                }
            })
            put("streamSettings", stream)
            put("tag", "proxy")
        }
        return JSONObject().apply {
            put("log", JSONObject().put("loglevel", "warning"))
            put("inbounds", JSONArray().put(JSONObject().apply {
                put("listen", "127.0.0.1")
                put("port", socksPort)
                put("protocol", "socks")
                put("settings", JSONObject().put("udp", true).put("auth", "noauth"))
                put("tag", "socks-in")
            }))
            put("outbounds", JSONArray().put(outbound).put(JSONObject().put("protocol", "freedom").put("tag", "direct")))
        }.toString(2)
    }
}
