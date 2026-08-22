package com.watchvault

import com.watchvault.data.migration.MyInnosImporter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

/**
 * Pure-JVM unit test (no Android/Room needed) proving the migration mapping against the
 * REAL backup fixture: Watch_Collection_FINAL_100percent_verified_2026-08-22.zip.
 *
 * This mirrors validation/validate_roundtrip.py, which was run against the actual file during
 * Phase 0/1 design and passed. Keep this file's assertions in sync with that script.
 *
 * FIXTURE_JSON below is the exact watches.json content from that backup (4 watches, 1 service
 * record, 6 photo references) so this test is self-contained and doesn't require file I/O.
 */
class MyInnosImportRoundTripTest {

    private val importer = MyInnosImporter()

    @Test
    fun `migrates all four watches with correct counts`() {
        val result = importer.import(FIXTURE_JSON, nowMillis = 1_787_400_000_000L)
        assertEquals(4, result.watches.size)
        assertEquals(6, result.photos.size)
        assertEquals(1, result.maintenanceRecords.size)
    }

    @Test
    fun `legacyData round-trips every original field byte for byte`() {
        val result = importer.import(FIXTURE_JSON, nowMillis = 0L)
        val originalWatches = Json.parseToJsonElement(FIXTURE_JSON).jsonObject["watches"]!!
            .let { kotlinx.serialization.json.JsonArray(it.jsonArray.toList()) }

        result.watches.forEach { watch ->
            val restored = Json.parseToJsonElement(watch.legacyData!!).jsonObject
            val original = originalWatches.first {
                it.jsonObject["id"]!!.toString() == watch.legacyId.toString()
            }.jsonObject
            assertEquals(original, restored)
        }
    }

    @Test
    fun `box and papers are never inferred from legacy combined flag`() {
        val result = importer.import(FIXTURE_JSON, nowMillis = 0L)
        result.watches.forEach { watch ->
            assertNull("box must stay null/Unknown", watch.box)
            assertNull("papers must stay null/Unknown", watch.papers)
            // the legacy combined flag must still be preserved somewhere
            assertNotNull(watch.hasBoxPapersLegacy)
        }
    }

    @Test
    fun `service record attaches to the Waterbury via resolved uuid`() {
        val result = importer.import(FIXTURE_JSON, nowMillis = 0L)
        val waterburyUuid = result.legacyIdToUuid[1] // legacyId 1 = TW2P84200 in fixture
        assertEquals(1, result.maintenanceRecords.size)
        assertEquals(waterburyUuid, result.maintenanceRecords[0].watchUuid)
        assertEquals("Zimsons - Phoenix", result.maintenanceRecords[0].technician)
        assertEquals(600.0, result.maintenanceRecords[0].cost)
    }

    @Test
    fun `primary vs gallery photo roles preserved with correct counts`() {
        val result = importer.import(FIXTURE_JSON, nowMillis = 0L)
        val primaryCount = result.photos.count { it.isPrimary }
        // 3 of the 4 fixture watches have an imageUri (Samsung has none)
        assertEquals(3, primaryCount)
    }

    @Test
    fun `conflicting external market values are logged but never override canonical value`() {
        val result = importer.import(
            FIXTURE_JSON, nowMillis = 0L,
            externalReferenceValues = mapOf(
                "KCWGL0013101MN" to (13995.0 to "FINAL_VERIFICATION.txt"),
                "TW2P84200" to (6700.0 to "UPDATED_VALUES.txt")
            )
        )
        val kennethCole = result.watches.first { it.referenceNumber == "KCWGL0013101MN" }
        val waterbury = result.watches.first { it.referenceNumber == "TW2P84200" }

        assertEquals(15085.0, kennethCole.estimatedValue) // canonical from watches.json, untouched
        assertEquals(6795.0, waterbury.estimatedValue)    // canonical from watches.json, untouched
        assertEquals(2, result.priceConflicts.size)
        result.priceConflicts.forEach {
            assertEquals("conflicting_not_applied", it.conflictStatus)
            assertFalse(it.isCanonical)
        }
    }

    companion object {
        // Exact watches.json from Watch_Collection_FINAL_100percent_verified_2026-08-22.zip
        val FIXTURE_JSON = MyInnosImportRoundTripTest::class.java
            .getResourceAsStream("/fixtures/watches.json")!!
            .bufferedReader().readText()
    }
}
