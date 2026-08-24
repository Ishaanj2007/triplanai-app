package com.example

import com.example.data.remote.VersionComparator
import org.junit.Assert.*
import org.junit.Test

class SemanticVersionTest {

    @Test
    fun testSemanticVersionComparison() {
        // Examples from requirement:
        // 2.0.9 < 2.1.0
        assertTrue(VersionComparator.compare("2.0.9", "2.1.0") < 0)
        assertTrue(VersionComparator.isOlderThan("2.0.9", "2.1.0"))

        // 2.1.0 = 2.1.0
        assertEquals(0, VersionComparator.compare("2.1.0", "2.1.0"))
        assertFalse(VersionComparator.isOlderThan("2.1.0", "2.1.0"))

        // 2.1.0 < 2.1.1
        assertTrue(VersionComparator.compare("2.1.0", "2.1.1") < 0)
        assertTrue(VersionComparator.isOlderThan("2.1.0", "2.1.1"))

        // 2.1.9 < 2.2.0
        assertTrue(VersionComparator.compare("2.1.9", "2.2.0") < 0)
        assertTrue(VersionComparator.isOlderThan("2.1.9", "2.2.0"))

        // Prefix handling (v2.2.0 vs 2.2.0)
        assertEquals(0, VersionComparator.compare("v2.2.0", "2.2.0"))
        assertEquals(0, VersionComparator.compare("2.2.0", "v2.2.0"))

        // Newer version comparison
        assertTrue(VersionComparator.compare("2.2.0", "2.1.0") > 0)
        assertFalse(VersionComparator.isOlderThan("2.2.0", "2.1.0"))

        // Different number of segments: 2.1 vs 2.1.0 vs 2.1.0.1
        assertEquals(0, VersionComparator.compare("2.1", "2.1.0"))
        assertTrue(VersionComparator.compare("2.1", "2.1.1") < 0)
        assertTrue(VersionComparator.compare("2.1.0.1", "2.1.0") > 0)
    }
}
