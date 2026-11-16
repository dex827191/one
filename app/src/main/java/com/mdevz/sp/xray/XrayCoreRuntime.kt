package com.mdevz.sp.xray

import android.content.Context
import android.provider.Settings
import go.Seq
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Thin lifecycle wrapper for dharak36/xray-onering's libv2ray.aar.
 *
 * This runs Xray in proxy-only mode (TUN fd = 0). The caller must provide a
 * complete Xray JSON config with a local SOCKS inbound and route device traffic
 * to that SOCKS port. It is intentionally independent from the SSH engine.
 */
class XrayCoreRuntime(context: Context) {
    private val appContext = context.applicationContext
    private val initialized = AtomicBoolean(false)
    @Volatile private var controller: CoreController? = null

    @Synchronized
    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        try {
            Seq.setContext(appContext)
            val assets = appContext.filesDir.absolutePath
            val deviceId = Settings.Secure.getString(
                appContext.contentResolver,
                Settings.Secure.ANDROID_ID
            ).orEmpty()
            Libv2ray.initCoreEnv(assets, deviceId)
        } catch (t: Throwable) {
            initialized.set(false)
            throw IllegalStateException("Unable to initialize Onering Xray runtime", t)
        }
    }

    @Synchronized
    fun start(configJson: String) {
        require(configJson.isNotBlank()) { "Xray config JSON is empty" }
        check(controller?.isRunning != true) { "Xray is already running" }
        initialize()
        val instance = Libv2ray.newCoreController(object : CoreCallbackHandler {
            override fun startup(): Long = 0L
            override fun shutdown(): Long = 0L
            override fun onEmitStatus(code: Long, message: String?): Long = 0L
        })
        try {
            instance.startLoop(configJson)
            check(instance.isRunning) { "Xray core did not enter running state" }
            controller = instance
        } catch (t: Throwable) {
            runCatching { instance.stopLoop() }
            throw IllegalStateException("Failed to start Xray/Onering: ${t.message}", t)
        }
    }

    @Synchronized
    fun stop() {
        val active = controller ?: return
        try {
            if (active.isRunning) active.stopLoop()
        } finally {
            controller = null
        }
    }

    fun isRunning(): Boolean = controller?.isRunning == true

    fun version(): String {
        initialize()
        return Libv2ray.checkVersionX()
    }
}
