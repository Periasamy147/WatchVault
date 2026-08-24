package com.watchvault.data.photo

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Plain-JVM coverage for the file-management half of [PhotoStorage] (the half that doesn't touch
 * android.graphics/ContentResolver) — promoting staged files into a watch's real photo directory
 * is exactly the step that turns a cancelled Add Watch into either "nothing written" or "an
 * orphaned file", so its rename/skip/lookup behavior is worth pinning down directly.
 */
class PhotoStorageTest {

    private lateinit var root: File

    @Before
    fun setUp() {
        root = File.createTempFile("photostorage", "test").apply {
            delete()
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun promoteStagingFilesMovesOnlyTheRequestedFiles() {
        val staging = File(root, "staging").apply { mkdirs() }
        val final = File(root, "final").apply { mkdirs() }
        File(staging, "a.jpg").writeText("a")
        File(staging, "b.jpg").writeText("b")
        File(staging, "untouched.jpg").writeText("c")

        val moved = PhotoStorage.promoteStagingFiles(staging, final, listOf("a.jpg", "b.jpg"))

        assertEquals(setOf("a.jpg", "b.jpg"), moved.keys)
        assertTrue(File(final, "a.jpg").exists())
        assertTrue(File(final, "b.jpg").exists())
        assertFalse(File(staging, "a.jpg").exists())
        assertTrue(File(staging, "untouched.jpg").exists())
        assertFalse(File(final, "untouched.jpg").exists())
    }

    @Test
    fun promoteStagingFilesSkipsNamesThatDoNotExist() {
        val staging = File(root, "staging").apply { mkdirs() }
        val final = File(root, "final").apply { mkdirs() }
        File(staging, "a.jpg").writeText("a")

        val moved = PhotoStorage.promoteStagingFiles(staging, final, listOf("a.jpg", "missing.jpg"))

        assertEquals(setOf("a.jpg"), moved.keys)
    }

    @Test
    fun deleteFileRemovesIt() {
        val file = File(root, "photo.jpg").apply { writeText("data") }
        PhotoStorage.deleteFile(file.absolutePath)
        assertFalse(file.exists())
    }

    @Test
    fun deleteFileOnMissingPathDoesNotThrow() {
        PhotoStorage.deleteFile(File(root, "does-not-exist.jpg").absolutePath)
    }

    @Test
    fun deleteDirRemovesDirectoryAndContents() {
        val dir = File(root, "staging").apply { mkdirs() }
        File(dir, "a.jpg").writeText("a")
        PhotoStorage.deleteDir(dir)
        assertFalse(dir.exists())
    }
}
