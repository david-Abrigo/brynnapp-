package com.example

import com.example.data.model.YapeTransaction
import com.example.util.ReportExportUtil
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportExportUtilTest {

    @Test
    fun testBuildTextReport_containsNoNotiyapeOrBrynnOrId() {
        val tx1 = YapeTransaction(
            id = 1L,
            remoteId = "rem_abc12345",
            senderName = "Carlos Mendoza",
            amount = 45.50,
            timestamp = 1716000000000L,
            note = "2x Hamburguesa Clásica",
            claimedByName = "Ana Cajera",
            claimedBy = "worker_123",
            isStoreTransaction = true
        )
        val tx2 = YapeTransaction(
            id = 2L,
            remoteId = "rem_xyz98765",
            senderName = "Desconocido X",
            amount = 10.00,
            timestamp = 1716003600000L,
            isStoreTransaction = false,
            exclusionReason = "Pago no reconocido"
        )

        val reportText = ReportExportUtil.buildTextReport(
            storeName = "Burger King Express",
            dateString = "20/09/2026",
            transactions = listOf(tx1, tx2),
            totalAmount = 45.50,
            workerName = "Ana Cajera",
            branchName = "Sucursal Central"
        )

        // Verify branding removal
        assertFalse("Report should not contain Notiyape", reportText.contains("Notiyape", ignoreCase = true))
        assertFalse("Report should not contain Brynn", reportText.contains("Brynn", ignoreCase = true))

        // Verify ID removal
        assertFalse("Report should not contain [ID:", reportText.contains("[ID:", ignoreCase = true))
        assertFalse("Report should not contain [#", reportText.contains("[#", ignoreCase = true))
        assertFalse("Report should not contain remoteId", reportText.contains("rem_abc12345"))
        assertFalse("Report should not contain remoteId", reportText.contains("rem_xyz98765"))

        // Verify content
        assertTrue("Report should contain REPORTE DE VENTAS", reportText.contains("REPORTE DE VENTAS"))
        assertTrue("Report should contain Store Name", reportText.contains("Burger King Express"))
        assertTrue("Report should contain Branch Name", reportText.contains("Sucursal Central"))
        assertTrue("Report should contain Worker Name", reportText.contains("Ana Cajera"))
        assertTrue("Report should contain product note", reportText.contains("2x Hamburguesa Clásica"))
        assertTrue("Report should contain amount", reportText.contains("45.50"))
    }
}
