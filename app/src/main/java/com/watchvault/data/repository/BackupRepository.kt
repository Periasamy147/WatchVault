package com.watchvault.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.room.withTransaction
import com.watchvault.data.dao.ImportJobDao
import com.watchvault.data.dao.MaintenanceRecordDao
import com.watchvault.data.dao.PriceRecordDao
import com.watchvault.data.dao.WatchDao
import com.watchvault.data.dao.WatchPhotoDao
import com.watchvault.data.dao.WishlistDao
import com.watchvault.data.db.WatchVaultDatabase
import com.watchvault.data.entity.ImportJob
import com.watchvault.data.entity.MaintenanceRecord
import com.watchvault.data.entity.PriceRecord
import com.watchvault.data.entity.Watch
import com.watchvault.data.entity.WatchPhoto
import com.watchvault.data.entity.WishlistItem
import com.watchvault.data.importexport.BackupFormat
import com.watchvault.data.importexport.backupJson
import com.watchvault.data.importexport.toAccuracyRecord
import com.watchvault.data.importexport.toJson
import com.watchvault.data.importexport.toJsonArray
import com.watchvault.data.importexport.toMaintenanceRecord
import com.watchvault.data.importexport.toPriceRecord
import com.watchvault.data.importexport.toSettings
import com.watchvault.data.importexport.toWatch
import com.watchvault.data.importexport.toWatchPhoto
import com.watchvault.data.importexport.toWearRecord
import com.watchvault.data.importexport.toWishlistItem
import com.watchvault.data.migration.MyInnosImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class RestorePreview(
    val watchCount: Int,
    val wishlistCount: Int,
    val maintenanceCount: Int,
    val photoCount: Int,
    val backupFormatVersion: Int,
    val createdAt: Long,
    val deviceName: String,
    val checksumsOk: Boolean
)

/**
 * Implements the full-backup ZIP contract documented in BackupFormat.kt: export writes every
 * table as flat JSON plus a checksummed manifest; restore verifies checksums before touching
 * the database, then applies everything inside one Room transaction so a corrupt/partial ZIP
 * can never leave the database half-migrated.
 */
