package com.pguindo.orofacial

import android.content.Context
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Fuente única de verdad del widget.
 *
 * SharedPreferences "orofacial_widget" con:
 *  - lastDate (yyyy-MM-dd): último día con sesión completada
 *  - sessionsToday (int)
 *  - totalSessions (int)
 *  - deadline (HH:mm, hora límite diaria, por defecto 20:00)
 *
 * El estado emocional se CALCULA en Kotlin (no se fía del JS):
 *  - CONTENTO: hecha hoy, o no hecha pero aún hay margen (now < deadline y now < 22:00 y ayer no fallado)
 *  - INQUIETO: no hecha hoy y now >= deadline y now < 22:00
 *  - LLOROSO: no hecha hoy y now >= 22:00 (gana al deadline)
 *  - DERROTADO: no hecha hoy y ayer fallado y now < deadline y now < 22:00 (mañana de culpa)
 */
object WidgetData {

    const val PREFS_NAME = "orofacial_widget"
    const val KEY_LAST_DATE = "lastDate"
    const val KEY_SESSIONS_TODAY = "sessionsToday"
    const val KEY_TOTAL = "totalSessions"
    const val KEY_DEADLINE = "deadline"

    const val DEFAULT_DEADLINE = "20:00"
    /** A partir de esta hora, si no está hecha, siempre LLOROSO (<2h para acabar el día). */
    const val LLOROSO_HOUR = 22

    private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

    enum class Mood { CONTENTO, INQUIETO, LLOROSO, DERROTADO }

    data class Snapshot(
        val lastDate: LocalDate?,
        val sessionsToday: Int,
        val totalSessions: Int,
        val deadline: LocalTime
    )

    fun load(context: Context): Snapshot {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = LocalDate.now()
        val lastDate = prefs.getString(KEY_LAST_DATE, null)?.let {
            try { LocalDate.parse(it, DATE_FMT) } catch (_: DateTimeParseException) { null }
        }
        // Si lastDate no es hoy, sessionsToday es 0 aunque el valor guardado sea viejo.
        val sessionsToday = if (lastDate == today) prefs.getInt(KEY_SESSIONS_TODAY, 0) else 0
        val total = prefs.getInt(KEY_TOTAL, 0)
        val deadline = parseTime(prefs.getString(KEY_DEADLINE, DEFAULT_DEADLINE))
        return Snapshot(lastDate, sessionsToday, total, deadline)
    }

    /**
     * Guarda el JSON enviado desde la web vía Android.guardarDato(json).
     * Acepta claves flexibles: {sessionsToday, totalSessions, lastDate, deadline}.
     * Ejemplo: {"sessionsToday":1,"totalSessions":5,"lastDate":"2026-09-26","deadline":"20:00"}
     */
    fun saveFromJson(context: Context, json: String): Boolean {
        return try {
            val o = JSONObject(json)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val ed = prefs.edit()

            if (o.has("lastDate") && !o.isNull("lastDate")) {
                ed.putString(KEY_LAST_DATE, o.optString("lastDate"))
            }
            if (o.has("sessionsToday")) {
                ed.putInt(KEY_SESSIONS_TODAY, o.optInt("sessionsToday", 0))
                // Si hay al menos 1 hoy y no trae lastDate, inferir hoy.
                if (o.optInt("sessionsToday", 0) > 0 &&
                    (!o.has("lastDate") || o.isNull("lastDate"))
                ) {
                    ed.putString(KEY_LAST_DATE, LocalDate.now().format(DATE_FMT))
                }
            }
            if (o.has("totalSessions")) {
                ed.putInt(KEY_TOTAL, o.optInt("totalSessions", 0))
            } else if (o.has("total")) {
                ed.putInt(KEY_TOTAL, o.optInt("total", 0))
            }
            if (o.has("deadline") && !o.isNull("deadline")) {
                ed.putString(KEY_DEADLINE, o.optString("deadline", DEFAULT_DEADLINE))
            } else if (o.has("horaLimite") && !o.isNull("horaLimite")) {
                ed.putString(KEY_DEADLINE, o.optString("horaLimite", DEFAULT_DEADLINE))
            }
            // "estado" se ignora a propósito: Kotlin lo recalcula.
            ed.apply()
            true
        } catch (e: Exception) {
            Log.w("WidgetData", "guardarDato JSON inválido: $json", e)
            false
        }
    }

