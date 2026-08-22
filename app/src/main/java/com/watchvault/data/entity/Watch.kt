package com.watchvault.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Core "My Collection" entity.
 *
 * Field provenance notes (see docs/PHASE1_SCHEMA_DESIGN.md section 1 for full rationale):
 *  - [legacyId] / [legacyData]: preserved from MyInnos "Watch Collection" export. legacyData
 *    holds the ENTIRE original JSON object for this watch, verbatim, so nothing sourced from
 *    the old app is ever unrecoverable even if a field has no structured home yet.
 *  - [box] / [papers]: intentionally nullable and NOT inferred from the legacy [hasBoxPapers]
 *    boolean. The old app only recorded one combined flag; splitting it into two facts would be
 *    fabricating data. UI must display "Unknown" until the user confirms each one explicitly.
 *  - [purchaseCurrency] / [estimatedValueCurrency]: MyInnos export has no currency field at all.
 *    Migration defaults to INR but this is an ASSUMPTION, not a migrated fact — import preview
 *    must label it "Currency: INR (assumed)".
 *  - [estimatedValueSource]: distinguishes the canonical imported market value from any
 *    conflicting externally-researched value (see PriceConflict / PriceRecord).
 */
@Entity(tableName = "watches")
data class Watch(
    @PrimaryKey
    val uuid: String,

    // --- Migration provenance ---
    @ColumnInfo(name = "legacy_id")
    val legacyId: Int? = null,

    @ColumnInfo(name = "legacy_data")
    val legacyData: String? = null, // raw JSON of the original source object, if imported

    @ColumnInfo(name = "source")
    val source: String = "manual", // "manual" | "myinnos_import" | "url_import" | "csv_import" | "json_import"

    // --- Identity ---
    val brand: String,
    val model: String,
    @ColumnInfo(name = "reference_number")
    val referenceNumber: String? = null,
    val nickname: String? = null,
    val collection: String? = null,
    @ColumnInfo(name = "watch_type")
    val watchType: String? = null,
    @ColumnInfo(name = "movement_raw")
    val movementRaw: String? = null,       // verbatim as imported/entered
    @ColumnInfo(name = "movement_normalized")
    val movementNormalized: String? = null, // mapped to app enum, nullable until confirmed
    @ColumnInfo(name = "serial_number")
    val serialNumber: String? = null,
    val upc: String? = null,

    // --- Purchase ---
    @ColumnInfo(name = "purchase_date")
    val purchaseDate: Long? = null,
    @ColumnInfo(name = "purchase_price")
    val purchasePrice: Double? = null,
    @ColumnInfo(name = "purchase_currency")
    val purchaseCurrency: String? = null,
    @ColumnInfo(name = "purchase_currency_assumed")
    val purchaseCurrencyAssumed: Boolean = false,
    val seller: String? = null,
    @ColumnInfo(name = "purchase_location")
    val purchaseLocation: String? = null,
    @ColumnInfo(name = "invoice_number")
    val invoiceNumber: String? = null,
    @ColumnInfo(name = "warranty_expiry")
    val warrantyExpiry: Long? = null,
    @ColumnInfo(name = "is_first_owner")
    val isFirstOwner: Boolean? = null,

    // --- Box / papers (see class doc — never inferred) ---
    @ColumnInfo(name = "has_box_papers_legacy")
    val hasBoxPapersLegacy: Boolean? = null, // preserved verbatim from old combined flag
    val box: Boolean? = null,
    val papers: Boolean? = null,
    val accessories: String? = null,

    // --- Specifications (free text where source has no enum) ---
    @ColumnInfo(name = "case_diameter_mm")
    val caseDiameterMm: Double? = null,
    @ColumnInfo(name = "case_thickness_mm")
    val caseThicknessMm: Double? = null,
    @ColumnInfo(name = "case_material")
    val caseMaterial: String? = null,
    @ColumnInfo(name = "case_colour")
    val caseColour: String? = null,
    @ColumnInfo(name = "case_shape")
    val caseShape: String? = null,
    val crystal: String? = null,
    @ColumnInfo(name = "dial_colour")
    val dialColour: String? = null,
    @ColumnInfo(name = "dial_type")
    val dialType: String? = null,
    val strap: String? = null,
    @ColumnInfo(name = "strap_material")
    val strapMaterial: String? = null,
    @ColumnInfo(name = "strap_colour")
    val strapColour: String? = null,
    @ColumnInfo(name = "lug_width_mm")
    val lugWidthMm: Double? = null,
    @ColumnInfo(name = "water_resistance")
    val waterResistance: String? = null,
    val caliber: String? = null,
    @ColumnInfo(name = "power_reserve")
    val powerReserve: String? = null,
    val complications: String? = null,
    @ColumnInfo(name = "battery_type")
    val batteryType: String? = null,
    @ColumnInfo(name = "battery_life")
    val batteryLife: String? = null,

    // --- Collection / valuation ---
    @ColumnInfo(name = "estimated_value")
    val estimatedValue: Double? = null,
    @ColumnInfo(name = "estimated_value_currency")
    val estimatedValueCurrency: String? = null,
    @ColumnInfo(name = "estimated_value_source")
    val estimatedValueSource: String? = null, // "myinnos_export" | "manual" | "url_fetch"
    @ColumnInfo(name = "condition_raw")
    val conditionRaw: String? = null,
    @ColumnInfo(name = "ownership_status")
    val ownershipStatus: String = "Active",

    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
