package org.chenhome.dailybrainy

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
import org.chenhome.dailybrainy.repo.*
import org.chenhome.dailybrainy.repo.helper.nukeRemoteDb
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@RunWith(AndroidJUnit4::class)
class FullGameRepoTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val fireDb: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val lifecycleOwner: TestLifecycleOwner = TestLifecycleOwner()

    lateinit var game: Game
    lateinit var idea: Idea
    lateinit var session: PlayerSession
    lateinit var challenge: Challenge
    lateinit var userId: String

    @Before
    fun before() {
        Timber.d("@before")
        lifecycleOwner.reg.currentState = Lifecycle.State.INITIALIZED
        userId = UserRepo(appContext).currentPlayerGuid

        runBlocking {
            suspendCoroutine<Unit> {
                fireDb.getReference(DbFolder.CHALLENGES.path)
                    .child("challenge-my-party")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onCancelled(error: DatabaseError) {}
                        override fun onDataChange(snapshot: DataSnapshot) {
                            challenge = snapshot.getValue<Challenge>() ?: error("expected value")
                            it.resume(Unit)
                        }
                    })
            }

            suspendCoroutine<Unit> {
                // create game with idea, challenge and session
                val gameRef = fireDb.getReference(DbFolder.GAMES.path).push()
                game = Game(gameRef.key!!, challenge.guid, userId)
                gameRef.setValue(game) { _, _ ->
                    it.resume(Unit)
                }
            }

            // create idea
            suspendCoroutine<Unit> {
                val ideaRef = fireDb.getReference(DbFolder.IDEAS.path)
                    .child(game.guid)
                    .push()
                idea = Idea(
                    ideaRef.key!!, game.guid, userId,
                    Idea.Origin.SKETCH
                )
                ideaRef.setValue(idea) { _, _ ->
                    it.resume(Unit)
                }
            }

            // create session
            suspendCoroutine<Unit> {
                val playerRef = fireDb.getReference(DbFolder.PLAYERSESSION.path)
                    .child(game.guid)
                    .push()
                session = PlayerSession(
                    playerRef.key!!,
                    userId,
                    game.guid,
                    "smauel3"
                )
                playerRef.setValue(session) { _, _ ->
                    it.resume(Unit)
                }
            }
        }
    }

    @After
    fun after() {

        Timber.d("@after")
        lifecycleOwner.reg.currentState = Lifecycle.State.DESTROYED
        nukeRemoteDb()
    }

    @Test
    fun testFullGame() {
        runBlocking {
            val fullGameRepo = FullGameRepo(appContext, game.guid, lifecycleOwner)
            lifecycleOwner.reg.currentState = Lifecycle.State.STARTED
            delay(3000)


            Timber.d("Started test")
            suspendCoroutine<Unit> { cont ->
                fullGameRepo.fullGame.observe(lifecycleOwner, Observer { fullGame ->
                    Timber.d("Observed ${fullGame.ideas.size} ideas and ${fullGame.players.size} sessions")
                    assertNotNull(fullGame)
                    assertEquals(game, fullGame.game)
                    assertEquals(challenge, fullGame.challenge)
                    assertEquals(1, fullGame.ideas.size)
                    assertEquals(idea, fullGame.ideas[0])
                    assertEquals(1, fullGame.players.size)
                    assertEquals(session, fullGame.players[0])
                    cont.resume(Unit)
                })
            }
            Timber.d("End test")
        }
    }

    @Test
    fun testInsert() {
        runBlocking {
            val fullGameRepo = FullGameRepo(appContext, game.guid, lifecycleOwner)
            lifecycleOwner.reg.currentState = Lifecycle.State.STARTED
            delay(3000)

            // insert and wait
            val idea2 = Idea("", game.guid, userId, Idea.Origin.SKETCH)
            fullGameRepo.insertRemote(idea2)
            fullGameRepo.insertRemote(idea2)

            // insert and wait
            val player2 = PlayerSession("", userId, game.guid, "foobar")
            fullGameRepo.insertRemote(player2)
            fullGameRepo.insertRemote(player2)

            delay(3000)

            // then observe
            suspendCoroutine<Unit> { cont ->
                fullGameRepo.fullGame.observe(lifecycleOwner, Observer { fullGame ->
                    Timber.d("Observed ${fullGame.ideas.size} ideas and ${fullGame.players.size} sessions")
                    assertEquals(3, fullGame.ideas.size)
                    assertEquals(idea, fullGame.ideas[0])

                    assertEquals(3, fullGame.players.size)
                    assertEquals(session, fullGame.players[0])

                    cont.resume(Unit)
                })
            }
        }
    }

    @Test
    fun testUpdatePlayer() {
        runBlocking {
            val fullGameRepo = FullGameRepo(appContext, game.guid, lifecycleOwner)
            lifecycleOwner.reg.currentState = Lifecycle.State.STARTED
            delay(3000)

            // get session from fullGame
            val fullGame = fullGameRepo.fullGame.blockingObserve()
                ?.let {
                    assertNotNull(it.players[0])
                    val session1 = it.players[0]
                    session1.imgFn = "fancy new image"
                    fullGameRepo.updateRemote(session1)
                    delay(3000)

                    // check result
                    fullGameRepo.fullGame.blockingObserve()
                        ?.let {
                            assertNotNull(it.players[0])
                            assertEquals(session1, it.players[0])
                        }
                }
        }
    }


    @Test
    fun testUpdateIdea() {
        runBlocking {
            val fullGameRepo = FullGameRepo(appContext, game.guid, lifecycleOwner)
            lifecycleOwner.reg.currentState = Lifecycle.State.STARTED
            delay(3000)

            // get from fullGame
            val fullGame = fullGameRepo.fullGame.blockingObserve()
                ?.let {
                    assertNotNull(it.ideas[0])
                    val idea1 = it.ideas[0]
                    idea1.vote()
                    fullGameRepo.updateRemote(idea1)
                    delay(3000)

                    // check result
                    fullGameRepo.fullGame.blockingObserve()
                        ?.let {
                            assertNotNull(it.ideas[0])
                            assertEquals(idea1, it.ideas[0])
                        }
                }
        }
    }


    @Test
    fun testUpdateGame() {
        runBlocking {
            val fullGameRepo = FullGameRepo(appContext, game.guid, lifecycleOwner)
            lifecycleOwner.reg.currentState = Lifecycle.State.STARTED
            delay(3000)

            // get from fullGame
            val fullGame = fullGameRepo.fullGame.blockingObserve()
                ?.let {
                    val game1 = it.game
                    assertNotNull(game1)
                    it.game.currentStep = Challenge.Step.VOTE_SKETCH
                    fullGameRepo.updateRemote(game1)
                    delay(3000)

                    // check result
                    fullGameRepo.fullGame.blockingObserve()
                        ?.let {
                            assertNotNull(it.game)
                            assertEquals(game1, it.game)
                        }
                }
        }
    }

}
