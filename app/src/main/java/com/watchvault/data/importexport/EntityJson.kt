package com.watchvault.data.importexport

import com.watchvault.data.entity.AccuracyRecord
import com.watchvault.data.entity.MaintenanceRecord
import com.watchvault.data.entity.PriceRecord
import com.watchvault.data.entity.Settings
import com.watchvault.data.entity.Watch
import com.watchvault.data.entity.WatchPhoto
import com.watchvault.data.entity.WearRecord
import com.watchvault.data.entity.WishlistItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

/**
 * Explicit, hand-written JSON (de)serialization for every entity written into a backup ZIP.
 * Deliberately not reflection-based and not @Serializable-annotated Room entities, so the
 * Phase 1 entity/DAO files stay exactly as designed — this is a pure adapter layer on top.
 *
 * Every JSON file under database/*.json in the backup ZIP is a flat JsonArray of these
 * objects, one per Room row, per BackupFormat's documented contract.
 */
val backupJson = Json { prettyPrint = false; ignoreUnknownKeys = true }

fun Watch.toJson(): JsonObject = buildJsonObject {
    put("uuid", uuid); putNullable("legacy_id", legacyId)
    putNullable("legacy_data", legacyData); put("source", source)
    put("brand", brand); put("model", model)
    putNullable("reference_number", referenceNumber); putNullable("nickname", nickname)
    putNullable("collection", collection); putNullable("watch_type", watchType)
    putNullable("movement_raw", movementRaw); putNullable("movement_normalized", movementNormalized)
    putNullable("serial_number", serialNumber); putNullable("upc", upc)
    putNullable("purchase_date", purchaseDate); putNullable("purchase_price", purchasePrice)
    putNullable("purchase_currency", purchaseCurrency); put("purchase_currency_assumed", purchaseCurrencyAssumed)
    putNullable("seller", seller); putNullable("purchase_location", purchaseLocation)
    putNullable("invoice_number", invoiceNumber); putNullable("warranty_expiry", warrantyExpiry)
    putNullable("is_first_owner", isFirstOwner)
    putNullable("has_box_papers_legacy", hasBoxPapersLegacy); putNullable("box", box); putNullable("papers", papers)
    putNullable("accessories", accessories)
    putNullable("case_diameter_mm", caseDiameterMm); putNullable("case_thickness_mm", caseThicknessMm)
    putNullable("case_material", caseMaterial); putNullable("case_colour", caseColour); putNullable("case_shape", caseShape)
    putNullable("crystal", crystal); putNullable("dial_colour", dialColour); putNullable("dial_type", dialType)
    putNullable("strap", strap); putNullable("strap_material", strapMaterial); putNullable("strap_colour", strapColour)
    putNullable("lug_width_mm", lugWidthMm); putNullable("water_resistance", waterResistance)
    putNullable("caliber", caliber); putNullable("power_reserve", powerReserve); putNullable("complications", complications)
    putNullable("battery_type", batteryType); putNullable("battery_life", batteryLife)
    putNullable("estimated_value", estimatedValue); putNullable("estimated_value_currency", estimatedValueCurrency)
    putNullable("estimated_value_source", estimatedValueSource); putNullable("condition_raw", conditionRaw)
    put("ownership_status", ownershipStatus); putNullable("notes", notes)
    put("created_at", createdAt); put("updated_at", updatedAt)
}

