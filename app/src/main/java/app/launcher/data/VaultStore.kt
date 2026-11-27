package app.launcher.data

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * #34 PIN vault cryptography. The PIN is never stored — only a
 * random salt (16 bytes, hex) and SHA-256(salt bytes + PIN UTF-8
 * bytes, hex). Verification re-hashes the entered PIN with the
 * stored salt and compares hashes in constant time.
 *
 * The salt + hash live in DataStore (LauncherStore); this object is
 * pure so the scheme can be unit-tested without Android storage.
 */
object PinCrypto {

    fun generateSaltHex(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    fun hashPin(pin: String, saltHex: String): String {
        val salt = hexToBytes(saltHex)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun verify(pin: String, saltHex: String, hashHex: String): Boolean {
        if (pin.isEmpty() || saltHex.isEmpty() || hashHex.isEmpty()) return false
        val candidate = runCatching { hashPin(pin, saltHex) }.getOrNull() ?: return false
        return constantTimeEquals(candidate, hashHex)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.lowercase()
        val out = ByteArray(clean.length / 2)
        for (i in out.indices) {
            out[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return out
    }
}
