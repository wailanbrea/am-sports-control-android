package com.example.btmcontabilidad.data.session

import java.security.GeneralSecurityException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SessionStoreTest {
    @Test
    fun `invalid encrypted state is reset and recreated`() {
        var attempts = 0
        var resets = 0

        val result = recoverEncryptedPreferences(
            create = {
                attempts++
                if (attempts == 1) throw GeneralSecurityException("invalid keyset")
                "recovered"
            },
            reset = { resets++ }
        )

        assertEquals("recovered", result)
        assertEquals(2, attempts)
        assertEquals(1, resets)
    }

    @Test
    fun `unexpected failures are not treated as encrypted state corruption`() {
        var resets = 0

        assertThrows(IllegalStateException::class.java) {
            recoverEncryptedPreferences(
                create = { throw IllegalStateException("unexpected") },
                reset = { resets++ }
            )
        }

        assertEquals(0, resets)
    }
}
