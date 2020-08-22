package org.chenhome.dailybrainy.repo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.chenhome.dailybrainy.TestLifecycleOwner
import org.chenhome.dailybrainy.repo.helper.nukeRemoteDb
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@RunWith(AndroidJUnit4::class)
class BrainyRepoTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val repo = BrainyRepo.singleton(appContext)
    private val user = UserRepo(appContext)
    private val owner = TestLifecycleOwner()
    private val fireDb = FirebaseDatabase.getInstance()

    @Before
    fun before() {
        owner.reg.currentState = Lifecycle.State.INITIALIZED
    }

    @After
    fun after() {
        user.clearUserPrefs()
        owner.reg.currentState = Lifecycle.State.DESTROYED
        nukeRemoteDb()
    }

    @Test
    fun testChallenges() {
        runBlocking {
            assertNotNull(repo.challenges)
            owner.reg.currentState = Lifecycle.State.STARTED

            suspendCoroutine<Unit> { cont ->
                repo.challenges.observe(owner, Observer<List<Challenge>> {
                    Timber.d("Found ${it.size} challenges")
                    assertNotNull(it)
                    assertEquals(5, it.size)
                    assert(it[0].hmw?.isNotEmpty()!!)
                    cont.resume(Unit)
                })
            }
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
    fun testGameStub() {
        runBlocking {


            // add game
            val gameRef = fireDb.getReference(DbFolder.GAMES.path)
                .push()
            val game = Game(gameRef.key!!, "", "")
            suspendCoroutine<Unit> {
                gameRef.setValue(game) { e, _ ->
                    assertNull(e)
                    it.resume(Unit)
                }
            }

            // register observers
            owner.reg.currentState = Lifecycle.State.STARTED

            // add session
            val sessionRef = fireDb.getReference(DbFolder.PLAYERSESSION.path)
                .child(game.guid)
                .push()
            val session =
                PlayerSession(sessionRef.key!!, user.currentPlayerGuid, game.guid, "Samuel")
            suspendCoroutine<Unit> {
                sessionRef.setValue(session) { e, _ ->
                    assertNull(e)
                    Timber.d("Added session1")
                    it.resume(Unit)
                }
            }

            val sessionRef2 = fireDb.getReference(DbFolder.PLAYERSESSION.path)
                .child(game.guid)
                .push()
            val session2 =
                PlayerSession(sessionRef2.key!!, user.currentPlayerGuid, game.guid, "Samuel2")
            suspendCoroutine<Unit> {
                sessionRef2.setValue(session2) { e, _ ->
                    assertNull(e)
                    Timber.d("Added session2")
                    it.resume(Unit)
                }
            }
            // observe gamestubs and wait for new session
            repo.gameStubs.observe(owner, Observer<List<GameStub>> {
                Timber.d("observed list ${it.size}")
                it.forEach { stub ->
                    assertEquals(game, stub.game)
                    if (stub.playerSession.guid == session.guid) assertEquals(
                        session,
                        stub.playerSession
                    )
                    if (stub.playerSession.guid == session2.guid) assertEquals(
                        session2,
                        stub.playerSession
                    )
                }
            })
            delay(3000)
        }
    }
}
