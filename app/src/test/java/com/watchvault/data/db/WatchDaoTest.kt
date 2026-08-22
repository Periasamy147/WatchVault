package com.watchvault.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.watchvault.data.entity.Watch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * In-memory Room DAO test, run via Robolectric so it executes as a plain JVM unit test
 * without a connected device/emulator. Requires the `robolectric` and `androidx.test:core`
 * test dependencies declared in app/build.gradle.kts.
 */
@RunWith(AndroidJUnit4::class)
class WatchDaoTest {

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

    private fun sampleWatch(uuid: String, legacyId: Int? = null, brand: String = "Timex", model: String = "Waterbury") = Watch(
        uuid = uuid,
        legacyId = legacyId,
        brand = brand,
        model = model,
        referenceNumber = "TW2P84200",
        createdAt = 0L,
        updatedAt = 0L
    )

    @Test
    fun insertAndRetrieveByUuid() = runBlocking {
        val watch = sampleWatch("uuid-1")
        db.watchDao().insert(watch)

        val loaded = db.watchDao().getByUuid("uuid-1")
        assertEquals(watch.brand, loaded?.brand)
        assertEquals(watch.model, loaded?.model)
    }

    @Test
    fun findByLegacyIdReturnsNullWhenAbsent() = runBlocking {
        assertNull(db.watchDao().findByLegacyId(999))
    }

    @Test
    fun findByReferenceNumberMatchesDuplicateDetectionRule() = runBlocking {
        db.watchDao().insert(sampleWatch("uuid-1", legacyId = 1))
        val matches = db.watchDao().findByReferenceNumber("TW2P84200")
        assertEquals(1, matches.size)
    }

    @Test
    fun findByBrandModelUsedAsFallbackDuplicateCheck() = runBlocking {
        db.watchDao().insert(sampleWatch("uuid-1", brand = "Kenneth Cole", model = "Green Dial Automatic Watch"))
        val matches = db.watchDao().findByBrandModel("Kenneth Cole", "Green Dial Automatic Watch")
        assertEquals(1, matches.size)
    }
}
