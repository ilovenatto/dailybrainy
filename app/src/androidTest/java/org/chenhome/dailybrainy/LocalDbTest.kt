package org.chenhome.dailybrainy

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.chenhome.dailybrainy.repo.local.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class LocalDbTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var appContext: Context
    lateinit var db: BrainyDb

    fun insertUniqueGame(): Game {
        val game = egGame.copy(guid = genGuid())
        assertTrue(db.gameDAO.insert(game) > 0)
        return game
    }


    @Before
    fun before() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext

        // In memory fixture
        db = Room.inMemoryDatabaseBuilder(
            appContext, BrainyDb::class.java
        ).build()

    }

    @After
    fun after() {
        db.clearAllTables()
    }

    @Test
    fun testChallengeDb() {
        // Insert challenge
        assertTrue(db.challengeDAO.insert(egChall1) > 0)
        assertTrue(db.challengeDAO.insert(egChall2) > 0)

        // Get one
        val c1 = db.challengeDAO.get(egChall1.guid)
        assertEquals(egChall1, c1)

        // update
        val modified = c1?.copy(title = "412312asdfadsf322")
        assertEquals(1, db.challengeDAO.update(modified!!))
        assertEquals(
            modified,
            db.challengeDAO.get(modified.guid)
        )

        // Get all
        val challenges = db.challengeDAO.getAll()
        assertEquals(2, challenges.size)
        challenges.forEach {
            when (it.guid) {
                modified.guid -> assertEquals(modified, it)
                egChall2.guid -> assertEquals(egChall2, it)
            }
        }

        // delete
        assertEquals(1, db.challengeDAO.delete(egChall1.guid))
        assertEquals(1, db.challengeDAO.delete(egChall2.guid))
    }

    @Test
    fun testPlayerDAO() {
        // add parent
        val gId = insertUniqueGame().guid
        val p1 = egPlayer.copy(gameGuid = gId)

        Timber.d("got guid ${gId} ${p1.guid}")
        // insert
        assertTrue(db.playerSessionDAO.insert(p1) > 0)
        val insertedP1 = db.playerSessionDAO.get(p1.guid)
        assertEquals(p1, insertedP1)

        // getByPlayer
        assertEquals(p1, db.playerSessionDAO.getByPlayer(p1.playerGuid))

        // update
        val modifiedP = insertedP1?.copy(name = "foobar")
        assertEquals(1, db.playerSessionDAO.update(modifiedP!!))
        assertEquals(
            modifiedP,
            db.playerSessionDAO.get(insertedP1.guid)
        )

        // insert antoher
        val toInsert = insertedP1.copy(name = "asdfasdf", guid = genGuid())
        assertTrue(db.playerSessionDAO.insert(toInsert) > 0)
        assertEquals(toInsert, db.playerSessionDAO.get(toInsert.guid))

        // get by game
        val playersByGame = db.playerSessionDAO.getByGameLive(gId).blockingObserve()
        assertTrue(playersByGame != null)
        assertEquals(2, playersByGame?.size)
        playersByGame?.forEach {
            when (it.guid) {
                p1.guid -> assertEquals(modifiedP, it)
                toInsert.guid -> assertEquals(toInsert, it)
            }
        }

        // delete
        assertEquals(1, db.playerSessionDAO.delete(p1.guid))
    }

    @Test
    fun testGameDAO() {
        // insert and get
        assertTrue(db.gameDAO.insert(egGame) > 0)
        assertEquals(egGame, db.gameDAO.get(egGame.guid))

        // update
        val modified = egGame.copy(pin = "4322", currentStep = Challenge.Step.GEN_SKETCH)
        assertEquals(1, db.gameDAO.update(modified))
        assertEquals(
            modified,
            db.gameDAO.get(egGame.guid)
        )

        // delete
        assertEquals(1, db.gameDAO.delete(modified.guid))


    }

    @Test
    fun testIdea() {
        val game1 = insertUniqueGame()

        // Does TypeConverter work?
        // insert
        val idea1 = egIdea.copy(gameGuid = game1.playerGuid)
        assertTrue(db.ideaDAO.insert(idea1) > 0)

        val inserted1 = db.ideaDAO.get(idea1.guid)
        assertEquals(idea1, inserted1)

        val changed = idea1.copy(origin = Idea.Origin.STORY_RESOLUTION)
        assertEquals(1, db.ideaDAO.update(changed))
        val finalIdea1 = db.ideaDAO.get(changed.guid)
        assertEquals(changed, finalIdea1)

        // insert 2 of same origin
        val idea2 = egIdea.copy(
            guid = genGuid(),
            gameGuid = game1.guid,
            origin = Idea.Origin.STORY_SETTING,
            title = "ffdaas"
        )
        val idea3 = egIdea.copy(
            guid = genGuid(),
            gameGuid = game1.guid,
            origin = Idea.Origin.STORY_SETTING,
            title = "f234fdaas"
        )
        assertTrue(db.ideaDAO.insert(idea2) > 0)
        assertTrue(db.ideaDAO.insert(idea3) > 0)

        // get by origin
        val ideasByOrigin = db.ideaDAO.getByOriginLive(game1.guid, Idea.Origin.STORY_SETTING)
            .blockingObserve()
        assertTrue(ideasByOrigin != null)
        assertEquals("Got ideas $ideasByOrigin", 2, ideasByOrigin?.size)
        ideasByOrigin?.forEach {
            assertTrue(it.origin == Idea.Origin.STORY_SETTING)
        }
    }

    @Test
    fun testGetGamesForPlayer() {
        val g1 = db.gameDAO.get(insertUniqueGame().guid)
        val g2 = db.gameDAO.get(insertUniqueGame().guid)
        val g3 = db.gameDAO.get(insertUniqueGame().guid)

        db.playerSessionDAO
            .insert(egPlayer.copy(guid = genGuid(), gameGuid = g1!!.guid))
        db.playerSessionDAO
            .insert(egPlayer.copy(guid = genGuid(), gameGuid = g2!!.guid))
        db.playerSessionDAO
            .insert(egPlayer.copy(guid = genGuid(), gameGuid = g3!!.guid))
        db.playerSessionDAO
            .insert(egPlayer.copy(guid = genGuid(), playerGuid = genGuid(), gameGuid = g1.guid))

        val res = db.gameDAO.getByPlayerLive(egPlayerGuid).blockingObserve()
        assertTrue(res != null)
        assertEquals(3, res?.size)
        res?.forEach {
            when (it.guid) {
                g1.guid -> assertEquals(g1, it)
                g2.guid -> assertEquals(g2, it)
                g3.guid -> assertEquals(g3, it)
            }
        }
    }
}


