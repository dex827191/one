package com.mdevz.sp.tunnel.config

import com.mdevz.sp.core.model.PayloadMode
import com.mdevz.sp.core.model.SshProfile

data class ConnectionSnapshot(
    val connectionType: String,
    val xrayProtocol: String,
    val xrayServer: String,
    val xrayPort: Int,
    val xrayUuid: String,
    val xrayPassword: String,
    val xraySecurity: String,
    val xrayNetwork: String,
    val xrayHost: String,
    val xrayPath: String,
    val xrayServerName: String,
    val xrayFingerprint: String,
    val xrayAllowInsecure: Boolean,
    val sshHost: String,
    val sshPort: Int,
    val username: String,
    val password: String,
    val payloadEnabled: Boolean,
    val payloadMode: PayloadMode,
    val payload: String,
    val proxyEnabled: Boolean,
    val proxyHost: String,
    val proxyPort: Int,
    val tlsEnabled: Boolean,
    val sni: String,
    val tlsVersion: String,
    val localSocksPort: Int,
    val keepAliveSeconds: Int,
    val socketSendBufferSize: Int,
    val socketReceiveBufferSize: Int,
    val tcpNoDelay: Boolean,
    val connectTimeoutMillis: Int,
    val readTimeoutMillis: Int
) {
    companion object {
        fun fromProfile(
            profile: SshProfile
        ): ConnectionSnapshot =
            ConnectionSnapshot(
                connectionType = profile.connectionType,
                xrayProtocol = profile.xrayProtocol,
                xrayServer = profile.xrayServer,
                xrayPort = profile.xrayPort,
                xrayUuid = profile.xrayUuid,
                xrayPassword = profile.xrayPassword,
                xraySecurity = profile.xraySecurity,
                xrayNetwork = profile.xrayNetwork,
                xrayHost = profile.xrayHost,
                xrayPath = profile.xrayPath,
                xrayServerName = profile.xrayServerName,
                xrayFingerprint = profile.xrayFingerprint,
                xrayAllowInsecure = profile.xrayAllowInsecure,
                sshHost = profile.sshHost,
                sshPort = profile.sshPort,
                username = profile.username,
                password = profile.password,
                payloadEnabled = profile.payloadEnabled,
                payloadMode = profile.payloadMode,
                payload = profile.payload,
                proxyEnabled = profile.proxyEnabled,
                proxyHost = profile.proxyHost,
                proxyPort = profile.proxyPort,
                tlsEnabled = profile.tlsEnabled,
                sni = profile.sni,
                tlsVersion = profile.tlsVersion,
                localSocksPort = profile.localSocksPort,
                keepAliveSeconds = profile.keepAliveSeconds,
                socketSendBufferSize = profile.socketSendBufferSize,
                socketReceiveBufferSize = profile.socketReceiveBufferSize,
                tcpNoDelay = profile.tcpNoDelay,
                connectTimeoutMillis = profile.connectTimeoutMillis,
                readTimeoutMillis = profile.readTimeoutMillis
            )
    }
}
