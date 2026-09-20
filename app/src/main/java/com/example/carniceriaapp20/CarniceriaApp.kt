package com.example.carniceriaapp20

import android.app.Application
import com.example.carniceriaapp20.util.AppLog
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CarniceriaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)

        // Un cierre inesperado queda en el registro (exportable) y luego sigue el manejo normal de Android.
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AppLog.e("CRASH", "Cierre inesperado en el hilo ${thread.name}", throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }
}
