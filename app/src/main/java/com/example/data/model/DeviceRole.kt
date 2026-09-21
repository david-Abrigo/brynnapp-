package com.example.data.model

enum class DeviceRole(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String
) {
    LOCAL_SPEAKER(
        id = "LOCAL_SPEAKER",
        title = "Solo Locutor Local (Caja)",
        subtitle = "Anuncio por voz autónomo y privado",
        description = "Lee en voz alta el monto y nombre de cada Yape recibido en este teléfono. 100% privado, no requiere internet ni configuración en la nube."
    ),
    SENDER(
        id = "SENDER",
        title = "Emisor + Nube (Principal)",
        subtitle = "Teléfono en tienda con respaldo en la nube",
        description = "Lee notificaciones de Yape en este teléfono, reproduce voz y sube los pagos a la nube en tiempo real para visualización en otros dispositivos."
    ),
    RECEIVER(
        id = "RECEIVER",
        title = "Receptor / Espejo (Remoto)",
        subtitle = "Monitoreo a distancia",
        description = "Recibe pagos a distancia en tiempo real, genera notificaciones push, anuncia por voz y actualiza el widget en vivo."
    )
}
