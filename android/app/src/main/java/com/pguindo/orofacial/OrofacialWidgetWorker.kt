package com.pguindo.orofacial

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Re-evalúa el widget periódicamente aunque la app esté cerrada.
 * Imprescindible porque los estados dependen de la hora (deadline, 22:00, 00:00).
 */
class OrofacialWidgetWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        OrofacialWidgetProvider.requestUpdate(applicationContext)
        return Result.success()
    }
}
