package com.mdevz.sp.core.config

import android.content.Context
import com.mdevz.sp.core.model.PayloadMode
import com.mdevz.sp.core.model.SshProfile
import org.json.JSONObject

class SharedPreferencesProfileRepository(
    context: Context,
    private val credentialCipher:
        CredentialCipher = CredentialCipher()
) : ProfileRepository {

    private val preferences =
        context.applicationContext
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

    override fun save(
        id: String,
        profile: SshProfile
    ) {
        require(id.isNotBlank()) {
            "Profile id must not be blank"
        }

        val json =
            JSONObject().apply {
                put("name", profile.name)
                put("connectionType", profile.connectionType)
                put("xrayProtocol", profile.xrayProtocol)
                put("xrayAddress", profile.xrayAddress)
                put("xrayPort", profile.xrayPort)
                put("xrayUuidEncrypted", credentialCipher.encrypt(profile.xrayUuid))
                put("xrayAlterId", profile.xrayAlterId)
                put("xraySecurity", profile.xraySecurity)
                put("xrayNetwork", profile.xrayNetwork)
                put("xrayPath", profile.xrayPath)
                put("xrayHost", profile.xrayHost)
                put("xrayTls", profile.xrayTls)
                put("xraySni", profile.xraySni)
                put("xrayFlow", profile.xrayFlow)
                put("xrayPasswordEncrypted", credentialCipher.encrypt(profile.xrayPassword))
                put("xrayMethod", profile.xrayMethod)
                put("xrayRawJsonEncrypted", credentialCipher.encrypt(profile.xrayRawJson))
                put("oneringEnabled", profile.oneringEnabled)
                put("oneringOriginDomain", profile.oneringOriginDomain)
                put("oneringBugDomain", profile.oneringBugDomain)
                put("sshHost", profile.sshHost)
                put("sshPort", profile.sshPort)
                put("username", profile.username)

                put(
                    "passwordEncrypted",
                    credentialCipher.encrypt(
                        profile.password
                    )
                )

                put(
                    "payloadEnabled",
                    profile.payloadEnabled
                )

            put(
                "proxyEnabled",
                profile.proxyEnabled
            )

                put(
                    "payloadMode",
                    profile.payloadMode.name
                )

                put(
                    "payloadEncrypted",
                    credentialCipher.encrypt(
                        profile.payload
                    )
                )

                put(
                    "proxyHost",
                    profile.proxyHost
                )

                put(
                    "proxyPort",
                    profile.proxyPort
                )

                put(
                    "tlsEnabled",
                    profile.tlsEnabled
                )

                put("sni", profile.sni)

                put(
                    "tlsVersion",
                    profile.tlsVersion
                )

                put(
                    "localSocksPort",
                    profile.localSocksPort
                )

                put(
                    "keepAliveSeconds",
                    profile.keepAliveSeconds
                )

                put(
                    "socketSendBufferSize",
                    profile.socketSendBufferSize
                )

                put(
                    "socketReceiveBufferSize",
                    profile.socketReceiveBufferSize
                )

                put(
                    "tcpNoDelay",
                    profile.tcpNoDelay
                )

                put(
                    "cpuWakeLockEnabled",
                    profile.cpuWakeLockEnabled
                )

                put(
                    "connectTimeoutMillis",
                    profile.connectTimeoutMillis
                )

                put(
                    "readTimeoutMillis",
                    profile.readTimeoutMillis
                )

                put(
                    "autoReconnect",
                    profile.autoReconnect
                )

                put(
                    "maxReconnect",
                    profile.maxReconnect
                )
            }

        preferences.edit()
            .putString(
                profileKey(id),
                json.toString()
            )
            .apply()

        val ids =
            preferences
                .getStringSet(
                    KEY_IDS,
                    emptySet()
                )
                .orEmpty()
                .toMutableSet()

        ids += id

        preferences.edit()
            .putStringSet(
                KEY_IDS,
                ids
            )
            .apply()
    }

    override fun load(
        id: String
    ): SshProfile? {
        val raw =
            preferences.getString(
                profileKey(id),
                null
            ) ?: return null

        val json = JSONObject(raw)

        val proxyEnabled =
            if (json.has("proxyEnabled")) {
                json.getBoolean("proxyEnabled")
            } else {
                json.getBoolean("payloadEnabled") &&
                    json.optString("proxyHost", "").isNotBlank() &&
                    json.optInt("proxyPort", 0) in 1..65535
            }

        return SshProfile(
            name = json.getString("name"),
            connectionType = json.optString("connectionType", "SSH"),
            xrayProtocol = json.optString("xrayProtocol", "vmess"),
            xrayAddress = json.optString("xrayAddress", ""),
            xrayPort = json.optInt("xrayPort", 443),
            xrayUuid = runCatching { credentialCipher.decrypt(json.optString("xrayUuidEncrypted", "")) }.getOrDefault(""),
            xrayAlterId = json.optInt("xrayAlterId", 0),
            xraySecurity = json.optString("xraySecurity", "auto"),
            xrayNetwork = json.optString("xrayNetwork", "ws"),
            xrayPath = json.optString("xrayPath", "/"),
            xrayHost = json.optString("xrayHost", ""),
            xrayTls = json.optBoolean("xrayTls", true),
            xraySni = json.optString("xraySni", ""),
            xrayFlow = json.optString("xrayFlow", ""),
            xrayPassword = runCatching { credentialCipher.decrypt(json.optString("xrayPasswordEncrypted", "")) }.getOrDefault(""),
            xrayMethod = json.optString("xrayMethod", "aes-128-gcm"),
            xrayRawJson = runCatching { credentialCipher.decrypt(json.optString("xrayRawJsonEncrypted", "")) }.getOrDefault(""),
            oneringEnabled = json.optBoolean("oneringEnabled", false),
            oneringOriginDomain = json.optString("oneringOriginDomain", ""),
            oneringBugDomain = json.optString("oneringBugDomain", ""),

            sshHost =
                json.getString("sshHost"),

            sshPort =
                json.getInt("sshPort"),

            username =
                json.getString("username"),

            password =
                credentialCipher.decrypt(
                    json.getString(
                        "passwordEncrypted"
                    )
                ),

            payloadEnabled =
                json.getBoolean(
                    "payloadEnabled"
                ),

            proxyEnabled =
                proxyEnabled,
            payloadMode =
                PayloadMode.valueOf(
                    json.getString(
                        "payloadMode"
                    )
                ),

            payload =
                credentialCipher.decrypt(
                    json.getString(
                        "payloadEncrypted"
                    )
                ),

            proxyHost =
                json.getString("proxyHost"),

            proxyPort =
                json.getInt("proxyPort"),

            tlsEnabled =
                json.getBoolean(
                    "tlsEnabled"
                ),

            sni =
                json.getString("sni"),

            tlsVersion =
                json.optString(
                    "tlsVersion",
                    "default"
                ),

            localSocksPort =
                json.getInt(
                    "localSocksPort"
                ),

            keepAliveSeconds =
                json.getInt(
                    "keepAliveSeconds"
                ),

            socketSendBufferSize =
                json.optInt(
                    "socketSendBufferSize",
                    16_384
                ),

            socketReceiveBufferSize =
                json.optInt(
                    "socketReceiveBufferSize",
                    32_768
                ),

            tcpNoDelay =
                json.optBoolean(
                    "tcpNoDelay",
                    true
                ),

            cpuWakeLockEnabled =
                json.optBoolean(
                    "cpuWakeLockEnabled",
                    false
                ),

            connectTimeoutMillis =
                json.getInt(
                    "connectTimeoutMillis"
                ),

            readTimeoutMillis =
                json.getInt(
                    "readTimeoutMillis"
                ),

            autoReconnect =
                json.getBoolean(
                    "autoReconnect"
                ),

            maxReconnect =
                json.optInt(
                    "maxReconnect",
                    0
                )
        )
    }

    override fun delete(
        id: String
    ) {
        preferences.edit()
            .remove(profileKey(id))
            .remove("profile_lock.$id")
            .apply()

        val ids =
            preferences
                .getStringSet(
                    KEY_IDS,
                    emptySet()
                )
                .orEmpty()
                .toMutableSet()

        ids -= id

        preferences.edit()
            .putStringSet(
                KEY_IDS,
                ids
            )
            .apply()
    }

    override fun listIds(): List<String> =
        preferences
            .getStringSet(
                KEY_IDS,
                emptySet()
            )
            .orEmpty()
            .sorted()

    private fun profileKey(
        id: String
    ): String =
        "profile.$id"

    companion object {
        private const val PREFS =
            "ssh_protocol_profiles"

        private const val KEY_IDS =
            "profile_ids"
    }
}
