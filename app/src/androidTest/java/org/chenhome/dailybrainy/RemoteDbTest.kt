package org.chenhome.dailybrainy

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

import org.chenhome.dailybrainy.repo.local.BrainyDb
import org.chenhome.dailybrainy.repo.remote.RemoteDb
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteDbTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var appContext: Context
    lateinit var fireDb: RemoteDb
    lateinit var localDb: BrainyDb
    lateinit var remoteDb: FirebaseDatabase

    @Before
    fun before() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        fireDb = RemoteDb
        fireDb.registerHandlers(appContext)
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
        val c1 = egChall1
        runBlocking {
            delay(8000)
            assertEquals(3, localDb.challengeDAO.getAll().size)

            // now add one
            val ref = remoteDb.getReference("challenges")
            ref.child(c1.guid).setValue(c1)
            delay(3000)
            assertEquals(4, localDb.challengeDAO.getAll().size)

            // remove it
            ref.child(c1.guid).setValue(null)
            delay(6000)

            assertEquals(3, localDb.challengeDAO.getAll().size)
        }
    }

}
