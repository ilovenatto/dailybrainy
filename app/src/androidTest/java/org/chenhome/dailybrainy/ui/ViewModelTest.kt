package org.chenhome.dailybrainy.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.chenhome.dailybrainy.TestLifecycleOwner
import org.chenhome.dailybrainy.blockingObserve
import org.chenhome.dailybrainy.createFullGame
import org.chenhome.dailybrainy.repo.BrainyRepo
import org.chenhome.dailybrainy.repo.Game
import org.chenhome.dailybrainy.repo.PlayerSession
import org.chenhome.dailybrainy.repo.UserRepo
import org.chenhome.dailybrainy.repo.game.GameStub
import org.chenhome.dailybrainy.repo.helper.nukeRemoteDb
import org.chenhome.dailybrainy.ui.challenges.ViewChallengesVM
import org.chenhome.dailybrainy.ui.game.NewGameVM
import org.chenhome.dailybrainy.ui.game.ViewGameVM
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain


@HiltAndroidTest
class ViewModelTest {
    @get:Rule
    val rule = RuleChain.outerRule(HiltAndroidRule(this))
        .around(InstantTaskExecutorRule())

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val owner = TestLifecycleOwner()
    private val userRepo = UserRepo(context)
    private val brainyRepo = BrainyRepo(userRepo)
    private val viewChallengesVM = ViewChallengesVM(brainyRepo)
    private val newGameVM = NewGameVM(userRepo, brainyRepo)
    private lateinit var pair: Pair<Game, PlayerSession>

    @Before
    fun before() {
        runBlocking {
            // add game for the current user
            owner.reg.currentState = Lifecycle.State.INITIALIZED
            pair = createFullGame(context)
            owner.reg.currentState = Lifecycle.State.CREATED
            owner.reg.currentState = Lifecycle.State.STARTED
            delay(5000)
        }

    }

    @After
    fun after() {
        owner.reg.currentState = Lifecycle.State.DESTROYED
        nukeRemoteDb()
    }

    @Test
    fun viewChallenges_testGames() {
        runBlocking {
            val games = viewChallengesVM.games.blockingObserve()
            assertNotNull(games)
            assertEquals(1, games?.size)
            val comp = GameStub(pair.first, pair.second)
            assertEquals(comp, games?.get(0))
        }
    }

    @Test
    fun newGame_testPlayer() {
        newGameVM.player.observe(owner, Observer {
            assertNotNull(it)
        })
    }

    @Test
    fun viewGame_test() {
        val viewGameVM = ViewGameVM(pair.first.guid, context)
        runBlocking {
            delay(2000)
            val game = viewGameVM.fullGame.blockingObserve()
            assertNotNull(game)
            with(pair.first) {
                assertEquals(guid, game?.game?.guid)
                assertEquals(challengeGuid, game?.game?.challengeGuid)
                assertEquals(pin, game?.game?.pin)
            }
            with(pair.second) {
                assertEquals(1, game?.players?.size)
                val session = game?.players?.get(0)
                assertEquals(name, session?.name)
                assertEquals(imgFn, session?.imgFn)
            }
        }
    }
}