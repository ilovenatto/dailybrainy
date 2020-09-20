package org.chenhome.dailybrainy.repo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.chenhome.dailybrainy.TestLifecycleOwner
import org.chenhome.dailybrainy.repo.game.GameStub
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

    private val user = UserRepo(appContext)
    private val repo = BrainyRepo(user)
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
            delay(2000)
            suspendCoroutine<Unit> { cont ->
                repo.challenges.observe(owner, Observer<List<Challenge>> {
                    Timber.d("Found ${it.size} challenges")
                    assertNotNull(it)
                    assertEquals(3, it.size)
                    it.forEach { ch ->
                        assertEquals(Challenge.Category.CHALLENGE, ch.category)
                    }
                    cont.resume(Unit)
                })
            }
            suspendCoroutine<Unit> { cont ->
                repo.lessons.observe(owner, Observer<List<Challenge>> {
                    Timber.d("Found ${it.size} lessons")
                    assertNotNull(it)
                    assertEquals(2, it.size)
                    it.forEach { ch ->
                        assertEquals(Challenge.Category.LESSON, ch.category)
                    }
                    cont.resume(Unit)
                })
            }
            suspendCoroutine<Unit> { cont ->
                repo.todayChallenge.observe(owner, Observer<Challenge> {
                    assertNotNull(it)
                    assertEquals(Challenge.Category.CHALLENGE, it.category)
                    cont.resume(Unit)
                })
            }
            suspendCoroutine<Unit> { cont ->
                repo.todayLesson.observe(owner, Observer<Challenge> {
                    assertNotNull(it)
                    assertEquals(Challenge.Category.LESSON, it.category)
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

    @Test
    fun testInsertSession() {
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

            val player = PlayerSession()
            player.name = "asdf"
            player.gameGuid = game.guid
            player.imgFn = "asdfads"
            player.userGuid = user.currentPlayerGuid
            val guid = repo.insertPlayerSession(game.guid, player)
            assertNotNull(guid)

            suspendCoroutine<Unit> {
                val playerRef = fireDb.getReference(DbFolder.PLAYERSESSION.path)
                    .child(gameRef.key!!)
                    .child(guid!!)
                Timber.d("Getting session at $playerRef")
                playerRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val compare = player.copy(guid = guid, gameGuid = game.guid)
                        val remote = snapshot.getValue<PlayerSession>()
                        Timber.d("Got session $remote")
                        assertEquals(compare, remote)
                        it.resume(Unit)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        it.resume(Unit)
                    }
                })
            }

        }
    }

    @Test
    fun testInsertGame() {
        val player = PlayerSession()
        player.name = "asdf"
        player.imgFn = "asdfads"

        runBlocking {
            val gameGuid = repo.insertNewGame("foobar", player, user.currentPlayerGuid)
            assertNotNull(gameGuid)

            // get game
            suspendCoroutine<Unit> {
                val gameRef = fireDb.getReference(DbFolder.GAMES.path).child(gameGuid!!)
                gameRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val remote = snapshot.getValue<Game>()
                        assertEquals(gameGuid, remote?.guid)
                        assertEquals("foobar", remote?.challengeGuid)
                        assertNotNull(remote?.playerGuid)
                        it.resume(Unit)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        it.resume(Unit)
                    }

                })
            }
        }
    }
}