fun JsonObject.toWatch(): Watch = Watch(
    uuid = str("uuid"), legacyId = intOrNull("legacy_id"), legacyData = strOrNull("legacy_data"),
    source = strOr("source", "manual"), brand = str("brand"), model = str("model"),
    referenceNumber = strOrNull("reference_number"), nickname = strOrNull("nickname"),
    collection = strOrNull("collection"), watchType = strOrNull("watch_type"),
    movementRaw = strOrNull("movement_raw"), movementNormalized = strOrNull("movement_normalized"),
    serialNumber = strOrNull("serial_number"), upc = strOrNull("upc"),
    purchaseDate = longOrNull("purchase_date"), purchasePrice = doubleOrNull("purchase_price"),
    purchaseCurrency = strOrNull("purchase_currency"), purchaseCurrencyAssumed = boolOr("purchase_currency_assumed", false),
    seller = strOrNull("seller"), purchaseLocation = strOrNull("purchase_location"),
    invoiceNumber = strOrNull("invoice_number"), warrantyExpiry = longOrNull("warranty_expiry"),
    isFirstOwner = boolOrNull("is_first_owner"),
    hasBoxPapersLegacy = boolOrNull("has_box_papers_legacy"), box = boolOrNull("box"), papers = boolOrNull("papers"),
    accessories = strOrNull("accessories"),
    caseDiameterMm = doubleOrNull("case_diameter_mm"), caseThicknessMm = doubleOrNull("case_thickness_mm"),
    caseMaterial = strOrNull("case_material"), caseColour = strOrNull("case_colour"), caseShape = strOrNull("case_shape"),
    crystal = strOrNull("crystal"), dialColour = strOrNull("dial_colour"), dialType = strOrNull("dial_type"),
    strap = strOrNull("strap"), strapMaterial = strOrNull("strap_material"), strapColour = strOrNull("strap_colour"),
    lugWidthMm = doubleOrNull("lug_width_mm"), waterResistance = strOrNull("water_resistance"),
    caliber = strOrNull("caliber"), powerReserve = strOrNull("power_reserve"), complications = strOrNull("complications"),
    batteryType = strOrNull("battery_type"), batteryLife = strOrNull("battery_life"),
    estimatedValue = doubleOrNull("estimated_value"), estimatedValueCurrency = strOrNull("estimated_value_currency"),
    estimatedValueSource = strOrNull("estimated_value_source"), conditionRaw = strOrNull("condition_raw"),
    ownershipStatus = strOr("ownership_status", "Active"), notes = strOrNull("notes"),
    createdAt = long("created_at"), updatedAt = long("updated_at")
)

fun WishlistItem.toJson(): JsonObject = buildJsonObject {
    put("uuid", uuid); put("brand", brand); put("model", model)
    putNullable("reference_number", referenceNumber); putNullable("product_url", productUrl)
    putNullable("manufacturer_url", manufacturerUrl); putNullable("store_url", storeUrl)
    putNullable("current_price", currentPrice); putNullable("target_price", targetPrice)
    putNullable("currency", currency); put("priority", priority); putNullable("category", category)
    putNullable("notes", notes); putNullable("specifications_json", specificationsJson)
    putNullable("availability_status", availabilityStatus); put("is_favourite", isFavourite)
    putNullable("converted_to_watch_uuid", convertedToWatchUuid); putNullable("raw_import_data", rawImportData)
    put("date_added", dateAdded); put("updated_at", updatedAt)
}

fun JsonObject.toWishlistItem(): WishlistItem = WishlistItem(
    uuid = str("uuid"), brand = str("brand"), model = str("model"),
    referenceNumber = strOrNull("reference_number"), productUrl = strOrNull("product_url"),
    manufacturerUrl = strOrNull("manufacturer_url"), storeUrl = strOrNull("store_url"),
    currentPrice = doubleOrNull("current_price"), targetPrice = doubleOrNull("target_price"),
    currency = strOrNull("currency"), priority = strOr("priority", "Medium"), category = strOrNull("category"),
    notes = strOrNull("notes"), specificationsJson = strOrNull("specifications_json"),
    availabilityStatus = strOrNull("availability_status"), isFavourite = boolOr("is_favourite", false),
    convertedToWatchUuid = strOrNull("converted_to_watch_uuid"), rawImportData = strOrNull("raw_import_data"),
    dateAdded = long("date_added"), updatedAt = long("updated_at")
)

fun MaintenanceRecord.toJson(): JsonObject = buildJsonObject {
    put("uuid", uuid); put("watch_uuid", watchUuid); putNullable("legacy_service_id", legacyServiceId)
    put("date", date); putNullable("cost", cost); putNullable("technician", technician)
    putNullable("description", description); putNullable("type", type)
    put("is_overhaul", isOverhaul); put("pressure_tested", pressureTested); putNullable("notes", notes)
}

fun JsonObject.toMaintenanceRecord(): MaintenanceRecord = MaintenanceRecord(
    uuid = str("uuid"), watchUuid = str("watch_uuid"), legacyServiceId = intOrNull("legacy_service_id"),
    date = long("date"), cost = doubleOrNull("cost"), technician = strOrNull("technician"),
    description = strOrNull("description"), type = strOrNull("type"),
    isOverhaul = boolOr("is_overhaul", false), pressureTested = boolOr("pressure_tested", false),
    notes = strOrNull("notes")
)

fun WatchPhoto.toJson(): JsonObject = buildJsonObject {
    put("uuid", uuid); put("watch_uuid", watchUuid); putNullable("wishlist_item_uuid", wishlistItemUuid)
    put("local_path", localPath); put("is_primary", isPrimary); put("sort_order", sortOrder)
    putNullable("legacy_uri", legacyUri); put("created_at", createdAt)
}

