package org.chenhome.dailybrainy

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.Assert.*
import kotlinx.coroutines.runBlocking
import org.chenhome.dailybrainy.repo.BrainyRepo
import org.chenhome.dailybrainy.repo.DomainObjectMapper
import org.chenhome.dailybrainy.repo.Game
import org.chenhome.dailybrainy.repo.UserRepo
import org.chenhome.dailybrainy.repo.local.*
import org.junit.*
import org.junit.runner.RunWith
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
class BrainyRepoAdvancedTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var appContext: Context
    lateinit var repo: BrainyRepo

    @Before
    fun before() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        repo = BrainyRepo(appContext)
    }

    @After
    fun after() {
        runBlocking {
            repo.nukeEverything()
        }
    }

    @Test
    fun testLoadLocal() {
        runBlocking {
            Timber.d("Starting preload")
            val result = repo.preloadChallenges()
            Timber.d("Ending preload");

            // get challenges
            assertNotNull(repo.todayChallenge)
            val ch = repo.todayChallenge?.blockingObserve()
            Timber.d("Got challenge $ch");
            assertNotNull(ch)
            assertNotNull(ch?.title)
            assertTrue(ch!!.title!!.isNotEmpty())
        }
    }

}
