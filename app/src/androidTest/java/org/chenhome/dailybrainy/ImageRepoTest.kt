package org.chenhome.dailybrainy

import android.content.Context
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.chenhome.dailybrainy.repo.image.LocalImageRepo
import org.chenhome.dailybrainy.repo.image.RemoteImage
import org.chenhome.dailybrainy.repo.image.RemoteImageFolder
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import timber.log.Timber
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@RunWith(AndroidJUnit4::class)
class ImageRepoTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var appContext: Context
    lateinit var localRepo: LocalImageRepo
    lateinit var remote: RemoteImage

    // fixture used in testing
    lateinit var tempFileUri: Uri

    @Before
    fun before() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        localRepo =
            LocalImageRepo(appContext)
        remote =
            RemoteImage(appContext)

        // create a random file
        val tempFile = File.createTempFile("temp", "tmp", null)
        tempFileUri = Uri.fromFile(tempFile)
        Timber.d("Creating file ${tempFileUri.path}")
        val inStream = appContext.resources.openRawResource(R.raw.brainydb_challenges)
        val bytes = Files.copy(
            inStream,
            File(tempFileUri.path).toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
        Assert.assertTrue("Expected temp file to be written", bytes > 0)
        inStream.close()

    }

    @After
    fun after() {
        // delete file
        val f = File(tempFileUri.path)
        Timber.d("Deleting file ${f.absolutePath}, size: ${f.length()}")
        if (!Files.deleteIfExists(f.toPath())) {
            Timber.w("No file to delete $tempFileUri")
        }
    }

    @Test
    fun testSaveLocal() {
        runBlocking {
            val folders =
                arrayListOf(localRepo.LOCALFOLDER_DOWNLOADS, localRepo.LOCALFOLDER_DOWNLOADS)
            for (f in folders) {
                val dstUri = localRepo.saveLocal(tempFileUri, f)
                assertNotNull(dstUri)

                // check it exists
                assertTrue(localRepo.isExist(dstUri!!))
                assertTrue(localRepo.deleteLocal(dstUri))
            }
        }
    }

    @Test
    fun testUploadRemote() {
        runBlocking {
            val r = remote.upload(RemoteImageFolder.CHALLENGES, tempFileUri)
            assertNotNull(r)
            Timber.d("Storage ref from upload ${r!!.path}")

            val r2 = remote.getValidStorageRef(r.path)
            assertNotNull(r2)
            assertEquals(r, r2)

            // delete it
            assertTrue(remote.deleteRemote(r))
        }
    }

    @Test
    fun testGetStaticRemoteImages() {
        runBlocking {
            val p = "challenges/challenge_swole.png"
            val ref = remote.getValidStorageRef(p)
            assertNotNull("ref $ref", ref)
            Timber.d("Got remote ref $ref, ${ref!!.path}")
        }
    }
}
