package com.watchvault.data.importexport

/**
 * Full-backup ZIP layout (spec section 5):
 *
 * watch-vault-backup-YYYY-MM-DD.zip
 * ├── database/
 * │   ├── watches.json
 * │   ├── wishlist.json
 * │   ├── maintenance.json
 * │   ├── accuracy.json
 * │   ├── wear_history.json
 * │   └── settings.json
 * ├── photos/
 * │   └── <watch_uuid>/<photo_uuid>.jpg
 * ├── documents/
 * │   └── <watch_uuid>/<document_uuid>.<ext>
 * └── metadata/
 *     └── manifest.json
 *
 * manifest.json contract (checked on both export and restore):
 *   {
 *     "appVersion": "1.0.0",
 *     "backupFormatVersion": 1,
 *     "createdAt": <epoch millis>,
 *     "deviceName": "<string>",
 *     "counts": { "watches": N, "wishlistItems": N, "photos": N, "maintenanceRecords": N, ... },
 *     "checksums": { "watches.json": "<sha256>", ... }   // per-file integrity check
 *   }
 *
 * Restore MUST verify checksums before importing anything, and MUST run inside a single Room
 * transaction so a partial/corrupt ZIP can never leave the database half-migrated.
 *
 * Every entity JSON file is a flat array of that entity's Room columns (i.e. the direct,
 * lossless serialization of the table — NOT a MyInnos-shaped object). MyInnos import produces
 * Watch Vault entities via MyInnosImporter first; from that point on, export/restore only ever
 * deals with Watch Vault's own shape, so backup/restore round-trips are pure and format-stable
 * regardless of where the data originally came from.
 */
object BackupFormat {
    const val CURRENT_VERSION = 1
    const val DATABASE_DIR = "database"
    const val PHOTOS_DIR = "photos"
    const val DOCUMENTS_DIR = "documents"
    const val METADATA_DIR = "metadata"
    const val MANIFEST_FILE = "metadata/manifest.json"
}

/**
 * CSV export/import column contract for the "My Collection" sheet. One row per watch.
 * Column order is fixed so round-trip CSV -> DB -> CSV is diff-stable.
 */
object WatchCsvSchema {
    val COLUMNS = listOf(
        "uuid", "legacy_id", "source", "brand", "model", "reference_number", "nickname",
        "collection", "watch_type", "movement_raw", "movement_normalized", "serial_number", "upc",
        "purchase_date", "purchase_price", "purchase_currency", "purchase_currency_assumed",
        "seller", "purchase_location", "invoice_number", "warranty_expiry", "is_first_owner",
        "has_box_papers_legacy", "box", "papers", "accessories",
        "case_diameter_mm", "case_thickness_mm", "case_material", "case_colour", "case_shape",
        "crystal", "dial_colour", "dial_type", "strap", "strap_material", "strap_colour",
        "lug_width_mm", "water_resistance", "caliber", "power_reserve", "complications",
        "battery_type", "battery_life",
        "estimated_value", "estimated_value_currency", "estimated_value_source",
        "condition_raw", "ownership_status", "notes", "created_at", "updated_at"
    )
}
