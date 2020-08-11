package org.chenhome.dailybrainy

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.chenhome.dailybrainy.repo.BrainyRepo
import org.chenhome.dailybrainy.repo.UserRepo
import org.chenhome.dailybrainy.repo.local.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
class BrainyRepoTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var appContext: Context
    lateinit var repo: BrainyRepo
    lateinit var user: UserRepo
    lateinit var db: LocalDb

    val c1: Challenge = egChall1
    val c2: Challenge = egChall2
    lateinit var l1: Challenge

    lateinit var g1: Game
    lateinit var g2: Game

    @Before
    fun before() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        repo = BrainyRepo.singleton(appContext)
        user = UserRepo(appContext)
        db = LocalDb.singleton(appContext)

        assertTrue(db.challengeDAO.insert(c1) > 0)
        assertTrue(db.challengeDAO.insert(c2) > 0)
        l1 = egChall2.copy(guid = genGuid(), category = Challenge.Category.LESSON)
        assertTrue(db.challengeDAO.insert(l1) > 0)

        g1 = egGame.copy(playerGuid = user.currentPlayerGuid)
        g2 = egGame.copy(playerGuid = user.currentPlayerGuid, guid = genGuid())
        assertTrue(db.gameDAO.insert(g1) > 0)
        assertTrue(db.gameDAO.insert(g2) > 0)

    }

    @After
    fun after() {
        db.clearAllTables()
        user.clearUserPrefs()
    }

    @Test
    fun testNuke() {
        runBlocking {
            repo.nukeEverything()
            assertEquals(0, db.challengeDAO.getAll().size)
        }
    }

    @Test
    fun testUser() {
        // get current gameid
        assert(user.currentGameGuid.isNullOrEmpty())
        user.currentGameGuid = "foobar"
        assertEquals("foobar", user.currentGameGuid)

        val guid = user.currentPlayerGuid
        assertTrue(guid.isNotEmpty())
        assertEquals(guid, user.currentPlayerGuid)
    }

    @Test
    fun testRepoGetters() {
        val myGames = db.gameDAO.getByPlayer(user.currentPlayerGuid)
        assertEquals(2, myGames.size)
        listOf(g1.guid, g2.guid).forEach {
            assertEquals("myGames ${myGames}", 1, myGames.count { game ->
                game.guid == it
            })
        }
        // today's stuff
        runBlocking {
            val lesson = repo.getNeverPlayed(Challenge.Category.LESSON)
            assertNotNull(lesson)
            assertEquals(l1, lesson)
            assertNotNull(repo.getNeverPlayed(Challenge.Category.CHALLENGE))
        }
    }

    @Test
    fun testInsertGame() {
        runBlocking {
            val newGame = repo.insertLocalGame(c1.guid)
            assertNotNull(newGame)
            assertEquals(c1.guid, newGame?.challengeGuid)
            Timber.d("Got game $newGame")
            //assertEquals(user.currentPlayerGuid, newGame?.playerGuid)
            assertEquals(Challenge.Step.GEN_IDEA, newGame?.currentStep)
            assertTrue(newGame?.sessionStartMillis!! > 0L)
            assert(newGame.pin.isNotEmpty())
        }
    }

    @Test
    fun testUpdateGameAndAddIdea() {
        runBlocking {
            val newGame = repo.insertLocalGame(c1.guid)
            assertNotNull(newGame)

            // check
            val game = db.gameDAO.get(newGame!!.guid)
            assertEquals(newGame, game)
            assertEquals(Challenge.Step.GEN_IDEA, game?.currentStep)

            // update
            game?.currentStep = Challenge.Step.VOTE_IDEA
            assert(db.gameDAO.update(game!!) > 0)

            // add idea
            val idea =
                repo.insertLocalIdea(newGame.guid, egIdea.copy(origin = Idea.Origin.BRAINSTORM))
            assertNotNull(idea)

            // get ideas
            val res =
                db.ideaDAO.getByOriginLive(newGame.guid, Idea.Origin.BRAINSTORM).blockingObserve()
            assertEquals(1, res!!.size)
            assertEquals(idea, res.get(0))
        }
    }
}
