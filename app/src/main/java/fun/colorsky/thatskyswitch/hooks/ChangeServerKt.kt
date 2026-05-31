package `fun`.colorsky.thatskyswitch.hooks

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class ChangeServerKt : XposedModule() {
    companion object {
        private const val TAG = "SkyServerSwitch"
        private const val TARGET = "com.tgc.sky.android"
        private const val DEFAULT_HOST = "live.radiance.thatgamecompany.com"
    }

    override fun onModuleLoaded(param: ModuleLoadedParam) {}

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onPackageLoaded(param: PackageLoadedParam) {}

    override fun onPackageReady(param: PackageReadyParam) {

        if (param.packageName != TARGET) return

        if (!param.isFirstPackage) return

        log(Log.INFO, TAG, "Target package loaded: ${param.packageName}")

        val prefs = getRemotePreferences("server_config")
        val store = prefs
            .getString("hostname", DEFAULT_HOST)

        val hostname = if (store?.isEmpty() ?: true) { DEFAULT_HOST } else { store }

        log(Log.INFO, TAG, "Target hostname: $hostname")

        try {
            val clazz = Class.forName(
                "com.tgc.sky.BuildConfig",
                true,
                param.classLoader
            )
            val field = clazz.getDeclaredField("SKY_SERVER_HOSTNAME")
            field.isAccessible = true
            field.set(null, hostname)
            log(Log.INFO, TAG, "Server hostname switch to $hostname")
        } catch (e: Exception) {
            log(Log.ERROR, TAG, "Server hostname failed: ${e.message}")
        }
    }
}
