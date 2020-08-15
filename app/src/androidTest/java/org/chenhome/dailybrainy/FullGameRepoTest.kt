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

    @Before
    fun before() {
        Timber.d("@before")
        lifecycleOwner.reg.currentState = Lifecycle.State.INITIALIZED

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
                game = Game(gameRef.key!!, challenge.guid, UserRepo(appContext).currentPlayerGuid)
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
                    ideaRef.key!!, game.guid, UserRepo(appContext).currentPlayerGuid,
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
                    UserRepo(appContext).currentPlayerGuid,
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
                    Timber.d("Observed fullgame $fullGame")
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

}
