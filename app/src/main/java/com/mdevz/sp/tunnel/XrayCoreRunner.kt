package com.mdevz.sp.tunnel

import android.content.Context
import android.provider.Settings
import com.mdevz.sp.core.config.XrayConfigBuilder
import com.mdevz.sp.core.model.SshProfile
import go.Seq
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import java.io.File
import java.util.UUID

/**
 * Lifecycle adapter for dharak36/xray-onering's bundled libv2ray AAR.
 *
 * The AAR's CoreController.startLoop API takes exactly one String argument.
 * Its GeoIP/GeoSite AAR assets are copied to the private asset directory expected by
 * Libv2ray.initCoreEnv, matching the asset-directory convention used by v2rayNG.
 */
class XrayCoreRunner(context: Context) {
    private val appContext = context.applicationContext
    private var controller: CoreController? = null

    @Synchronized
    fun start(profile: SshProfile) {
        check(controller == null) { "Xray core is already running" }

        Seq.setContext(appContext)
        val assetDir = appContext.getDir("assets", Context.MODE_PRIVATE)
        copyCoreAssets(assetDir)

        val deviceId = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

        Libv2ray.initCoreEnv(assetDir.absolutePath, deviceId)

        val callback = object : CoreCallbackHandler {
            override fun startup(): Long = 0L
            override fun shutdown(): Long = 0L
            override fun onEmitStatus(status: Long, message: String?): Long = 0L
        }

        val core = Libv2ray.newCoreController(callback)
        val config = XrayConfigBuilder.build(profile, profile.localSocksPort)
        try {
            // Confirmed against libv2ray.aar: startLoop(String), not startLoop(String, Int).
            core.startLoop(config)
        } catch (failure: Throwable) {
            runCatching { core.stopLoop() }
            throw (failure.cause ?: failure)
        }

        controller = core
        if (!core.isRunning) {
            runCatching { stop() }
            throw IllegalStateException("Xray core failed to start")
        }
    }

    fun isRunning(): Boolean = controller?.isRunning == true

    @Synchronized
    fun stop() {
        val core = controller ?: return
        try {
            core.stopLoop()
        } finally {
            controller = null
        }
    }

    private fun copyCoreAssets(assetDir: File) {
        listOf("geoip.dat", "geosite.dat").forEach { name ->
            val destination = File(assetDir, name)
            if (!destination.isFile || destination.length() == 0L) {
                appContext.assets.open(name).use { input ->
                    destination.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
}
