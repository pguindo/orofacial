package com.pguindo.orofacial

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Widget clásico (RemoteViews) que muestra el personaje según el estado del día.
 * Lee SharedPreferences "orofacial_widget" y se refresca cuando:
 *  - la web llama a Android.guardarDato(json) -> requestUpdate()
 *  - el Worker periódico (15 min) lo re-evalúa (deadline / 22:00 / medianoche con app cerrada)
 */
class OrofacialWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.pguindo.orofacial.ACTION_WIDGET_REFRESH"

        fun requestUpdate(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val cn = ComponentName(context, OrofacialWidgetProvider::class.java)
            val ids = mgr.getAppWidgetIds(cn)
            if (ids.isEmpty()) return
            val intent = Intent(context, OrofacialWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }

        internal fun renderOne(context: Context, mgr: AppWidgetManager, appWidgetId: Int) {
            val snapshot = WidgetData.load(context)
            val mood = WidgetData.calcularEstado(snapshot)

            val views = RemoteViews(context.packageName, R.layout.widget_orofacial)
            views.setImageViewResource(R.id.widget_mood, drawableFor(mood))
            views.setContentDescription(R.id.widget_mood, WidgetData.tituloFor(mood))
            views.setTextViewText(R.id.widget_message, WidgetData.mensajeFor(mood, snapshot))

            // Toque en el widget -> abrir la app (WebView).
            val openApp = Intent(context, MainActivity::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pi = PendingIntent.getActivity(context, 0, openApp, flags)
            views.setOnClickPendingIntent(R.id.widget_root, pi)

            mgr.updateAppWidget(appWidgetId, views)
        }

        private fun drawableFor(mood: WidgetData.Mood): Int = when (mood) {
            WidgetData.Mood.CONTENTO -> R.drawable.ic_mood_contento
            WidgetData.Mood.INQUIETO -> R.drawable.ic_mood_inquieto
            WidgetData.Mood.LLOROSO -> R.drawable.ic_mood_lloroso
            WidgetData.Mood.DERROTADO -> R.drawable.ic_mood_derrotado
        }
    }

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) renderOne(context, mgr, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            requestUpdate(context)
        }
    }
}