    fun calcularEstado(
        snapshot: Snapshot,
        nowDate: LocalDate = LocalDate.now(),
        nowTime: LocalTime = LocalTime.now()
    ): Mood {
        val doneToday = snapshot.lastDate == nowDate && snapshot.sessionsToday > 0
        if (doneToday) return Mood.CONTENTO

        // 1) Lloroso gana a todo si no está hecha (incluido deadline futuro).
        if (nowTime.hour >= LLOROSO_HOUR) return Mood.LLOROSO
        // 2) Inquieto tras el deadline.
        if (!nowTime.isBefore(snapshot.deadline)) return Mood.INQUIETO
        // 3) Mañana con margen: derrotado si ayer se falló, si no contento.
        // Ayer fallado = existe lastDate y es anterior a ayer (nunca -> contento, es usuario nuevo).
        val yesterday = nowDate.minusDays(1)
        val missedYesterday = snapshot.lastDate != null && snapshot.lastDate.isBefore(yesterday)
        return if (missedYesterday) Mood.DERROTADO else Mood.CONTENTO
    }

    fun tituloFor(mood: Mood): String = when (mood) {
        Mood.CONTENTO -> "Contento"
        Mood.INQUIETO -> "Inquieto"
        Mood.LLOROSO -> "Lloroso"
        Mood.DERROTADO -> "Derrotado"
    }

    // Mensajes cortos y positivos; cada día se muestra uno distinto (rota por fecha).
    private val MENSAJES_HECHO = listOf("¡Sesión hecha!", "¡Gran trabajo!", "¡Lo conseguiste!", "Sigue así", "¡Bravo!")
    private val MENSAJES_MARGEN = listOf("Adelante, tú puedes", "Hoy es tu día", "A por ello", "Tú puedes", "Vamos, empieza")
    private val MENSAJES_INQUIETO = listOf("Aún estás a tiempo", "Hazla ahora", "No lo dejes", "Última llamada", "Vamos, hazla")
    private val MENSAJES_LLOROSO = listOf("¡Queda poco!", "¡Rápido, hazla!", "No te rindas", "Casi se acaba", "¡Ya casi!")
    private val MENSAJES_DERROTADO = listOf("Hoy sí", "Nuevo día", "Recupérala hoy", "Empieza de nuevo", "Hoy lo logras")

    fun mensajeFor(
        mood: Mood,
        snapshot: Snapshot,
        date: LocalDate = LocalDate.now()
    ): String {
        val doneToday = snapshot.lastDate == date && snapshot.sessionsToday > 0
        val pool = when (mood) {
            Mood.CONTENTO -> if (doneToday) MENSAJES_HECHO else MENSAJES_MARGEN
            Mood.INQUIETO -> MENSAJES_INQUIETO
            Mood.LLOROSO -> MENSAJES_LLOROSO
            Mood.DERROTADO -> MENSAJES_DERROTADO
        }
        val idx = ((date.toEpochDay() % pool.size).toInt() + pool.size) % pool.size
        return pool[idx]
    }

    private fun parseTime(raw: String?): LocalTime {
        if (raw.isNullOrBlank()) return LocalTime.of(20, 0)
        return try {
            // Acepta "20:00" y "20".
            if (raw.trim().contains(":")) LocalTime.parse(raw.trim(), TIME_FMT)
            else LocalTime.of(raw.trim().toInt().coerceIn(0, 23), 0)
        } catch (_: Exception) {
            LocalTime.of(20, 0)
        }
    }
}