fun JsonObject.toWatchPhoto(): WatchPhoto = WatchPhoto(
    uuid = str("uuid"), watchUuid = str("watch_uuid"), wishlistItemUuid = strOrNull("wishlist_item_uuid"),
    localPath = str("local_path"), isPrimary = boolOr("is_primary", false), sortOrder = intOrNull("sort_order") ?: 0,
    legacyUri = strOrNull("legacy_uri"), createdAt = long("created_at")
)

fun PriceRecord.toJson(): JsonObject = buildJsonObject {
    put("uuid", uuid); putNullable("watch_uuid", watchUuid); putNullable("wishlist_item_uuid", wishlistItemUuid)
    put("price", price); put("currency", currency); put("source", source); putNullable("source_url", sourceUrl)
    put("recorded_at", recordedAt); put("is_canonical", isCanonical); putNullable("conflict_status", conflictStatus)
}

fun JsonObject.toPriceRecord(): PriceRecord = PriceRecord(
    uuid = str("uuid"), watchUuid = strOrNull("watch_uuid"), wishlistItemUuid = strOrNull("wishlist_item_uuid"),
    price = double("price"), currency = str("currency"), source = str("source"), sourceUrl = strOrNull("source_url"),
    recordedAt = long("recorded_at"), isCanonical = boolOr("is_canonical", true), conflictStatus = strOrNull("conflict_status")
)

fun AccuracyRecord.toJson(): JsonObject = buildJsonObject {
    put("uuid", uuid); put("watch_uuid", watchUuid); put("seconds_per_day", secondsPerDay)
    put("measurement_date", measurementDate); putNullable("reference_source", referenceSource); putNullable("notes", notes)
}

fun JsonObject.toAccuracyRecord(): AccuracyRecord = AccuracyRecord(
    uuid = str("uuid"), watchUuid = str("watch_uuid"), secondsPerDay = double("seconds_per_day"),
    measurementDate = long("measurement_date"), referenceSource = strOrNull("reference_source"), notes = strOrNull("notes")
)

fun WearRecord.toJson(): JsonObject = buildJsonObject {
    put("uuid", uuid); put("watch_uuid", watchUuid); put("worn_date", wornDate); putNullable("notes", notes)
}

fun JsonObject.toWearRecord(): WearRecord = WearRecord(
    uuid = str("uuid"), watchUuid = str("watch_uuid"), wornDate = long("worn_date"), notes = strOrNull("notes")
)

fun Settings.toJson(): JsonObject = buildJsonObject { put("key", key); put("value", value) }
fun JsonObject.toSettings(): Settings = Settings(key = str("key"), value = str("value"))

fun List<JsonObject>.toJsonArray(): JsonArray = JsonArray(this)

// --- small helpers over JsonObject to keep the mappings above readable ---
// kotlinx.serialization.json.JsonPrimitive has built-in nullable overloads for String/Boolean/
// Number that emit JsonNull when the value is null, so put(...) and putNullable(...) can share them.
private fun kotlinx.serialization.json.JsonObjectBuilder.put(key: String, value: String) = put(key, JsonPrimitive(value))
private fun kotlinx.serialization.json.JsonObjectBuilder.put(key: String, value: Boolean) = put(key, JsonPrimitive(value))
private fun kotlinx.serialization.json.JsonObjectBuilder.put(key: String, value: Long) = put(key, JsonPrimitive(value))
private fun kotlinx.serialization.json.JsonObjectBuilder.put(key: String, value: Double) = put(key, JsonPrimitive(value))
private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: String?) = put(key, JsonPrimitive(value))
private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: Boolean?) = put(key, JsonPrimitive(value))
private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: Int?) = put(key, JsonPrimitive(value))
private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: Long?) = put(key, JsonPrimitive(value))
private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: Double?) = put(key, JsonPrimitive(value))

private fun JsonObject.str(key: String): String = this[key]!!.jsonPrimitive.content
private fun JsonObject.strOr(key: String, default: String): String = this[key]?.jsonPrimitive?.contentOrNull ?: default
private fun JsonObject.strOrNull(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.intOrNull(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
private fun JsonObject.long(key: String): Long = this[key]!!.jsonPrimitive.long
private fun JsonObject.longOrNull(key: String): Long? = this[key]?.jsonPrimitive?.longOrNull
private fun JsonObject.double(key: String): Double = this[key]!!.jsonPrimitive.double
private fun JsonObject.doubleOrNull(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull
private fun JsonObject.boolOr(key: String, default: Boolean): Boolean = this[key]?.jsonPrimitive?.booleanOrNull ?: default
private fun JsonObject.boolOrNull(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull
