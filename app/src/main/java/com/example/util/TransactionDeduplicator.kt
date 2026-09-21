package com.example.util

import android.util.Log
import com.example.data.db.TransactionDao
import com.example.data.model.YapeTransaction
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

/**
 * Motor centralizado y robusto de deduplicación de transacciones Yape/Plin.
 *
 * Previene el registro duplicado de pagos originados por:
 *  - Llegada tardía de mensajes Push FCM cuando el dispositivo estuvo desconectado o en Doze mode.
 *  - Sincronización REST previa (`syncRemoteReceiver` / `fetchAllTransactionsForStore`).
 *  - Eventos de Supabase Realtime WebSocket (`applyRealtimeTransactionUpdate`).
 *  - Variaciones de mayúsculas/minúsculas y tildes (Unicode NFD) en nombres de emisores.
 *  - Diferencias o desfases de reloj entre dispositivos emisor y receptor.
 */
object TransactionDeduplicator {

    private const val TAG = "TransactionDeduplicator"

    /**
     * Busca una transacción idéntica o coincidente en Room.
     *
     * Jerarquía de búsqueda:
     *  1. Coincidencia exacta por [remoteId] (si no está vacío).
     *  2. Coincidencia por consulta SQL Room [findDuplicate] (prioriza remoteId, ventana de 5m).
     *  3. Búsqueda profunda en candidatos en ventana de ±12 horas:
     *      - Coincidencia de código de seguridad Yape (ej. "482").
     *      - Coincidencia de texto crudo de notificación (`rawNotification`).
     *      - Coincidencia de nombre de emisor normalizado sin tildes dentro de 2 horas o mismo día.
     */
    suspend fun findDuplicate(
        dao: TransactionDao,
        timestamp: Long,
        amount: Double,
        senderName: String,
        rawNotification: String = "",
        storeCode: String = "",
        remoteId: String = ""
    ): YapeTransaction? {
        val trimmedRemoteId = remoteId.trim()

        // 1. FAST PATH: Coincidencia directa por remoteId de Supabase
        if (trimmedRemoteId.isNotBlank()) {
            val byRemote = dao.findByRemoteId(trimmedRemoteId)
            if (byRemote != null) {
                Log.d(TAG, "findDuplicate: Match por remoteId exacto '$trimmedRemoteId' -> localId=${byRemote.id}")
                return byRemote
            }
        }

        // 2. Consulta SQL Room (rápida e indexada)
        val sqlDuplicate = dao.findDuplicate(
            timestamp = timestamp,
            amount = amount,
            senderName = senderName.trim(),
            remoteId = trimmedRemoteId
        )
        if (sqlDuplicate != null) {
            Log.d(TAG, "findDuplicate: Match por Room SQL -> localId=${sqlDuplicate.id}, remoteId=${sqlDuplicate.remoteId}")
            return sqlDuplicate
        }

        // 3. BÚSQUEDA PROFUNDA DE CANDIDATOS (tolerante a retrasos de red, corte de internet y tildes)
        val timeWindow = 12 * 60 * 60 * 1000L // Ventana de 12 horas
        val baseTime = if (timestamp > 0L) timestamp else System.currentTimeMillis()
        val minTime = baseTime - timeWindow
        val maxTime = baseTime + timeWindow

        val candidates = dao.findCandidateDuplicates(
            amount = amount,
            minTimestamp = minTime,
            maxTimestamp = maxTime,
            storeCode = storeCode.trim()
        )

        if (candidates.isEmpty()) return null

        val incomingNormalizedName = normalizeSenderName(senderName)
        val incomingSecCode = extractSecurityCode(rawNotification)

        for (candidate in candidates) {
            // A. Coincidencia por remoteId si ambos lo tienen
            if (trimmedRemoteId.isNotBlank() && candidate.remoteId.isNotBlank() &&
                candidate.remoteId.equals(trimmedRemoteId, ignoreCase = true)
            ) {
                Log.d(TAG, "findDuplicate: Candidato match por remoteId -> localId=${candidate.id}")
                return candidate
            }

            // B. Coincidencia por Código de Seguridad de 3 o 4 dígitos de Yape
            val candidateSecCode = candidate.securityCode ?: extractSecurityCode(candidate.rawNotification)
            if (!incomingSecCode.isNullOrBlank() && !candidateSecCode.isNullOrBlank()) {
                if (incomingSecCode == candidateSecCode) {
                    Log.d(TAG, "findDuplicate: Match por código de seguridad '$incomingSecCode' -> localId=${candidate.id}")
                    return candidate
                }
            }

            // C. Coincidencia exacta de texto crudo de notificación
            if (rawNotification.isNotBlank() && candidate.rawNotification.isNotBlank()) {
                if (rawNotification.trim().equals(candidate.rawNotification.trim(), ignoreCase = true)) {
                    Log.d(TAG, "findDuplicate: Match por rawNotification exacto -> localId=${candidate.id}")
                    return candidate
                }
            }

            // D. Coincidencia por nombre de emisor normalizado (sin tildes ni diacríticos)
            val candidateNormalizedName = normalizeSenderName(candidate.senderName)
            val namesMatch = isNameMatch(incomingNormalizedName, candidateNormalizedName)

            if (namesMatch) {
                val timeDiff = abs(candidate.timestamp - timestamp)
                // Si los nombres coinciden y ocurrieron dentro de 2 horas
                if (timeDiff <= 2 * 60 * 60 * 1000L) {
                    Log.d(TAG, "findDuplicate: Match por nombre normalizado ('$incomingNormalizedName' vs '$candidateNormalizedName', diff=${timeDiff / 1000}s) -> localId=${candidate.id}")
                    return candidate
                }

                // Si la notificación FCM no trajo timestamp original (usó currentTimeMillis)
                // y coincide dentro del mismo día calendario con el mismo monto y emisor
                if (isSameCalendarDay(candidate.timestamp, timestamp)) {
                    Log.d(TAG, "findDuplicate: Match por mismo día y emisor ('$incomingNormalizedName') -> localId=${candidate.id}")
                    return candidate
                }
            }
        }

        return null
    }

