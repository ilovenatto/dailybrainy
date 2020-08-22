package org.chenhome.dailybrainy.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.chenhome.dailybrainy.TestLifecycleOwner
import org.chenhome.dailybrainy.blockingObserve
import org.chenhome.dailybrainy.repo.DbFolder
import org.chenhome.dailybrainy.repo.Game
import org.chenhome.dailybrainy.repo.UserRepo
import org.chenhome.dailybrainy.repo.helper.nukeRemoteDb
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@RunWith(AndroidJUnit4::class)
class ViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val owner = TestLifecycleOwner()
    private val userRepo = UserRepo(appContext)
    private val viewChallengesVM = ViewChallengesVM(userRepo, appContext)
    private val fireDb = FirebaseDatabase.getInstance()


    @Before
    fun before() {
        runBlocking {
            // add game for the current user

            val gameRef = fireDb.getReference(DbFolder.GAMES.path)
                .push()
            val game = Game(gameRef.key!!, "", userRepo.currentPlayerGuid)
            suspendCoroutine<Unit> {
                gameRef.setValue(game) { e, _ ->
                    assertNull(e)
                    it.resume(Unit)
                }
            }
            // Allow BrainyRepo observers time to retreive info from remote firebase db
            owner.reg.currentState = Lifecycle.State.INITIALIZED
            owner.reg.currentState = Lifecycle.State.STARTED
            delay(2000)
        }

    }

    @After
    fun after() {
        owner.reg.currentState = Lifecycle.State.DESTROYED
        nukeRemoteDb()
    }

    @Test
    fun testGetChallenges() {
        runBlocking {
            val challenges = viewChallengesVM.challenges.blockingObserve()
            assertNotNull(challenges)
            assertEquals(5, challenges?.size)
        }
    }

    @Test
    fun testGetGames() {
        runBlocking {
            val games = viewChallengesVM.games.blockingObserve()
            assertNotNull(games)
            assertEquals(1, games?.size)
        }
    }
}