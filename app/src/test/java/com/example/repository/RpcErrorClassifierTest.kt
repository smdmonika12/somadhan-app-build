package com.example.repository

import com.example.data.repository.RpcErrorClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** [Step 12.10d] permanent vs transient RPC error শ্রেণিবিন্যাস (pure JVM, Android/Supabase ছাড়া)। */
class RpcErrorClassifierTest {

    @Test
    fun `known server guard codes are permanent`() {
        val samples = listOf(
            "PROBLEM_DISPUTED",
            "NOT_AUTHORIZED",
            "KYC_REQUIRED",
            "INVALID_REFUND_PERCENTAGE",
            "ROLE_INACTIVE",
            "SOLVER_ROLE_INACTIVE",
            "USER_ROLE_INACTIVE",
            "INVALID_ROLE",
            "INSUFFICIENT_BALANCE: 500.00",
            "BELOW_MIN_WITHDRAWAL: 100",
            "USER_NOT_FOUND",
            "ERROR: PROBLEM_DISPUTED (code P0001)"
        )
        for (s in samples) {
            assertTrue("permanent হওয়ার কথা: $s", RpcErrorClassifier.isPermanent(Exception(s)))
        }
    }

    @Test
    fun `network and transient errors are not permanent`() {
        val samples = listOf(
            "Unable to resolve host \"mghvvpndkxnscwryfkib.supabase.co\"",
            "timeout",
            "JWT expired",
            "HTTP 503 Service Unavailable",
            "Connection reset",
            "ALREADY_TERMINAL",
            // [Step 12.11] escrow এখনো cloud-এ পৌঁছায়নি (accept_bid outbox-এ জমা) — sync-lag, স্থায়ী নয়
            "ESCROW_NOT_FOUND"
        )
        for (s in samples) {
            assertFalse("transient হওয়ার কথা: $s", RpcErrorClassifier.isPermanent(Exception(s)))
        }
        assertFalse(RpcErrorClassifier.isPermanent(null))
        assertFalse(RpcErrorClassifier.isPermanent(Exception()))
    }

    @Test
    fun `withdrawal user message maps code to bengali text with generic fallback`() {
        assertTrue(RpcErrorClassifier.withdrawalUserMessage(Exception("KYC_REQUIRED")).contains("KYC"))
        assertTrue(RpcErrorClassifier.withdrawalUserMessage(Exception("INSUFFICIENT_BALANCE: 5")).contains("ব্যালেন্স"))
        assertEquals(
            RpcErrorClassifier.withdrawalUserMessage(Exception("PROBLEM_DISPUTED")),
            RpcErrorClassifier.withdrawalUserMessage(null)
        )
    }

    @Test
    fun `permanent code is found through the cause chain and toString`() {
        val inner = Exception("ERROR: PROBLEM_DISPUTED")
        val outer = RuntimeException("Bad Request", inner)
        assertTrue(RpcErrorClassifier.isPermanent(outer))
        // message null কিন্তু toString-এ কোড থাকলেও ধরা পড়ে
        class CodedException : Exception() {
            override fun toString() = "RestException: KYC_REQUIRED"
        }
        assertTrue(RpcErrorClassifier.isPermanent(CodedException()))
    }

    @Test
    fun `escrow release message is specific for dispute and generic otherwise`() {
        assertTrue(RpcErrorClassifier.escrowReleaseUserMessage(Exception("PROBLEM_DISPUTED")).contains("বিবাদ"))
        assertEquals(
            RpcErrorClassifier.escrowReleaseUserMessage(Exception("ESCROW_NOT_FOUND")),
            RpcErrorClassifier.escrowReleaseUserMessage(null)
        )
    }
}
