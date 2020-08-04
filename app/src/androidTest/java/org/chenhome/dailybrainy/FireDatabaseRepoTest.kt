package org.chenhome.dailybrainy

import android.content.Context
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import junit.framework.Assert.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.chenhome.dailybrainy.repo.FireDatabaseRepo
import org.chenhome.dailybrainy.repo.LocalImageRepo
import org.chenhome.dailybrainy.repo.RemoteImageFolder
import org.chenhome.dailybrainy.repo.RemoteImageRepo
import org.chenhome.dailybrainy.repo.local.BrainyDb
import org.chenhome.dailybrainy.repo.local.ChallengeDb
import org.junit.*
import org.junit.runner.RunWith
import timber.log.Timber
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@RunWith(AndroidJUnit4::class)
class FireDatabaseRepoTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var appContext: Context
    lateinit var fireDb: FireDatabaseRepo
    lateinit var localDb: BrainyDb
    lateinit var remoteDb: FirebaseDatabase

    val c1 = ChallengeDb(0, "challenge-foobar", "asdf", "fasdf", "asdfad", "asdfa")

    @Before
    fun before() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        fireDb = FireDatabaseRepo(appContext)
        fireDb.registerHandlers()
        localDb = BrainyDb.getDb(appContext)
        remoteDb = Firebase.database
    }

    @After
    fun after() {
        fireDb.deregisterHandlers()
        localDb.clearAllTables()
    }

    @Test
    fun testAdd() {
        runBlocking {
            delay(8000)
            localDb.challengeDAO.getAllBlocking()?.let {
                assertEquals(3, it.size)
            }
            localDb.lessonDAO.getAll().blockingObserve()?.let {
                assertEquals(2, it.size)
            }


            // now add one
            val ref = remoteDb.getReference("challenges")
            ref.child(c1.guid).setValue(c1)
            delay(3000)

            localDb.challengeDAO.getAllBlocking().let {
                assertEquals(4, it.size)
            }

            // remove it
            ref.child(c1.guid).setValue(null)
            delay(6000)

            localDb.challengeDAO.getAllBlocking().let {
                assertEquals(3, it.size)
            }
        }
    }

}
