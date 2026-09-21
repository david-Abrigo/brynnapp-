package com.example

import com.example.service.parser.ParseResult
import com.example.service.parser.YapeNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YapeNotificationParserTest {

    @Test
    fun testParseFormat1_TeYapearon() {
        val result = YapeNotificationParser.parse(
            title = "Yape",
            text = "¡Te yapearon! Andrea Palacios te envió S/ 45.00"
        )
        assertTrue(result is ParseResult.Success)
        val s = result as ParseResult.Success
        assertEquals("Andrea Palacios", s.senderName)
        assertEquals(45.00, s.amount, 0.001)
    }

    @Test
    fun testParseFormat2_TeYapeoDirect() {
        val result = YapeNotificationParser.parse(
            title = "Yape",
            text = "Juan Carlos te yapeó S/ 15.50"
        )
        assertTrue(result is ParseResult.Success)
        val s = result as ParseResult.Success
        assertEquals("Juan Carlos", s.senderName)
        assertEquals(15.50, s.amount, 0.001)
    }

    @Test
    fun testParseFormat3_RecibisteUnYapeDe() {
        val result = YapeNotificationParser.parse(
            title = "Yape",
            text = "Recibiste un Yape de Maria Gomez por S/ 100.00"
        )
        assertTrue(result is ParseResult.Success)
        val s = result as ParseResult.Success
        assertEquals("Maria Gomez", s.senderName)
        assertEquals(100.00, s.amount, 0.001)
    }

    @Test
    fun testParseFormat4_ConfirmamosQue() {
        val result = YapeNotificationParser.parse(
            title = "Yape",
            text = "Confirmamos que Rodrigo Flores te envió S/ 250.00"
        )
        assertTrue(result is ParseResult.Success)
        val s = result as ParseResult.Success
        assertEquals("Rodrigo Flores", s.senderName)
        assertEquals(250.00, s.amount, 0.001)
    }

    @Test
    fun testRejectPromo() {
        val result = YapeNotificationParser.parse(
            title = "Yape",
            text = "¡Gana con Yape! Pide tu préstamo y llévate hasta S/ 500 hoy"
        )
        assertTrue(result is ParseResult.Ignored)
    }

    @Test
    fun testRejectOutboundPayment() {
        val result = YapeNotificationParser.parse(
            title = "Yape",
            text = "Enviaste S/ 40.00 a Cesar Chavez con éxito"
        )
        assertTrue(result is ParseResult.Ignored)
    }

    @Test
    fun testRejectFailedPayment() {
        val result = YapeNotificationParser.parse(
            title = "Yape",
            text = "No se pudo procesar tu pago de S/ 15.00"
        )
        assertTrue(result is ParseResult.Ignored)
    }

    @Test
    fun testParsePlinWithAsteriskAndSecurityCode() {
        val result = YapeNotificationParser.parse(
            title = "Confirmación de Pago",
            text = "Olga Ros* te envió un pago por S/ 1. El cód. de seguridad es: 371"
        )
        assertTrue(result is ParseResult.Success)
        val s = result as ParseResult.Success
        assertEquals("Olga Ros*", s.senderName)
        assertEquals(1.00, s.amount, 0.001)
    }

    @Test
    fun testParseYapeWithTitleConfirmacionDePago() {
        val result = YapeNotificationParser.parse(
            title = "Confirmación de Pago",
            text = "Yape! DAVID ABRIGO te envió un pago por S/ 1"
        )
        assertTrue(result is ParseResult.Success)
        val s = result as ParseResult.Success
        assertEquals("David Abrigo", s.senderName)
        assertEquals(1.00, s.amount, 0.001)
    }
}
