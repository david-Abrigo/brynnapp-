package com.example.service.parser

import java.util.regex.Pattern

sealed class ParseResult {
    data class Success(
        val senderName: String,
        val amount: Double,
        val rawNotification: String
    ) : ParseResult()

    data class Ignored(
        val reason: String,
        val rawNotification: String
    ) : ParseResult()
}

object YapeNotificationParser {

    private val IGNORED_KEYWORDS = listOf(
        "enviaste",
        "yapeaste",
        "pago enviado",
        "transferencia enviada",
        "envío a",
        "envio a",
        "no se pudo",
        "fallido",
        "fallida",
        "rechazado",
        "rechazada",
        "error al",
        "código de validación",
        "clave dinámica",
        "clave dinamica",
        "préstamo",
        "prestamo",
        "gana con yape",
        "promoción",
        "promocion",
        "descuento",
        "recarga tu celular",
        "paga tus servicios",
        "notiyape",
        "confirmado por yape • cobro registrado",
        "generado por notiyape cloud"
    )

    // Regex patterns for matching Yape payment receipts
    private val AMOUNT_PATTERN = Pattern.compile(
        """(?:S\/?\.?\s*|\$|soles\s*)?([0-9]+(?:[.,][0-9]{1,2})?)\s*(?:soles|sol|PEN)?""",
        Pattern.CASE_INSENSITIVE
    )

