package com.example.btmcontabilidad

import com.example.btmcontabilidad.domain.model.LedgerEntry
import com.example.btmcontabilidad.domain.model.LedgerEntryType
import com.example.btmcontabilidad.domain.model.LedgerSourceType
import com.example.btmcontabilidad.ui.viewmodel.findReversibleLedgerEntry
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordListViewModelTest {
    @Test
    fun findsOnlyAnActiveMatchingOriginalEntry() {
        val original = ledgerEntry(id = "ledger-1")

        assertEquals(
            original,
            findReversibleLedgerEntry(
                entries = listOf(original),
                sourceType = LedgerSourceType.COLLECTION,
                sourceId = "collection-1"
            )
        )
        assertNull(
            findReversibleLedgerEntry(
                entries = listOf(original),
                sourceType = LedgerSourceType.ADVANCE,
                sourceId = "collection-1"
            )
        )
    }

    @Test
    fun doesNotOfferReversalWhenAReversalEntryExists() {
        val original = ledgerEntry(id = "ledger-1")
        val reversal = ledgerEntry(
            id = "ledger-2",
            sourceId = "collection-1",
            reversalOfEntryId = "ledger-1"
        )

        assertNull(
            findReversibleLedgerEntry(
                entries = listOf(original, reversal),
                sourceType = LedgerSourceType.COLLECTION,
                sourceId = "collection-1"
            )
        )
    }

    private fun ledgerEntry(
        id: String,
        sourceId: String = "collection-1",
        reversalOfEntryId: String? = null
    ) = LedgerEntry(
        id = id,
        branchId = "branch-1",
        sourceType = LedgerSourceType.COLLECTION,
        sourceId = sourceId,
        entryType = LedgerEntryType.CREDIT,
        signedAmount = BigDecimal("100.00"),
        balanceBefore = BigDecimal("1000.00"),
        balanceAfter = BigDecimal("900.00"),
        businessDate = "2026-09-18",
        description = "Cobro registrado",
        reversalOfEntryId = reversalOfEntryId
    )
}
