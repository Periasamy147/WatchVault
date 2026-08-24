package com.watchvault.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Owns on-disk storage for watch photos under app-private storage (`filesDir/watch_photos`).
 * Every imported image is downsampled to a bounded max dimension and re-encoded as JPEG before
 * it touches disk or memory at full size, so a 12MP camera/gallery photo never sits around just
 * to back a thumbnail.
 *
 * A new watch's photos land in a per-session staging directory (see [stagingDir]) instead of a
 * real watch-uuid directory, since the Watch row itself doesn't exist in the database until the
 * user taps Save — nothing here is written to the DB (or kept as a real file under the watch's
 * final directory) until that happens, so cancelling Add Watch never leaves an orphaned DB row.
 */
object PhotoStorage {
    private const val MAX_DIMENSION = 1600
    private const val JPEG_QUALITY = 85

    private fun photosRoot(context: Context): File =
        File(context.filesDir, "watch_photos").apply { mkdirs() }

    fun stagingDir(context: Context, sessionId: String): File =
        File(photosRoot(context), "_staging_$sessionId").apply { mkdirs() }

    fun watchDir(context: Context, watchUuid: String): File =
        File(photosRoot(context), watchUuid).apply { mkdirs() }

    /** Downsamples [sourceUri] and writes it as a new JPEG file inside [targetDir]. Returns the
     *  absolute path, or null if the source couldn't be read or decoded. */
    fun importImage(context: Context, sourceUri: Uri, targetDir: File): String? {
        val bitmap = decodeSampledBitmap(context, sourceUri) ?: return null
        val outFile = File(targetDir, "${UUID.randomUUID()}.jpg")
        return try {
            FileOutputStream(outFile).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out) }
            outFile.absolutePath
        } catch (e: Exception) {
            outFile.delete()
            null
        } finally {
            bitmap.recycle()
        }
    }

    /** Moves each named file from [stagingDir] into [finalDir], keyed by original file name so
     *  callers can re-associate moved paths with the [com.watchvault.data.entity.WatchPhoto] rows
     *  they belong to. Files that no longer exist (e.g. already removed by the user) are skipped,
     *  not treated as an error. */
    fun promoteStagingFiles(stagingDir: File, finalDir: File, fileNames: List<String>): Map<String, String> {
        val moved = mutableMapOf<String, String>()
        fileNames.forEach { name ->
            val source = File(stagingDir, name)
            if (!source.exists()) return@forEach
            val dest = File(finalDir, name)
            if (source.renameTo(dest)) moved[name] = dest.absolutePath
        }
        return moved
    }

    fun deleteFile(path: String) {
        runCatching { File(path).delete() }
    }

    fun deleteDir(dir: File) {
        runCatching { dir.deleteRecursively() }
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MAX_DIMENSION || bounds.outHeight / sampleSize > MAX_DIMENSION) {
            sampleSize *= 2
        }

        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        } ?: return null

        // inSampleSize only halves per step, so the decoded bitmap can still exceed MAX_DIMENSION
        // by up to ~2x; do one precise scale-down pass to land at (or under) the actual bound.
        val scale = minOf(MAX_DIMENSION.toFloat() / decoded.width, MAX_DIMENSION.toFloat() / decoded.height, 1f)
        if (scale >= 1f) return decoded
        val scaled = Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }
}
