package com.mdevz.sp.xray

import org.json.JSONArray
import org.json.JSONObject

/**
 * Applies the Onering serverName convention documented by dharak36/xray-onering:
 * onering:yourdomain:bugdomain
 *
 * For VMess over WebSocket, bugDomain is the outbound address, while originDomain
 * is used as the WebSocket Host header. TLS serverName is kept verbatim.
 */
object OneringConfig {
    fun apply(configJson: String, originDomain: String, bugDomain: String): String {
        require(validHost(originDomain)) { "Origin/Host domain is invalid" }
        require(validHost(bugDomain)) { "Bug domain is invalid" }
        val root = JSONObject(configJson)
        val outbounds = root.optJSONArray("outbounds")
            ?: throw IllegalArgumentException("Xray config has no outbounds array")
        var changed = false
        for (i in 0 until outbounds.length()) {
            val outbound = outbounds.optJSONObject(i) ?: continue
            val protocol = outbound.optString("protocol")
            val settings = outbound.optJSONObject("settings") ?: continue
            val servers = settings.optJSONArray("vnext")
            if (protocol == "vmess" && servers != null) {
                for (j in 0 until servers.length()) {
                    servers.optJSONObject(j)?.put("address", bugDomain)
                    changed = true
                }
            }
            val stream = outbound.optJSONObject("streamSettings") ?: continue
            if (stream.optString("network") == "ws") {
                val ws = stream.optJSONObject("wsSettings") ?: JSONObject().also {
                    stream.put("wsSettings", it)
                }
                val headers = ws.optJSONObject("headers") ?: JSONObject().also {
                    ws.put("headers", it)
                }
                headers.put("Host", originDomain)
                changed = true
            }
            val tls = stream.optJSONObject("tlsSettings")
            if (tls != null) {
                tls.put("serverName", "onering:$originDomain:$bugDomain")
                changed = true
            }
        }
        require(changed) { "No supported VMess/WS/TLS outbound found to apply Onering settings" }
        return root.toString()
    }

    private fun validHost(value: String): Boolean =
        value.length in 1..253 && value.matches(Regex("(?i)^[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?$")) &&
            !value.contains("..")
}
