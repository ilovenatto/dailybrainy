package org.chenhome.dailybrainy.repo

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.chenhome.dailybrainy.TestLifecycleOwner
import org.chenhome.dailybrainy.blockingObserve
import org.chenhome.dailybrainy.repo.game.Sketch
import org.chenhome.dailybrainy.repo.helper.nukeRemoteDb
import org.chenhome.dailybrainy.repo.image.RemoteImage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltAndroidTest
class FullGameRepoTest {

    @get:Rule
    val rule = RuleChain.outerRule(HiltAndroidRule(this))
        .around(InstantTaskExecutorRule())

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val fireDb: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val lifecycleOwner: TestLifecycleOwner = TestLifecycleOwner()
    private val remoteImg = RemoteImage()

    lateinit var game: Game
    lateinit var idea: Idea
    lateinit var sketch: Sketch
    lateinit var session: PlayerSession
    lateinit var challenge: Challenge
    lateinit var userId: String
    lateinit var fullGameRepo: FullGameRepo
    var challengeImgUri: Uri? = null

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
                            Timber.d("Got remote challenge $challenge at location ${snapshot.ref}")
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
                    Idea.Origin.BRAINSTORM
                )
                idea.imgUri = challengeImgUri?.path
                ideaRef.setValue(idea) { _, _ ->
                    it.resume(Unit)
                }
            }

            // create sketch
            challengeImgUri = remoteImg.getValidStorageRef(challenge.imgFn)?.let {
                remoteImg.getDownloadUri(it)
            }
            assertNotNull(challengeImgUri)


            suspendCoroutine<Unit> {
                val sketchRef = fireDb.getReference(DbFolder.IDEAS.path)
                    .child(game.guid)
                    .push()
                val idea = Idea(
                    sketchRef.key!!, game.guid, userId,
                    Idea.Origin.SKETCH)
                idea.imgUri = challengeImgUri?.path
                sketch = Sketch(idea)
                sketch.idea.imgFn = challenge.imgFn
                sketchRef.setValue(sketch.idea) { _, _ ->
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

            fullGameRepo = FullGameRepo(appContext, game.guid)
            lifecycleOwner.reg.currentState = Lifecycle.State.CREATED
            lifecycleOwner.reg.currentState = Lifecycle.State.STARTED
            delay(1500)
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
            Timber.d("Started test")
            suspendCoroutine<Unit> { cont ->
                fullGameRepo.fullGame.observe(lifecycleOwner, Observer { fullGame ->
                    val notes = fullGame.ideas(Idea.Origin.BRAINSTORM)
                    val sketches = fullGame.ideas(Idea.Origin.SKETCH)

                    // just copy over the imageUri
                    challenge.imageUri = fullGame.challenge.imageUri

                    Timber.d("Observed ${notes.size} notes, ${sketches.size} sketches and ${fullGame.players.size} sessions")
                    assertNotNull(fullGame)
                    assertEquals(game, fullGame.game)
                    assertEquals(challenge, fullGame.challenge)
                    assertEquals(1, notes.size)
                    assertEquals(1, sketches.size)
                    assertEquals(idea, notes[0])
                    assertEquals(sketch, Sketch(sketches[0]))
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
            // insert and wait
            val player2 = PlayerSession("1234", userId, game.guid, "foobar")
            fullGameRepo.insertRemote(player2)
            fullGameRepo.insertRemote(player2)
            delay(2000)

            // insert 2nd idea
            val idea2 = idea.copy(guid = "")
            fullGameRepo.insertRemote(idea2)

            // insert 2nd sketch
            val sketch2 =
                Sketch(idea.copy(imgFn = challenge.imgFn, guid = "", origin = Idea.Origin.SKETCH,
                    imgUri = challengeImgUri?.path))
            fullGameRepo.insertRemote(sketch2)
            delay(1500)

            // then observe
            suspendCoroutine<Unit> { cont ->
                fullGameRepo.fullGame.observe(lifecycleOwner, Observer { fullGame ->
                    val notes = fullGame.ideas(Idea.Origin.BRAINSTORM)
                    val sketches = fullGame.ideas(Idea.Origin.SKETCH)

                    Timber.d("Observed ${notes.size} notes, ${sketches.size} sketches and ${fullGame.players.size} sessions")

                    assertEquals(2, notes.size)
                    assertEquals(idea.copy(guid = ""), notes[0].copy(guid = "")) // ignore guid

                    assertEquals(2, sketches.size)
                    assertEquals(sketch, Sketch(sketches[0]))

                    assert(idea.playerName!!.isNotEmpty())
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
            // get session from fullGame
            fullGameRepo.fullGame.blockingObserve()
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
            // get from fullGame
            fullGameRepo.fullGame.blockingObserve()
                ?.let { fullGame ->
                    val notes = fullGame.ideas(Idea.Origin.BRAINSTORM)
                    val sketches = fullGame.ideas(Idea.Origin.SKETCH)

                    assertEquals(1, notes.size)
                    val idea1 = notes[0]
                    idea1.vote()
                    fullGameRepo.updateRemote(idea1)

                    assertEquals(1, sketches.size)
                    val sketch1 = sketches[0]
                    sketch1.vote()
                    sketch1.imgFn = challenge.imgFn
                    fullGameRepo.updateRemote(sketch1)

                    delay(3000)

                    // check result
                    fullGameRepo.fullGame.blockingObserve()
                        ?.let {
                            assertEquals(idea1, it.ideas(Idea.Origin.BRAINSTORM)[0])
                            assertEquals(sketch1, it.ideas(Idea.Origin.SKETCH)[0])
                        }
                }
        }
    }


    @Test
    fun testUpdateGame() {
        runBlocking {
            // get from fullGame
            fullGameRepo.fullGame.blockingObserve()
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
