package com.watchvault.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.watchvault.data.entity.Watch
import com.watchvault.data.entity.WatchPhoto
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the photo-management operations added for Add/Edit Watch's image system: multi-insert,
 * reordering/primary updates via [androidx.room.Update], per-watch cascade delete, and that
 * deleting a single photo doesn't touch its siblings.
 */
@RunWith(AndroidJUnit4::class)
class WatchPhotoDaoTest {

    private lateinit var db: WatchVaultDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WatchVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sampleWatch(uuid: String) = Watch(
        uuid = uuid, brand = "Seiko", model = "SKX007", createdAt = 0L, updatedAt = 0L
    )

    private fun samplePhoto(uuid: String, watchUuid: String, sortOrder: Int, isPrimary: Boolean = false) = WatchPhoto(
        uuid = uuid, watchUuid = watchUuid, localPath = "/data/watch_photos/$watchUuid/$uuid.jpg",
        isPrimary = isPrimary, sortOrder = sortOrder, createdAt = 0L
    )

    @Test
    fun insertAllAndReadBackInSortOrder() = runBlocking {
        db.watchDao().insert(sampleWatch("w1"))
        db.watchPhotoDao().insertAll(
            listOf(
                samplePhoto("p2", "w1", sortOrder = 1),
                samplePhoto("p1", "w1", sortOrder = 0, isPrimary = true)
            )
        )

        val photos = db.watchPhotoDao().forWatch("w1")
        assertEquals(listOf("p1", "p2"), photos.map { it.uuid })
        assertTrue(photos.first().isPrimary)
    }

    @Test
    fun updateAllPersistsReorderAndPrimaryChange() = runBlocking {
        db.watchDao().insert(sampleWatch("w1"))
        db.watchPhotoDao().insertAll(
            listOf(
                samplePhoto("p1", "w1", sortOrder = 0, isPrimary = true),
                samplePhoto("p2", "w1", sortOrder = 1)
            )
        )

        val reordered = db.watchPhotoDao().forWatch("w1").map { it.copy(isPrimary = it.uuid == "p2") }
            .sortedByDescending { it.uuid }
            .mapIndexed { index, photo -> photo.copy(sortOrder = index) }
        db.watchPhotoDao().updateAll(reordered)

        val result = db.watchPhotoDao().forWatch("w1")
        assertEquals("p2", result.first().uuid)
        assertTrue(result.first { it.uuid == "p2" }.isPrimary)
        assertTrue(!result.first { it.uuid == "p1" }.isPrimary)
    }

    @Test
    fun deletingOnePhotoLeavesSiblingsIntact() = runBlocking {
        db.watchDao().insert(sampleWatch("w1"))
        val p1 = samplePhoto("p1", "w1", sortOrder = 0)
        val p2 = samplePhoto("p2", "w1", sortOrder = 1)
        db.watchPhotoDao().insertAll(listOf(p1, p2))

        db.watchPhotoDao().delete(p1)

        val remaining = db.watchPhotoDao().forWatch("w1")
        assertEquals(listOf("p2"), remaining.map { it.uuid })
    }

    @Test
    fun deletingWatchCascadesToItsPhotos() = runBlocking {
        val watch = sampleWatch("w1")
        db.watchDao().insert(watch)
        db.watchPhotoDao().insertAll(listOf(samplePhoto("p1", "w1", sortOrder = 0)))

        db.watchDao().delete(watch)

        assertTrue(db.watchPhotoDao().forWatch("w1").isEmpty())
    }
}
