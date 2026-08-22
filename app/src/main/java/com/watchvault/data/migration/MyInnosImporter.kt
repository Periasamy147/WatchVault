package com.watchvault.data.migration

import com.watchvault.data.entity.MaintenanceRecord
import com.watchvault.data.entity.PriceRecord
import com.watchvault.data.entity.Watch
import com.watchvault.data.entity.WatchPhoto
import kotlinx.serialization.json.*
import java.util.UUID

/**
 * Migrates a MyInnos "Watch Collection" export (watches.json + images/) into Watch Vault
 * entities.
 *
 * Design rules — locked per Phase 0 review, DO NOT change without re-confirming with the user:
 *  1. hasBoxPapers is preserved verbatim into [Watch.hasBoxPapersLegacy]. box/papers are left
 *     null ("Unknown") — never inferred as true/true.
 *  2. marketValue from watches.json is the CANONICAL estimatedValue. Values found in any
 *     accompanying research/reference files (e.g. FINAL_VERIFICATION.txt, UPDATED_VALUES.txt)
 *     must be recorded as PriceRecord(isCanonical=false, conflictStatus="conflicting_not_applied")
 *     and must never silently overwrite the canonical value.
 *  3. legacyData holds the complete original per-watch JSON object, verbatim, so a round-trip
 *     export always fully recovers the source — see MyInnosImporterTest for the proof.
 *  4. purchaseCurrency has no source field in MyInnos; default to "INR" with
 *     purchaseCurrencyAssumed = true, and the import PREVIEW SCREEN MUST SURFACE THIS rather
 *     than presenting it as fact (spec section 21).
 */
class MyInnosImporter {

    data class ImportResult(
        val watches: List<Watch>,
        val photos: List<WatchPhoto>,
        val maintenanceRecords: List<MaintenanceRecord>,
        val priceConflicts: List<PriceRecord>,
        val legacyIdToUuid: Map<Int, String>
    )

