package com.example.util

import com.example.data.preferences.StoreConfig
import java.util.Calendar

object WorkerScheduleHelper {

    /**
     * Determina si la hora y día actual corresponden al horario laboral
     * configurado por el dueño para este receptor / trabajador.
     */
    fun isWithinSchedule(
        scheduleEnabled: Boolean,
        startTime: String,
        endTime: String,
        days: String
    ): Boolean {
        if (!scheduleEnabled) return true // Sin restricción: 24/7 activo

        val now = Calendar.getInstance()

        // 1. Validar día de la semana si no es 'ALL'
        if (!days.equals("ALL", ignoreCase = true)) {
            val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
            val dayMap = mapOf(
                Calendar.MONDAY to "LUN",
                Calendar.TUESDAY to "MAR",
                Calendar.WEDNESDAY to "MIE",
                Calendar.THURSDAY to "JUE",
                Calendar.FRIDAY to "VIE",
                Calendar.SATURDAY to "SAB",
                Calendar.SUNDAY to "DOM"
            )
            val currentDayCode = dayMap[dayOfWeek] ?: ""
            val allowedDays = days.uppercase().split(",").map { it.trim() }
            if (currentDayCode.isNotBlank() && !allowedDays.contains(currentDayCode)) {
                return false // Hoy no es día de turno
            }
        }

        // 2. Validar rango de horas (HH:mm)
        return try {
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

            val startParts = startTime.split(":").map { it.trim().toInt() }
            val startMinutes = startParts[0] * 60 + startParts.getOrElse(1) { 0 }

            val endParts = endTime.split(":").map { it.trim().toInt() }
            val endMinutes = endParts[0] * 60 + endParts.getOrElse(1) { 0 }

            if (startMinutes <= endMinutes) {
                // Turno habitual mismo día (ej: 08:00 a 20:00)
                currentMinutes in startMinutes..endMinutes
            } else {
                // Turno nocturno que cruza medianoche (ej: 22:00 a 06:00)
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }
        } catch (_: Exception) {
            true // En caso de inconsistencia en el formato, permitir por precaución
        }
    }

    fun isConfigWithinSchedule(config: StoreConfig): Boolean {
        return isWithinSchedule(
            scheduleEnabled = config.workerScheduleEnabled,
            startTime = config.workerScheduleStartTime,
            endTime = config.workerScheduleEndTime,
            days = config.workerScheduleDays
        )
    }
}
