package com.netguardpro.mobile

import com.netguardpro.mobile.updater.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for UpdateChecker version comparison logic.
 */
class UpdateCheckerTest {

    // Use reflection to access private isNewerVersion method
    private fun isNewerVersion(remote: String, local: String): Boolean {
        val method = UpdateChecker::class.java.getDeclaredMethod(
            "isNewerVersion",
            String::class.java,
            String::class.java,
        )
        method.isAccessible = true
        return method.invoke(UpdateChecker, remote, local) as Boolean
    }

    @Test
    fun `newer major version detected`() {
        assertTrue(isNewerVersion("2.0.0", "1.0.0"))
    }

    @Test
    fun `newer minor version detected`() {
        assertTrue(isNewerVersion("1.2.0", "1.1.0"))
    }

    @Test
    fun `newer patch version detected`() {
        assertTrue(isNewerVersion("1.1.1", "1.1.0"))
    }

    @Test
    fun `same version not newer`() {
        assertFalse(isNewerVersion("1.1.0", "1.1.0"))
    }

    @Test
    fun `older version not newer`() {
        assertFalse(isNewerVersion("1.0.0", "1.1.0"))
    }

    @Test
    fun `different length versions`() {
        assertTrue(isNewerVersion("1.1.0.1", "1.1.0"))
        assertFalse(isNewerVersion("1.1", "1.1.0"))
    }

    @Test
    fun `handles non-numeric parts gracefully`() {
        // Non-numeric parts should be treated as 0
        assertFalse(isNewerVersion("abc", "1.0.0"))
    }

    @Test
    fun `current version constant exists`() {
        assertTrue(UpdateChecker.CURRENT_VERSION.isNotEmpty())
        assertTrue(UpdateChecker.CURRENT_VERSION.contains("."))
    }

    @Test
    fun `current version format is valid semver`() {
        val parts = UpdateChecker.CURRENT_VERSION.split(".")
        assertTrue(parts.size >= 2)
        parts.forEach { part ->
            assertTrue("Part '$part' should be numeric", part.all { it.isDigit() })
        }
    }
}
