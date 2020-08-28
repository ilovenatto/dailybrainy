package org.chenhome.dailybrainy.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.chenhome.dailybrainy.TestLifecycleOwner
import org.chenhome.dailybrainy.blockingObserve
import org.chenhome.dailybrainy.createFullGame
import org.chenhome.dailybrainy.repo.UserRepo
import org.chenhome.dailybrainy.repo.helper.nukeRemoteDb
import org.chenhome.dailybrainy.ui.challenges.ViewChallengesVM
import org.chenhome.dailybrainy.ui.game.NewGameVM
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class ViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val owner = TestLifecycleOwner()
    private val userRepo = UserRepo(appContext)
    private val viewChallengesVM = ViewChallengesVM(appContext)
    private val newGameVM = NewGameVM(userRepo, appContext)


    @Before
    fun before() {
        runBlocking {
            // add game for the current user
            owner.reg.currentState = Lifecycle.State.INITIALIZED
            createFullGame(appContext)
            owner.reg.currentState = Lifecycle.State.CREATED
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
    fun viewChallenges_testChallenges() {
        runBlocking {
            val challenges = viewChallengesVM.challenges.blockingObserve()
            assertNotNull(challenges)
            assertEquals(5, challenges?.size)
        }
    }

    @Test
    fun viewChallenges_testGames() {
        runBlocking {
            val games = viewChallengesVM.games.blockingObserve()
            assertNotNull(games)
            assertEquals(0, games?.size)
        }
    }

    @Test
    fun newGame_testPlayer() {
        newGameVM.player.observe(owner, Observer {
            assertNotNull(it)
        })
    }
}