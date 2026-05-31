package `fun`.colorsky.thatskyswitch.hooks

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface


class SkipVeriftCert: XposedModule() {
    companion object {
        private const val TAG = "SkyServerSwitch"
        private const val TARGET = "com.tgc.sky.android"
    }

    fun initHook() {
        System.loadLibrary("thatskyswitch")
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        val prefs = getRemotePreferences("verify_config")
        val isSkipVerify = prefs.getBoolean("skip", false)

        if (!param.isFirstPackage) return
         if (!isSkipVerify) return

        if (param.packageName==TARGET) {
                try {
                    initHook()
                    log(Log.INFO, TAG, "Shadow Hook init.")

                } catch (e: Throwable) {
                    log(Log.ERROR, TAG, e.toString())
                }
        }
    }
}