    /**
     * Normaliza nombres eliminando tildes, diacríticos, caracteres especiales y colapsando espacios.
     * Ej: " JOSÉ ÁNGEL ROMERO " -> "jose angel romero"
     */
    fun normalizeSenderName(name: String): String {
        if (name.isBlank()) return ""
        val nfd = Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
        val withoutAccents = nfd.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return withoutAccents.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Compara dos nombres normalizados con tolerancia a diferencias de orden o apellidos truncados.
     */
    fun isNameMatch(name1: String, name2: String): Boolean {
        if (name1.isBlank() || name2.isBlank()) return false
        if (name1 == name2) return true
        if (name1.contains(name2) || name2.contains(name1)) return true

        val words1 = name1.split(" ").filter { it.length >= 3 }
        val words2 = name2.split(" ").filter { it.length >= 3 }
        if (words1.isEmpty() || words2.isEmpty()) return false

        val common = words1.intersect(words2.toSet())
        val minWords = words1.size.coerceAtMost(words2.size)
        return common.isNotEmpty() && common.size >= minWords
    }

    /**
     * Extrae el código de seguridad de 3 o 4 dígitos de la notificación Yape si está presente.
     */
    fun extractSecurityCode(raw: String): String? {
        if (raw.isBlank()) return null
        val regex = Regex(
            """(?:c[oó]d(?:igo|\.)?(?:\s+de\s+seguridad)?(?:\s+es)?|seguridad(?:\s+es)?)\s*[:#-]?\s*([0-9]{3,4})\b""",
            RegexOption.IGNORE_CASE
        )
        return regex.find(raw)?.groupValues?.get(1)
    }

    /**
     * Verifica si dos timestamps pertenecen a la misma fecha calendario local.
     */
    fun isSameCalendarDay(t1: Long, t2: Long): Boolean {
        if (t1 <= 0L || t2 <= 0L) return false
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
