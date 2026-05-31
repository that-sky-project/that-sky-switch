package `fun`.colorsky.thatskyswitch

import android.app.Application
import android.util.Log
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class App : Application() {

    interface ServiceStateListener {
        fun onServiceStateChanged(service: XposedService?)
    }

    companion object {
        private const val TAG = "SkySwitchApp"
        private var sService: XposedService? = null
        private val sListeners = mutableSetOf<ServiceStateListener>()

        fun getService(): XposedService? = sService

        fun addServiceStateListener(listener: ServiceStateListener, notifyNow: Boolean) {
            sListeners.add(listener)
            if (notifyNow) listener.onServiceStateChanged(sService)
        }

        fun removeServiceStateListener(listener: ServiceStateListener) {
            sListeners.remove(listener)
        }
    }

    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                Log.i(TAG, "XposedService bound")
                sService = service
                for (l in sListeners) l.onServiceStateChanged(service)
            }

            override fun onServiceDied(service: XposedService) {
                Log.w(TAG, "XposedService died")
                sService = null
                for (l in sListeners) l.onServiceStateChanged(null)
            }
        })
    }
}
