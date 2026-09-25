package com.example.data.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt

/**
 * Centralized helper for hashing and verifying user passwords with bcrypt.
 *
 * IMPORTANT: All new/updated passwords must go through [hash] before being
 * persisted to Room (Supabase Auth handles its own credentials separately).
 * Never store or sync a raw plaintext password again.
 *
 * Legacy accounts created before this change still have plaintext passwords
 * saved locally (and, historically, in the now-removed Firestore backend).
 * [isHashed] lets calling code detect that
 * case so it can fall back to a plaintext comparison once, then transparently
 * migrate the account to a bcrypt hash (see SomadhanViewModel login flow).
 *
 * [hash] and [verify] are suspend functions that hop to [Dispatchers.Default]
 * internally — bcrypt is deliberately CPU-slow (that's what makes it secure),
 * so it must never run on the Main dispatcher (viewModelScope's default) or
 * it will jank the UI.
 */
object PasswordHasher {

    // Work factor: higher = slower/more secure. 12 is a reasonable default
    // for a mobile client in 2026.
    private const val BCRYPT_ROUNDS = 12

    /** Hashes a plaintext password for storage. */
    suspend fun hash(plainPassword: String): String = withContext(Dispatchers.Default) {
        BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS))
    }

    /**
     * Verifies a plaintext password against a stored value that is assumed
     * to already be a bcrypt hash. Only call this when [isHashed] is true.
     */
    suspend fun verify(plainPassword: String, storedHash: String): Boolean = withContext(Dispatchers.Default) {
        try {
            BCrypt.checkpw(plainPassword, storedHash)
        } catch (e: IllegalArgumentException) {
            // storedHash wasn't actually a valid bcrypt hash.
            false
        }
    }

    /**
     * Bcrypt hashes always start with one of these version prefixes
     * (e.g. "$2a$12$..."). Plaintext legacy passwords won't match this,
     * which is what lets us tell the two cases apart during migration.
     */
    fun isHashed(storedPassword: String): Boolean {
        return storedPassword.startsWith("\$2a\$") ||
            storedPassword.startsWith("\$2b\$") ||
            storedPassword.startsWith("\$2y\$")
    }
}
