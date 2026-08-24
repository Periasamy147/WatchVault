package com.watchvault.ui.screens.addedit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchvault.data.entity.Watch
import com.watchvault.data.entity.WatchPhoto
import com.watchvault.data.photo.PhotoStorage
import com.watchvault.data.repository.WatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * For a new watch (no [existingUuid]), photo files are imported into a private per-session
 * staging directory and only ever written to the database once the user actually taps Save —
 * this is what keeps a cancelled Add flow from creating an orphaned Watch row or leaving photo
 * files attached to nothing. For an existing watch, add/remove/reorder take effect immediately,
 * the same as every other edit already does elsewhere on this screen.
 */
class AddEditWatchViewModel(
    private val repository: WatchRepository,
    private val existingUuid: String?
) : ViewModel() {

    private val isNew = existingUuid == null
    private val stagingSessionId = UUID.randomUUID().toString()
    private var photosLoaded = false

    private val _photos = MutableStateFlow<List<WatchPhoto>>(emptyList())
    val photos: StateFlow<List<WatchPhoto>> = _photos

    suspend fun load(): Watch? {
        val existing = existingUuid?.let { repository.getByUuid(it) } ?: return null
        if (!photosLoaded) {
            _photos.value = repository.photosForWatch(existing.uuid).sortedBy { it.sortOrder }
            photosLoaded = true
        }
        return existing
    }

    fun addPhotos(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val targetDir = if (isNew) {
                PhotoStorage.stagingDir(context, stagingSessionId)
            } else {
                PhotoStorage.watchDir(context, existingUuid!!)
            }
            val startOrder = _photos.value.size
            val hadNone = _photos.value.isEmpty()
            val imported = uris.mapIndexedNotNull { index, uri ->
                val path = PhotoStorage.importImage(context, uri, targetDir) ?: return@mapIndexedNotNull null
                WatchPhoto(
                    uuid = UUID.randomUUID().toString(),
                    watchUuid = existingUuid ?: "",
                    localPath = path,
                    isPrimary = hadNone && index == 0,
                    sortOrder = startOrder + index,
                    createdAt = System.currentTimeMillis()
                )
            }
            if (imported.isEmpty()) return@launch
            _photos.value = _photos.value + imported
            if (!isNew) repository.addPhotos(imported)
        }
    }

    fun setPrimary(photoUuid: String) {
        val updated = _photos.value.map { it.copy(isPrimary = it.uuid == photoUuid) }
        _photos.value = updated
        if (!isNew) viewModelScope.launch { repository.updatePhotos(updated) }
    }

    fun removePhoto(photo: WatchPhoto) {
        _photos.value = _photos.value.filterNot { it.uuid == photo.uuid }
        PhotoStorage.deleteFile(photo.localPath)
        if (!isNew) viewModelScope.launch { repository.deletePhoto(photo) }
    }

    fun movePhoto(photo: WatchPhoto, delta: Int) {
        val list = _photos.value.toMutableList()
        val from = list.indexOfFirst { it.uuid == photo.uuid }
        if (from == -1) return
        val to = (from + delta).coerceIn(0, list.lastIndex)
        if (from == to) return
        val item = list.removeAt(from)
        list.add(to, item)
        val reordered = list.mapIndexed { index, p -> p.copy(sortOrder = index) }
        _photos.value = reordered
        if (!isNew) viewModelScope.launch { repository.updatePhotos(reordered) }
    }

    /** Called when the user backs out of adding a new watch without saving, so staged photo
     *  files don't linger in app storage forever. No-op for an existing watch, whose photos are
     *  already committed. */
    fun discardIfUnsaved(context: Context) {
        if (isNew) PhotoStorage.deleteDir(PhotoStorage.stagingDir(context, stagingSessionId))
    }

    fun save(watch: Watch, context: Context, onSaved: (String) -> Unit) {
        viewModelScope.launch {
            val finalUuid = if (isNew) UUID.randomUUID().toString() else watch.uuid
            repository.upsert(watch.copy(uuid = finalUuid), isNew)

            if (isNew && _photos.value.isNotEmpty()) {
                val staged = PhotoStorage.stagingDir(context, stagingSessionId)
                val finalDir = PhotoStorage.watchDir(context, finalUuid)
                val fileNames = _photos.value.map { File(it.localPath).name }
                val movedByName = PhotoStorage.promoteStagingFiles(staged, finalDir, fileNames)
                val finalPhotos = _photos.value.mapNotNull { photo ->
                    val newPath = movedByName[File(photo.localPath).name] ?: return@mapNotNull null
                    photo.copy(watchUuid = finalUuid, localPath = newPath)
                }
                repository.addPhotos(finalPhotos)
                PhotoStorage.deleteDir(staged)
            }
            onSaved(finalUuid)
        }
    }
}