    private val STRICT_AMOUNT_PATTERN = Pattern.compile(
        """(?:S\/?\.?\s*)([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?|[0-9]+(?:,[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(title: String?, text: String?, subText: String? = null, packageName: String? = null): ParseResult {
        val fullContent = listOfNotNull(title, text, subText)
            .joinToString(" ")
            .trim()

        if (fullContent.isBlank()) {
            return ParseResult.Ignored("Notificación vacía", "")
        }

        val lowerContent = fullContent.lowercase()

        // 1. Validate negative keywords
        for (ignored in IGNORED_KEYWORDS) {
            if (lowerContent.contains(ignored)) {
                return ParseResult.Ignored("Notificación no corresponde a pago recibido: contiene '$ignored'", fullContent)
            }
        }

        // 2. Validate positive reception indicators
        val isReceivedPayment = lowerContent.contains("te yapeó") ||
                lowerContent.contains("te yapeo") ||
                lowerContent.contains("te yapearon") ||
                lowerContent.contains("te envió") ||
                lowerContent.contains("te envio") ||
                lowerContent.contains("te enviaron") ||
                lowerContent.contains("recibiste") ||
                lowerContent.contains("pago recibido") ||
                lowerContent.contains("confirmamos que") ||
                lowerContent.contains("te transfirió") ||
                lowerContent.contains("te transfirio") ||
                lowerContent.contains("yape recibido") ||
                lowerContent.contains("plin") ||
                lowerContent.contains("confirmado") ||
                ((packageName?.contains("yape") == true || packageName?.contains("bcp") == true || packageName?.contains("plin") == true) && (lowerContent.contains("s/") || lowerContent.contains("soles")))

        if (!isReceivedPayment) {
            return ParseResult.Ignored("No se detectaron palabras clave de recepción de dinero en Yape/Plin", fullContent)
        }

        // 3. Extract Amount
        val amount = extractAmount(fullContent)
            ?: return ParseResult.Ignored("No se pudo identificar el monto del pago en el texto", fullContent)

        if (amount <= 0.0) {
            return ParseResult.Ignored("El monto extraído debe ser mayor a 0", fullContent)
        }

        // 4. Extract Sender Name
        val sender = extractSender(title ?: "", text ?: "", fullContent)

        return ParseResult.Success(
            senderName = sender,
            amount = amount,
            rawNotification = fullContent
        )
    }

    private fun extractAmount(content: String): Double? {
        // First try to match explicit S/ pattern
        val strictMatcher = STRICT_AMOUNT_PATTERN.matcher(content)
        if (strictMatcher.find()) {
            val rawNum = strictMatcher.group(1)?.replace(",", "")?.trim()
            rawNum?.toDoubleOrNull()?.let { return it }
        }

        // Match general pattern around "S/" or "soles"
        val generalRegex = Regex("""S\/?\.?\s*([0-9]+(?:[.,][0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        val match = generalRegex.find(content)
        if (match != null) {
            val rawNum = match.groupValues[1].replace(",", ".").trim()
            rawNum.toDoubleOrNull()?.let { return it }
        }

        // Fallback search for numbers following 'por', 'de', 'envió'
        val fallbackRegex = Regex("""(?:por|monto de|envio|envió|enviaron)\s*(?:S\/?\.?\s*)?([0-9]+(?:[.,][0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        val fallbackMatch = fallbackRegex.find(content)
        if (fallbackMatch != null) {
            val rawNum = fallbackMatch.groupValues[1].replace(",", ".").trim()
            rawNum.toDoubleOrNull()?.let { return it }
        }

        return null
    }

    private fun extractSender(title: String, text: String, fullContent: String): String {
        val nameChars = """[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9\s\.\*\-\'\&]"""

        // Pattern 1: "¡Te yapearon! <Nombre> te envió S/ 10" or "<Nombre> te yapeó S/ 10" or "Confirmación de Pago <Nombre> te envió..."
        val yapeoMatcher = Regex("""(?:¡?Te yapearon!?\s*)?($nameChars+?)\s+(?:te envió|te envio|te enviaron|te yapeó|te yapeo|te transfirió|te transfirio)""", RegexOption.IGNORE_CASE)
        val match1 = yapeoMatcher.find(fullContent)
        if (match1 != null) {
            val candidate = cleanSenderName(match1.groupValues[1])
            if (candidate.isNotBlank() && candidate.length > 1 && !isGenericHeader(candidate)) {
                return candidate
            }
        }

        // Pattern 2: "Recibiste un Yape de <Nombre> por S/ 20" or "Pago recibido de <Nombre> por S/ 20"
        val deMatcher = Regex("""(?:recibiste un yape de|recibiste un plin de|pago recibido de|recibiste de|de)\s+($nameChars+?)(?:\s+por|\s+de|\s+S\/|\s*$|\.)""", RegexOption.IGNORE_CASE)
        val match2 = deMatcher.find(fullContent)
        if (match2 != null) {
            val candidate = cleanSenderName(match2.groupValues[1])
            if (candidate.isNotBlank() && candidate.length > 1 && !isGenericHeader(candidate)) {
                return candidate
            }
        }

        // Pattern 3: If title contains sender name (e.g., Title: "Juan Pérez", Text: "Te envió S/ 25.00")
        if (title.isNotBlank() && !isGenericHeader(title)) {
            val candidate = cleanSenderName(title)
            if (candidate.isNotBlank() && candidate.length > 1 && !isGenericHeader(candidate)) {
                return candidate
            }
        }

        // Pattern 4: Look for words before "te envió" or "te yapeó" in text
        val textYapeMatch = Regex("""^($nameChars+?)\s+(?:te envió|te envio|te enviaron|te yapeó|te yapeo)""", RegexOption.IGNORE_CASE).find(text)
        if (textYapeMatch != null) {
            val candidate = cleanSenderName(textYapeMatch.groupValues[1])
            if (candidate.isNotBlank() && candidate.length > 1 && !isGenericHeader(candidate)) {
                return candidate
            }
        }

        return "Cliente Yape/Plin"
    }

    private fun isGenericHeader(text: String): Boolean {
        val lower = text.lowercase().trim()
        val genericList = listOf(
            "yape", "yape!", "plin", "plin!", "bcp", "alerta bcp",
            "confirmación de pago", "confirmacion de pago",
            "notificación de pago", "notificacion de pago",
            "pago recibido", "te yapearon", "confirmamos que",
            "transferencia recibida", "cliente yape", "cliente yape/plin"
        )
        return genericList.any { lower == it || (lower.startsWith(it) && lower.length == it.length) }
    }

    private fun cleanSenderName(raw: String): String {
        var cleaned = raw.replace("¡", "")
            .replace("!", "")
            .replace(":", "")
            .replace("Yape", "", ignoreCase = true)
            .replace("Plin", "", ignoreCase = true)
            .replace("Notificación", "", ignoreCase = true)
            .replace("BCP", "", ignoreCase = true)
            .trim()

        val leadingNoise = listOf(
            "confirmación de pago",
            "confirmacion de pago",
            "notificación de pago",
            "notificacion de pago",
            "pago recibido de",
            "pago recibido",
            "confirmamos que",
            "confirmado",
            "te yapearon",
            "recibiste de",
            "recibiste un yape de",
            "recibiste un plin de",
            "nuevo yape de",
            "nuevo plin de",
            "nuevo pago de",
            "aviso",
            "alerta",
            "hola",
            "listo"
        )

        var changed = true
        while (changed) {
            changed = false
            for (noise in leadingNoise) {
                if (cleaned.startsWith(noise, ignoreCase = true)) {
                    cleaned = cleaned.substring(noise.length).trim()
                    changed = true
                }
            }
        }

        val trailingNoise = listOf(
            "te envió",
            "te envio",
            "te enviaron",
            "te yapeó",
            "te yapeo",
            "te transfirió",
            "te transfirio"
        )
        for (noise in trailingNoise) {
            if (cleaned.endsWith(noise, ignoreCase = true)) {
                cleaned = cleaned.substring(0, cleaned.length - noise.length).trim()
            }
        }

        return cleaned
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }
}