    /**
     * @param backupJson raw text of watches.json
     * @param externalReferenceValues optional map of referenceNumber -> (value, sourceLabel) parsed
     *        from accompanying research files. These are NEVER applied as the canonical value —
     *        only logged as conflicting PriceRecords when they differ from the export's marketValue.
     */
    fun import(
        backupJson: String,
        nowMillis: Long,
        externalReferenceValues: Map<String, Pair<Double, String>> = emptyMap()
    ): ImportResult {
        val root = Json.parseToJsonElement(backupJson).jsonObject
        val watchesArray = root["watches"]!!.jsonArray
        val serviceRecordsArray = root["serviceRecords"]?.jsonArray ?: JsonArray(emptyList())

        val legacyIdToUuid = mutableMapOf<Int, String>()
        val watches = mutableListOf<Watch>()
        val photos = mutableListOf<WatchPhoto>()
        val priceConflicts = mutableListOf<PriceRecord>()

        for (element in watchesArray) {
            val obj = element.jsonObject
            val legacyId = obj["id"]!!.jsonPrimitive.int
            val newUuid = UUID.randomUUID().toString()
            legacyIdToUuid[legacyId] = newUuid

            val referenceNumber = obj["referenceNumber"]?.jsonPrimitive?.contentOrNull
            val marketValue = obj["marketValue"]?.jsonPrimitive?.doubleOrNull

            val watch = Watch(
                uuid = newUuid,
                legacyId = legacyId,
                legacyData = obj.toString(), // verbatim original object — nothing dropped
                source = "myinnos_import",
                brand = obj["brand"]!!.jsonPrimitive.content,
                model = obj["model"]!!.jsonPrimitive.content,
                referenceNumber = referenceNumber,
                movementRaw = obj["movementType"]?.jsonPrimitive?.contentOrNull,
                conditionRaw = obj["condition"]?.jsonPrimitive?.contentOrNull,
                ownershipStatus = obj["status"]?.jsonPrimitive?.contentOrNull ?: "Active",
                purchaseDate = obj["purchaseDate"]?.jsonPrimitive?.longOrNull,
                purchasePrice = obj["purchasePrice"]?.jsonPrimitive?.doubleOrNull,
                purchaseCurrency = "INR",           // assumed — see class doc rule 4
                purchaseCurrencyAssumed = true,
                estimatedValue = marketValue,        // canonical — rule 2
                estimatedValueCurrency = "INR",
                estimatedValueSource = "myinnos_export",
                hasBoxPapersLegacy = obj["hasBoxPapers"]?.jsonPrimitive?.booleanOrNull,
                box = null,                          // never inferred — rule 1
                papers = null,                        // never inferred — rule 1
                isFirstOwner = obj["isFirstOwner"]?.jsonPrimitive?.booleanOrNull,
                notes = obj["notes"]?.jsonPrimitive?.contentOrNull,
                createdAt = nowMillis,
                updatedAt = obj["updatedAt"]?.jsonPrimitive?.longOrNull ?: nowMillis
            )
            watches.add(watch)

            // photos: imageUri = primary, galleryUris = gallery, in original order
            obj["imageUri"]?.jsonPrimitive?.contentOrNull?.let { uri ->
                photos.add(
                    WatchPhoto(
                        uuid = UUID.randomUUID().toString(),
                        watchUuid = newUuid,
                        localPath = "", // filled in by the file-copy step using basename(legacyUri)
                        isPrimary = true,
                        sortOrder = 0,
                        legacyUri = uri,
                        createdAt = nowMillis
                    )
                )
            }
            obj["galleryUris"]?.jsonArray?.forEachIndexed { index, el ->
                val uri = el.jsonPrimitive.content
                photos.add(
                    WatchPhoto(
                        uuid = UUID.randomUUID().toString(),
                        watchUuid = newUuid,
                        localPath = "",
                        isPrimary = false,
                        sortOrder = index,
                        legacyUri = uri,
                        createdAt = nowMillis
                    )
                )
            }

            // rule 2: log conflicts, never overwrite
            if (referenceNumber != null && marketValue != null) {
                externalReferenceValues[referenceNumber]?.let { (externalValue, sourceLabel) ->
                    if (externalValue != marketValue) {
                        priceConflicts.add(
                            PriceRecord(
                                uuid = UUID.randomUUID().toString(),
                                watchUuid = newUuid,
                                price = externalValue,
                                currency = "INR",
                                source = "external_research",
                                sourceUrl = sourceLabel,
                                recordedAt = nowMillis,
                                isCanonical = false,
                                conflictStatus = "conflicting_not_applied"
                            )
                        )
                    }
                }
            }
        }

        val maintenanceRecords = serviceRecordsArray.map { el ->
            val obj = el.jsonObject
            val legacyWatchId = obj["watchId"]!!.jsonPrimitive.int
            val watchUuid = legacyIdToUuid[legacyWatchId]
                ?: error("serviceRecord references unknown watchId=$legacyWatchId")
            MaintenanceRecord(
                uuid = UUID.randomUUID().toString(),
                watchUuid = watchUuid,
                legacyServiceId = obj["id"]?.jsonPrimitive?.intOrNull,
                date = obj["serviceDate"]!!.jsonPrimitive.long,
                cost = obj["cost"]?.jsonPrimitive?.doubleOrNull,
                technician = obj["technician"]?.jsonPrimitive?.contentOrNull,
                description = obj["description"]?.jsonPrimitive?.contentOrNull,
                type = "battery",
                isOverhaul = obj["isOverhaul"]?.jsonPrimitive?.booleanOrNull ?: false,
                pressureTested = obj["pressureTested"]?.jsonPrimitive?.booleanOrNull ?: false
            )
        }

        return ImportResult(watches, photos, maintenanceRecords, priceConflicts, legacyIdToUuid)
    }
}
