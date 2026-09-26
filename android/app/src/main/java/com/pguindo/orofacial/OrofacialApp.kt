package com.pguindo.orofacial

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Programa el refresco periódico del widget (cada 15 min, mínimo permitido por WorkManager).
 * Así las transiciones deadline -> inquieto, 22:00 -> lloroso y 00:00 -> derrotado
 * ocurren sin necesidad de abrir la app.
 */
class OrofacialApp : Application() {

    companion object {
        private const val UNIQUE_WORK = "orofacial-widget-refresh"
    }

    override fun onCreate() {
        super.onCreate()
        val request = PeriodicWorkRequestBuilder<OrofacialWidgetWorker>(15, TimeUnit.MINUTES)
            .addTag(UNIQUE_WORK)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