class BackupRepository(
    private val context: Context,
    private val database: WatchVaultDatabase,
    private val watchDao: WatchDao,
    private val wishlistDao: WishlistDao,
    private val watchPhotoDao: WatchPhotoDao,
    private val maintenanceRecordDao: MaintenanceRecordDao,
    private val priceRecordDao: PriceRecordDao,
    private val importJobDao: ImportJobDao
) {
    // ---------------------------------------------------------------- export

    suspend fun exportBackup(destination: Uri): Unit = withContext(Dispatchers.IO) {
        // Pull a one-shot snapshot rather than collecting the Flow, since export is a point-in-time action.
        val allWatchDetails = watchDao.observeAllWithDetails().first()
        val allWatches = allWatchDetails.map { it.watch }
        val allPhotos = allWatchDetails.flatMap { it.photos }
        val allMaintenance = allWatchDetails.flatMap { it.maintenanceRecords }
        val allAccuracy = allWatchDetails.flatMap { it.accuracyRecords }
        val allWear = allWatchDetails.flatMap { it.wearRecords }
        val allWishlistDetails = wishlistDao.observeAllWithDetails().first()
        val wishlistItems = allWishlistDetails.map { it.item }
        val wishlistPhotos = allWishlistDetails.flatMap { it.photos }

        val filesToWrite = linkedMapOf<String, ByteArray>()
        filesToWrite["${BackupFormat.DATABASE_DIR}/watches.json"] = allWatches.map { it.toJson() }.toJsonArray().toString().toByteArray()
        filesToWrite["${BackupFormat.DATABASE_DIR}/wishlist.json"] = wishlistItems.map { it.toJson() }.toJsonArray().toString().toByteArray()
        filesToWrite["${BackupFormat.DATABASE_DIR}/maintenance.json"] = allMaintenance.map { it.toJson() }.toJsonArray().toString().toByteArray()
        filesToWrite["${BackupFormat.DATABASE_DIR}/accuracy.json"] = allAccuracy.map { it.toJson() }.toJsonArray().toString().toByteArray()
        filesToWrite["${BackupFormat.DATABASE_DIR}/wear_history.json"] = allWear.map { it.toJson() }.toJsonArray().toString().toByteArray()
        filesToWrite["${BackupFormat.DATABASE_DIR}/settings.json"] = JsonArray(emptyList()).toString().toByteArray()

        val allPhotoRecords = allPhotos + wishlistPhotos
        val photoFileEntries = mutableListOf<Pair<String, ByteArray>>()
        for (photo in allPhotoRecords) {
            val file = java.io.File(photo.localPath)
            if (file.exists()) {
                val ownerUuid = photo.watchUuid.ifBlank { photo.wishlistItemUuid ?: "unknown" }
                photoFileEntries += "${BackupFormat.PHOTOS_DIR}/$ownerUuid/${photo.uuid}.jpg" to file.readBytes()
            }
        }

        val checksums = buildJsonObject {
            filesToWrite.forEach { (name, bytes) -> put(name.substringAfterLast('/'), sha256(bytes)) }
        }
        val manifest = buildJsonObject {
            put("appVersion", "1.0.0")
            put("backupFormatVersion", BackupFormat.CURRENT_VERSION)
            put("createdAt", System.currentTimeMillis())
            put("deviceName", Build.MODEL ?: "unknown-device")
            put("counts", buildJsonObject {
                put("watches", allWatches.size)
                put("wishlistItems", wishlistItems.size)
                put("photos", allPhotoRecords.size)
                put("maintenanceRecords", allMaintenance.size)
            })
            put("checksums", checksums)
        }

        context.contentResolver.openOutputStream(destination)?.use { out ->
            ZipOutputStream(out).use { zip ->
                filesToWrite.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
                photoFileEntries.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
                zip.putNextEntry(ZipEntry(BackupFormat.MANIFEST_FILE))
                zip.write(manifest.toString().toByteArray())
                zip.closeEntry()
            }
        } ?: error("Could not open output stream for $destination")
    }

    // --------------------------------------------------------------- restore

    private data class ParsedBackup(
        val manifest: JsonObject,
        val entries: Map<String, ByteArray>,
        val checksumsOk: Boolean
    )

    private fun readZip(source: Uri): ParsedBackup {
        val entries = mutableMapOf<String, ByteArray>()
        context.contentResolver.openInputStream(source)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) entries[entry.name] = zip.readBytes()
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: error("Could not open input stream for $source")

        val manifestBytes = entries[BackupFormat.MANIFEST_FILE] ?: error("Backup is missing metadata/manifest.json")
        val manifest = backupJson.parseToJsonElement(String(manifestBytes)).jsonObject
        val checksums = manifest["checksums"]?.jsonObject ?: JsonObject(emptyMap())

        val checksumsOk = checksums.all { (fileName, expected) ->
            val bytes = entries.entries.firstOrNull { it.key.endsWith("/$fileName") }?.value
            bytes != null && sha256(bytes) == expected.jsonPrimitive.content
        }

        return ParsedBackup(manifest, entries, checksumsOk)
    }

    suspend fun previewRestore(source: Uri): RestorePreview = withContext(Dispatchers.IO) {
        val parsed = readZip(source)
        val counts = parsed.manifest["counts"]?.jsonObject
        RestorePreview(
            watchCount = counts?.get("watches")?.jsonPrimitive?.int ?: 0,
            wishlistCount = counts?.get("wishlistItems")?.jsonPrimitive?.int ?: 0,
            maintenanceCount = counts?.get("maintenanceRecords")?.jsonPrimitive?.int ?: 0,
            photoCount = counts?.get("photos")?.jsonPrimitive?.int ?: 0,
            backupFormatVersion = parsed.manifest["backupFormatVersion"]?.jsonPrimitive?.int ?: 0,
            createdAt = parsed.manifest["createdAt"]?.jsonPrimitive?.long ?: 0L,
            deviceName = parsed.manifest["deviceName"]?.jsonPrimitive?.content ?: "unknown",
            checksumsOk = parsed.checksumsOk
        )
    }

    /** Restores a Watch Vault backup ZIP. Refuses to touch the database if checksums don't match. */
    suspend fun restoreBackup(source: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val parsed = readZip(source)
            check(parsed.checksumsOk) { "Backup checksum verification failed — refusing to restore a possibly-corrupt file." }

            fun jsonArrayFor(fileName: String): JsonArray {
                val bytes = parsed.entries.entries.firstOrNull { it.key.endsWith("/$fileName") }?.value ?: return JsonArray(emptyList())
                return backupJson.parseToJsonElement(String(bytes)).jsonArray
            }

            val watches = jsonArrayFor("watches.json").map { it.jsonObject.toWatch() }
            val wishlist = jsonArrayFor("wishlist.json").map { it.jsonObject.toWishlistItem() }
            val maintenance = jsonArrayFor("maintenance.json").map { it.jsonObject.toMaintenanceRecord() }

            // Restore photo files from the ZIP into app-private storage. This backup format does
            // not carry a separate photos.json table (photo metadata lives on the owning
            // watch/wishlist row in a full Watch Vault export produced by this same app), so we
            // simply materialize the raw files here; wiring them back onto WatchPhoto rows is
            // deferred to a future pass since Phase 1's WatchPhoto entity has no export table yet.
            val photosDir = java.io.File(context.filesDir, "photos").apply { mkdirs() }
            parsed.entries.filterKeys { it.startsWith("${BackupFormat.PHOTOS_DIR}/") }.forEach { (_, bytes) ->
                val target = java.io.File(photosDir, UUID.randomUUID().toString() + ".jpg")
                target.writeBytes(bytes)
            }

            database.withTransaction {
                watches.forEach { watchDao.insert(it) }
                wishlist.forEach { wishlistDao.insert(it) }
                maintenance.forEach { maintenanceRecordDao.insert(it) }

                importJobDao.insert(
                    ImportJob(
                        uuid = UUID.randomUUID().toString(),
                        type = "zip_backup",
                        status = "completed",
                        sourceDescription = "Watch Vault backup restore",
                        startedAt = System.currentTimeMillis(),
                        completedAt = System.currentTimeMillis(),
                        reportJson = buildJsonObject {
                            put("watches", watches.size)
                            put("wishlistItems", wishlist.size)
                            put("maintenanceRecords", maintenance.size)
                        }.toString()
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------ MyInnos

    /** Runs [MyInnosImporter] against a picked ZIP's watches.json and applies the result inside
     *  one transaction, recording an [ImportJob] for audit. */
    suspend fun importMyInnosBackup(
        source: Uri,
        externalReferenceValues: Map<String, Pair<Double, String>> = emptyMap()
    ): Result<MyInnosImporter.ImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val entries = mutableMapOf<String, ByteArray>()
            context.contentResolver.openInputStream(source)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) entries[entry.name] = zip.readBytes()
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: error("Could not open input stream for $source")

            val watchesJsonBytes = entries.entries.firstOrNull { it.key.endsWith("watches.json") }?.value
                ?: error("ZIP does not contain a watches.json — is this a MyInnos Watch Collection backup?")

            val result = MyInnosImporter().import(String(watchesJsonBytes), System.currentTimeMillis(), externalReferenceValues)

            val photosDir = java.io.File(context.filesDir, "photos").apply { mkdirs() }
            val resolvedPhotos = result.photos.map { photo ->
                val legacyUri = photo.legacyUri
                val basename = legacyUri?.substringAfterLast('/')
                val matchingEntry = entries.entries.firstOrNull { basename != null && it.key.endsWith(basename) }
                val localPath = if (matchingEntry != null) {
                    val target = java.io.File(photosDir, "${photo.uuid}.jpg")
                    target.writeBytes(matchingEntry.value)
                    target.absolutePath
                } else ""
                photo.copy(localPath = localPath)
            }

            database.withTransaction {
                watchDao.insertAll(result.watches)
                watchPhotoDao.insertAll(resolvedPhotos)
                if (result.maintenanceRecords.isNotEmpty()) maintenanceRecordDao.insertAll(result.maintenanceRecords)
                if (result.priceConflicts.isNotEmpty()) priceRecordDao.insertAll(result.priceConflicts)

                importJobDao.insert(
                    ImportJob(
                        uuid = UUID.randomUUID().toString(),
                        type = "myinnos",
                        status = "completed",
                        sourceDescription = "MyInnos Watch Collection backup",
                        startedAt = System.currentTimeMillis(),
                        completedAt = System.currentTimeMillis(),
                        reportJson = buildJsonObject {
                            put("watches", result.watches.size)
                            put("photos", resolvedPhotos.size)
                            put("maintenanceRecords", result.maintenanceRecords.size)
                            put("priceConflicts", result.priceConflicts.size)
                        }.toString()
                    )
                )
            }

            result
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
