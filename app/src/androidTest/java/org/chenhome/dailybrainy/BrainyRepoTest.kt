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
class BrainyRepoTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var appContext : Context
    lateinit var repo : BrainyRepo
    lateinit var user : UserRepo
    lateinit var db : BrainyDb

    lateinit var c1 :ChallengeDb
    lateinit var c2 :ChallengeDb
    lateinit var c3 :ChallengeDb

    lateinit var l1 : LessonDb
    lateinit var l2 :LessonDb

    lateinit var g1 : GameDb
    lateinit var g2 : GameDb
    lateinit var g3 : GameDb

    lateinit var p1 : PlayerDb
    lateinit var p2 : PlayerDb
    lateinit var p3 : PlayerDb

    lateinit var i1:IdeaDb
    lateinit var i2:IdeaDb
    lateinit var i3:IdeaDb

    lateinit var s1:StoryboardDb
    lateinit var myGuid:String

    @Before
    fun before() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        repo = BrainyRepo(appContext)
        user = UserRepo(appContext)
        db = BrainyDb.getDb(appContext)

        myGuid = user.currentPlayerGuid

        c1 = egChall1.copy(id =db.challengeDAO.insert(egChall1))
        c2 = egChall1.copy(id =db.challengeDAO.insert(egChall1.copy(title="fsdfaf")))
        c3 = egChall1.copy(id =db.challengeDAO.insert(egChall1.copy(title="fsdfa323f")))

        l1 = egLesson.copy(id=db.lessonDAO.insert(egLesson))
        l2 = egLesson.copy(id=db.lessonDAO.insert(egLesson.copy(title = "434332;asf")))

        // 2 associated with c1
        g1 = egGame.copy(id = db.gameDAO.insert(egGame.copy(challengeId=c1.id)))
        g2 = egGame.copy(id = db.gameDAO.insert(egGame.copy(challengeId=c1.id, pin="423423")))

        // 1 w/ c2
        g3 = egGame.copy(id = db.gameDAO.insert(egGame.copy(challengeId=c2.id, pin="423423432")))

        // same player w/ 2 games
        p1 = egPlayer.copy(id = db.playerDAO
            .insert(egPlayer.copy(guid=myGuid, gameId = g1.id, name="4342343")))
        p2 = egPlayer.copy(id = db.playerDAO
            .insert(egPlayer.copy(guid=myGuid, gameId = g1.id, name="434234fsd3")))

        // another plyaer w/ 1 game
        p3 = egPlayer.copy(id = db.playerDAO
            .insert(egPlayer.copy(guid="random234", gameId = g2.id, name="4342343")))

        // several ideas under g1
        i1 = egIdea.copy(id = db.ideaDAO
            .insert(egIdea.copy(gameId=g1.id, phase=ChallengeDb.Phase.BRAINSTORM)))
        i2 = egIdea.copy(id = db.ideaDAO
            .insert(egIdea.copy(gameId=g1.id, phase=ChallengeDb.Phase.BRAINSTORM)))
        i3 = egIdea.copy(id = db.ideaDAO
            .insert(egIdea.copy(gameId=g1.id, phase=ChallengeDb.Phase.SKETCH)))

        // story board
        s1 = egStory.copy(gameId=g1.id, id = db.storyboardDAO
            .insert(egStory.copy(gameId=g1.id)))

    }

    @After
    fun after() {
        db.clearAllTables()
        user.clearUserPrefs()

    }

    @Test
    fun testUser() {
        // get current gameid
        assertEquals(0,user.currentGameId)
        user.currentGameId = 100
        assertEquals(100, user.currentGameId)

        val guid = user.currentPlayerGuid
        assertTrue(guid.isNotEmpty())
        assertEquals(guid, user.currentPlayerGuid)
    }

    @Test
    fun testRepoGetters() {
        assertEquals(myGuid, user.currentPlayerGuid)
        val myGames = repo.myGames.blockingObserve()
        assertEquals(2,myGames?.size)
        /*listOf(g1.id, g2.id).forEach {
            assertEquals("myGames ${myGames}",1,myGames?.count {game->
                game.id==it
            })
        }
*/
        // today's stuff
        assertNotNull(repo.todayLesson?.blockingObserve())
        assertNotNull(repo.todayChallenge?.blockingObserve())
    }

    @Test
    fun testToGame() {
        // get game from g1 and c1
        val mapper =
            DomainObjectMapper(db, appContext)
        val game = mapper.toGame(g1, c1)
        assertEquals(g1.id, game.gameId)
        assertEquals(g1.pin, game.pin)
        assertEquals(c1.id, game.challId)
        assertEquals(c1.hmw, game.challHmw)
        assertEquals("Step2Count ${game.step2Count}", 2, game.step2Count[ChallengeDb.Step.GEN_IDEA])
        assertEquals("Step2Count ${game.step2Count}", 1, game.step2Count[ChallengeDb.Step.GEN_SKETCH])
        Timber.d("got step2count ${game.step2Count}")
    }

    @Test
    fun testToGameDb() {
        val mapper =
            DomainObjectMapper(db, appContext)
        val game = mapper.toGame(egGame, c1)
        val gameDb = mapper.toGameDb(game)
        assertEquals(egGame.copy(challengeId=c1.id), gameDb)

    }
    @Test
    fun testGetGame() {
        val game = repo.getGame(g1.id)
        assertNotNull(game)

        assertEquals(g1.id, game?.gameId)
        assertEquals(g1.pin, game?.pin)
        assertEquals(c1.id, game?.challId)
        assertEquals(c1.hmw, game?.challHmw)
        assertEquals("Step2Count ${game?.step2Count}", 2, game?.step2Count?.get(ChallengeDb.Step.GEN_IDEA))
        assertEquals("Step2Count ${game?.step2Count}", 1, game?.step2Count?.get(ChallengeDb.Step.GEN_SKETCH))
    }

    @Test
    fun testGenPin() {
        val res = Game.genPin()
        assertEquals(4,res.length)
        Timber.d("got $res")
    }

    @Test
    fun testGetPlayers() {
        // should be 2
        assertEquals(2, repo.getPlayers(g1.id).blockingObserve()?.size)
    }

    @Test
    fun testGetStoryboard() {
        assertEquals(s1, repo.getStoryboard(g1.id).blockingObserve())
    }

    @Test
    fun testGetIdeas() {
        // 2 ideas in BRainstorm, 1 idea in Sketch
        assertEquals(2, repo.getIdeas(ChallengeDb.Phase.BRAINSTORM,g1.id).blockingObserve()?.size)
        assertEquals(1, repo.getIdeas(ChallengeDb.Phase.SKETCH,g1.id).blockingObserve()?.size)
    }

    @Test
    fun testInsertGame() {
        runBlocking {
            val newGame = repo.makeAndInsertGame(c1.id)
            assertNotNull(newGame)
            assertEquals(c1.id, newGame?.challId)
            assertEquals(c1.title, newGame?.challTitle)
            assertEquals(ChallengeDb.Step.GEN_IDEA, newGame?.currentStep)
            assertTrue(newGame?.sessionStartMillisEpoch!! > 0L)
            assertTrue(newGame?.gameId > 0)
        }
    }

    @Test
    fun testUpdateGameAndAddIdea() {
        runBlocking {
            val newGame = repo.makeAndInsertGame(c1.id)
            assertNotNull(newGame)
            assertTrue(newGame?.gameId != 0L)

            // check
            val game = repo.getGame(newGame!!.gameId)
            assertEquals(ChallengeDb.Step.GEN_IDEA, game?.currentStep)

            // update
            game?.currentStep = ChallengeDb.Step.VOTE_IDEA
            assertTrue(repo.updateGame(game!!))

            // add idea. should increment
            val idea = egIdea.copy(gameId=game!!.gameId, votes = 1) // brainstorm phase
            assertTrue(repo.addIdea(idea))

            // get it
            val game2 = repo.getGame(game!!.gameId)
            assertNotNull(game2)
            assertEquals(ChallengeDb.Step.VOTE_IDEA, game2?.currentStep)

            // check numIDeas
            assertEquals(1, game2!!.step2Count[ChallengeDb.Step.GEN_IDEA])
            assertEquals(1, game2!!.step2Count[ChallengeDb.Step.VOTE_IDEA])
            assertEquals(0, game2!!.step2Count[ChallengeDb.Step.REVIEW_IDEA])
        }
    }
}
