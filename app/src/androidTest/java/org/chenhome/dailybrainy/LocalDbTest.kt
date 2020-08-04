package org.chenhome.dailybrainy

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.chenhome.dailybrainy.repo.local.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class LocalDbTest {
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var appContext : Context
    lateinit var db : BrainyDb
    val egChall1 = ChallengeDb(0, "chall1", "Getting down and up","HMW do this","HMW do this and that", "asdfadsf")
    val egChall2 = ChallengeDb(
        0,
        "chall2",
        "Getting down and up2",
        "HMW2 do this",
        "HMW2 do this and that",
        "adsasdf"
    )
    val egGame = GameDb(0, genGuid(), 0, "1234", System.currentTimeMillis())
    val egPlayer = PlayerDb(
        id = 0,
        gameId = 0,
        name = "p1",
        points = 100,
        imgFn = "file://foobar",
        guid = "sadfadf"
    )
    val egStory =StoryboardDb(0,0,"3 little pigs", "an awseoms tory", "asdfasf", "asdfadsf","asdfadsf")
    val egIdea = IdeaDb(0,"asdfadsf","asdfasdf",0,0, ChallengeDb.Phase.BRAINSTORM)


    fun insertGameHelper(): GameDb {
        val cId = db.challengeDAO.insert(egChall1)
        val game = egGame.copy(challengeId = cId)
        val gid = db.gameDAO.insert(game)
        return game.copy(id=gid)
    }


    @Before
    fun before() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext

        // In memory fixture
        db = Room.inMemoryDatabaseBuilder(
            appContext, BrainyDb::class.java).build()

    }

    @After
    fun after() {
        db.clearAllTables()
    }

    @Test
    fun testChallengeDb() {
        // Insert challenge
        val c1Id = db.challengeDAO.insert(egChall1)
        assertTrue(c1Id > 0)
        val c2Id = db.challengeDAO.insert(egChall2)
        assertTrue(c2Id > 0)

        // Get one
        val c1 = db.challengeDAO.get(c1Id)
        assertEquals(egChall1.copy(id = c1Id), c1)

        // update
        val modified = c1?.copy(title= "412312asdfadsf322")
        assertEquals(1,db.challengeDAO.update(modified!!))
        assertEquals(modified,
            db.challengeDAO.get(c1Id))

        // Get all
        val challenges = db.challengeDAO.getAll().blockingObserve()
        val challengesDead = db.challengeDAO.getAllBlocking()
        assertEquals(2, challenges?.size)
        assertEquals(2, challengesDead?.size)
        challenges?.forEach {
            when (it.id) {
                c1Id -> assertEquals(modified, it)
                c2Id -> assertEquals(egChall2.copy(id = c2Id), it)
            }
        }
        challengesDead?.forEach {
            when (it.id) {
                c1Id -> assertEquals(modified, it)
                c2Id -> assertEquals(egChall2.copy(id = c2Id), it)
            }
        }


        // delete
        assertEquals(1,db.challengeDAO.delete(egChall1.copy(id = c1Id)))
        assertEquals(1,db.challengeDAO.delete(egChall2.copy(id = c2Id)))
    }

    @Test
    fun testPlayerDAO() {
        // add parent
        val gId = insertGameHelper().id
        val guid = genGuid()
        val p1 = egPlayer.copy(gameId = gId, guid = guid)

        // insert
        val id = db.playerDAO.insert(p1)
        val insertedP1 = db.playerDAO.get(id)
        assertEquals(p1.copy(id = id), insertedP1)

        // get by guid
        assertEquals(p1.copy(id =id), db.playerDAO.getByGuid(guid))

        // update
        val modifiedP = insertedP1?.copy(points=200)
        assertEquals(1,db.playerDAO.update(modifiedP!!))
        assertEquals(modifiedP,
            db.playerDAO.get(id))

        // insert antoher
        val toInsert = insertedP1.copy(name="asdfasdf", id=0)
        val id2 = db.playerDAO.insert(toInsert)
        assertTrue(id2 > 0)
        val insertedP2 = db.playerDAO.get(id2)
        assertEquals(toInsert.copy(id=id2), insertedP2)

        // get by game
        val playersByGame = db.playerDAO.getByGame(gId).blockingObserve()
        assertTrue(playersByGame!= null)
        assertEquals(2, playersByGame?.size)
        playersByGame?.forEach {
            when (it.id) {
                id->assertEquals(modifiedP, it)
                id2-> assertEquals(insertedP2, it)
            }
        }

        // delete
        assertEquals(1,db.playerDAO.delete(modifiedP))
    }

    @Test
    fun testGameDAO() {
        val cId = db.challengeDAO.insert(egChall1)
        val game = egGame.copy(challengeId = cId)

        // insert and get
        val id = db.gameDAO.insert(game)
        val g = db.gameDAO.get(id)
        assertEquals("Got $g", game.copy(id = id), g)

        // check challenge the same
        assertEquals(egChall1.copy(id=cId), db.challengeDAO.get(g!!.challengeId))

        // update
        val modified = g?.copy(pin="4322", currentStep = ChallengeDb.Step.GEN_SKETCH)
        assertEquals(1,db.gameDAO.update(modified!!))
        assertEquals(modified,
            db.gameDAO.get(id))

        // delete
        assertEquals(1,db.gameDAO.delete(modified))
    }

    @Test
    fun testStoryboardDAO() {
        // add game
        val gId = insertGameHelper().id

        // insert and get
        val s = egStory.copy(gameId = gId)
        val id = db.storyboardDAO.insert(s)
        val insertedS = db.storyboardDAO.get(id).blockingObserve()
        val insertedS2 = db.storyboardDAO.getByGame(gId).blockingObserve()
        assertEquals(s.copy(id = id),insertedS)
        assertEquals(s.copy(id = id),insertedS2)

        // update
        val modified = insertedS?.copy(title = "fdsafadsfadf")
        assertEquals(1,db.storyboardDAO.update(modified!!))
        assertEquals(modified,
            db.storyboardDAO.get(id).blockingObserve())

        // delete
        assertEquals(1,db.storyboardDAO.delete(modified))
    }

    @Test
    fun testPreloadJson() {
        val result = BrainyDb.BrainyDbHelper.getChallengesFromJson(appContext)
        assertTrue(result != null && result.size == 3)
    }

    @Test
    fun testIdea() {
        // insert game and storyboard
        val gId = insertGameHelper().id
        assertTrue(gId> 0)

        // Does TypeConverter work?
        // insert
        val iid = db.ideaDAO.insert(egIdea.copy(gameId = gId))
        assertTrue(iid >0)
        val inserted1 = db.ideaDAO.get(iid)
        assertEquals(egIdea.copy(id=iid, gameId=gId), inserted1)

        val changed = egIdea.copy(id=iid, gameId=gId, phase = ChallengeDb.Phase.SKETCH)
        assertEquals(1,db.ideaDAO.update(changed))
        val finalIdea1 = db.ideaDAO.get(iid)
        assertEquals(changed, finalIdea1)

        // insert another
        val idea2 = egIdea.copy(gameId = gId, phase = ChallengeDb.Phase.SKETCH, title = "ffdaas")
        val iid2 = db.ideaDAO.insert(idea2)
        assertTrue(iid2 > 0)

        // get all
        assertEquals(2,db.ideaDAO.getAll(gId).size)

        // test getIDea by Step. should be 2 of create_storyboard
        val ideasByStep = db.ideaDAO.getByPhase(gId,
            ChallengeDb.Phase.SKETCH).blockingObserve()
        assertTrue(ideasByStep != null && ideasByStep?.size==2)
        ideasByStep?.forEach {
            assertTrue(it.phase== ChallengeDb.Phase.SKETCH)
        }
    }

    @Test
    fun testGetGamesForPlayer() {
        val g1 = db.gameDAO.get(insertGameHelper().id)
        val g2 = db.gameDAO.get(insertGameHelper().id)
        val g3 = db.gameDAO.get(insertGameHelper().id)

        val guidA = genGuid()
        val pA1 = db.playerDAO.get(
            db.playerDAO
                .insert(egPlayer.copy(guid = guidA, gameId = g1!!.id))
        )
        val pA2 = db.playerDAO.get(db.playerDAO
            .insert(egPlayer.copy(guid = guidA, gameId = g2!!.id)))
        val pA3 = db.playerDAO.get(db.playerDAO
            .insert(egPlayer.copy(guid = guidA, gameId = g3!!.id)))
        val pB1 = db.playerDAO.get(
            db.playerDAO
                .insert(egPlayer.copy(guid = genGuid(), gameId = g1!!.id))
        )
        assertTrue(pA1 !=  null && pA2 != null && pA3 != null && pB1 != null)

        val res = db.gameDAO.getByPlayer(guidA).blockingObserve()
        assertTrue(res != null)
        assertEquals(3, res?.size)
        res?.forEach {
            when(it.id) {
                g1?.id -> assertEquals(g1, it)
                g2?.id -> assertEquals(g2, it)
                g3?.id -> assertEquals(g3, it)
            }
        }
    }

    @Test
    fun testLessonDb() {
        val egLesson = LessonDb(0, genGuid(), "asdf", "asdf", "asdfasdf", "asdfasdf")
        val lId = db.lessonDAO.insert(egLesson)
        assertTrue(lId>0)
        val l = db.lessonDAO.get(lId)
        assertEquals(egLesson.copy(id=lId), l)
        val egLesson2 = egLesson.copy(id=0,title="dfasdfadsfads")
        val l2Id = db.lessonDAO.insert(egLesson2)
        val l2 = db.lessonDAO.get(l2Id)
        val ls = db.lessonDAO.getAll().blockingObserve()
        assertTrue(ls != null && ls.size == 2)
        ls?.forEach {
            when(it.id) {
                lId -> assertEquals(egLesson.copy(id=lId), l)
                l2Id -> assertEquals(egLesson2.copy(id=l2Id), l2)
            }
        }

    }
}


