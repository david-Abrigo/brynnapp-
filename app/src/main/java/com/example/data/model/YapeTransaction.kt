package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "yape_transactions")
data class YapeTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderName: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val rawNotification: String = "",
    val transactionType: String = "RECEIVED", // RECEIVED, TEST
    val note: String = "",
    val storeCode: String = "",
    val isSynced: Boolean = false,
    val remoteId: String = "",
    val syncTimestamp: Long = 0L,
    val isStoreTransaction: Boolean = true, // true = Venta del negocio/tienda, false = Desconocido / No pertenece a la tienda
    val exclusionReason: String = "", // Motivo de exclusión opcional (ej. "Personal", "Desconocido", etc.)
    val branchName: String = "", // Sucursal que confirmó el pago (ej. "Sucursal Centro")
    val claimedBy: String = "", // ID del usuario/trabajador que confirmó el pago
    val claimedByName: String = "", // Nombre legible de quien confirmó
    val claimedAt: Long = 0L // Timestamp de confirmación
) {
    val isRemote: Boolean
        get() = remoteId.isNotBlank() || rawNotification.contains("FCM", ignoreCase = true) || rawNotification.contains("remote", ignoreCase = true)

    val isUnknownOrNonStore: Boolean
        get() = !isStoreTransaction

    val isConfirmed: Boolean
        get() = branchName.isNotBlank() || claimedAt > 0L || claimedByName.isNotBlank() || claimedBy.isNotBlank()
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("hh:mm a", Locale("es", "PE"))
            return sdf.format(Date(timestamp))
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE"))
            return sdf.format(Date(timestamp))
        }

    val formattedAmount: String
        get() = String.format(Locale("es", "PE"), "S/ %.2f", amount)

    val dateKey: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    /**
     * Extrae el código de seguridad de 3 dígitos de la notificación si existiera.
     * Ejemplo de notificación Yape: "Código de seguridad: 123", "Cod: 123", "código: 123", o patrón de 3 dígitos explícito.
     */
    val securityCode: String?
        get() {
            // 1. Buscar "cód[igo] [de seguridad] [es] 123"
            val explicitRegex = Regex("""(?:c[oó]d(?:igo|\.)?(?:\s+de\s+seguridad)?(?:\s+es)?)\s*[:#-]?\s*([0-9]{3,4})\b""", RegexOption.IGNORE_CASE)
            val match = explicitRegex.find(rawNotification)
            if (match != null) {
                return match.groupValues[1]
            }

            // 2. Buscar patrón "seguridad [es] 123"
            val segRegex = Regex("""seguridad(?:\s+es)?\s*[:#-]?\s*([0-9]{3,4})\b""", RegexOption.IGNORE_CASE)
            val segMatch = segRegex.find(rawNotification)
            if (segMatch != null) {
                return segMatch.groupValues[1]
            }

            return null
        }

    /**
     * Retorna true si el emisor o la notificación contiene un asterisco '*',
     * indicando privacidad/ocultamiento de apellidos típico de Yape oficial.
     */
    val hasAsterisk: Boolean
        get() = senderName.contains("*") || rawNotification.contains("*")

    /**
     * Verifica si el usuario actual es quien confirmó/reclamó este pago.
     * Permite desmarcar solo al trabajador que realizó la confirmación.
     */
    fun isClaimedByUser(userId: String, userName: String = "", userEmail: String = ""): Boolean {
        // Si no está confirmada, no aplica restricción
        if (!isConfirmed) return true

        // 1. Si no hay registro de quién lo confirmó (pago antiguo o local sin autor), permitir
        if (claimedBy.isBlank() && claimedByName.isBlank()) {
            return true
        }

        // 2. Comparación prioritaria por ID único de usuario (UUID)
        if (claimedBy.isNotBlank() && userId.isNotBlank()) {
            if (claimedBy.equals(userId.trim(), ignoreCase = true)) {
                return true
            }
        }

        // 3. Comparación complementaria por nombre o email
        val trimmedName = userName.trim()
        val trimmedEmail = userEmail.trim()
        val who = claimedByName.trim()

        if (who.isNotBlank()) {
            if (trimmedName.isNotBlank() && who.equals(trimmedName, ignoreCase = true)) {
                return true
            }
            if (trimmedEmail.isNotBlank()) {
                if (who.equals(trimmedEmail, ignoreCase = true) ||
                    who.equals(trimmedEmail.substringBefore("@"), ignoreCase = true)
                ) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Retorna true si este pago fue efectivamente confirmado/cobrado por el usuario dado.
     */
    fun isConfirmedBy(userId: String, userName: String = "", userEmail: String = ""): Boolean {
        if (!isConfirmed) return false
        if (claimedBy.isNotBlank() && userId.isNotBlank()) {
            if (claimedBy.equals(userId.trim(), ignoreCase = true)) {
                return true
            }
        }
        val trimmedName = userName.trim()
        val trimmedEmail = userEmail.trim()
        val who = claimedByName.trim()
        if (who.isNotBlank()) {
            if (trimmedName.isNotBlank() && who.equals(trimmedName, ignoreCase = true)) {
                return true
            }
            if (trimmedEmail.isNotBlank()) {
                if (who.equals(trimmedEmail, ignoreCase = true) ||
                    who.equals(trimmedEmail.substringBefore("@"), ignoreCase = true)
                ) {
                    return true
                }
            }
        }
        return false
    }
}

