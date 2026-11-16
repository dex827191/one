package com.mdevz.sp.core.model

data class SshProfile(
    val name: String,

    // SSH remains the default for backwards compatibility. Xray profiles are
    // stored in the same profile library but use their own outbound settings.
    val connectionType: String = "SSH",
    val xrayProtocol: String = "vmess",
    val xrayAddress: String = "",
    val xrayPort: Int = 443,
    val xrayUuid: String = "",
    val xrayAlterId: Int = 0,
    val xraySecurity: String = "auto",
    val xrayNetwork: String = "ws",
    val xrayPath: String = "/",
    val xrayHost: String = "",
    val xrayTls: Boolean = true,
    val xraySni: String = "",
    val xrayFlow: String = "",
    val xrayPassword: String = "",
    val xrayMethod: String = "aes-128-gcm",
    val xrayRawJson: String = "",
    val oneringEnabled: Boolean = false,
    val oneringOriginDomain: String = "",
    val oneringBugDomain: String = "",

    val sshHost: String,
    val sshPort: Int = 22,
    val username: String,
    val password: String,

    val payloadEnabled: Boolean = false,
    val payloadMode: PayloadMode = PayloadMode.NORMAL,
    val payload: String = "",

    val proxyEnabled: Boolean = false,
    val proxyHost: String = "",
    val proxyPort: Int = 0,

    val tlsEnabled: Boolean = false,
    val sni: String = "",
    val tlsVersion: String = "default",

    val localSocksPort: Int = 1080,
    val keepAliveSeconds: Int = 30,
    val socketSendBufferSize: Int = 16_384,
    val socketReceiveBufferSize: Int = 32_768,
    val tcpNoDelay: Boolean = true,
    val cpuWakeLockEnabled: Boolean = false,
    val connectTimeoutMillis: Int = 15_000,
    val readTimeoutMillis: Int = 0,

    val autoReconnect: Boolean = true,
    val maxReconnect: Int = 0
)
