package org.chenhome.dailybrainy.repo

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.runBlocking
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
class ObjectTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var appContext: Context
    lateinit var fireDb: FirebaseDatabase
    lateinit var chall: Challenge

    @Before
    fun before() {
        Timber.d("@before")
        fireDb = FirebaseDatabase.getInstance()
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        chall = Challenge(
            "foo-bar-chall",
            "asd",
            "asd",
            "asdf",
            Challenge.Category.CHALLENGE,
            null,
            null, null
        )
    }

    @After
    fun after() {
        Timber.d("@after")
        nukeRemoteDb()
    }

    @Test
    fun testChallenge() {
        runBlocking {
            val chalRef = fireDb.getReference(DbFolder.CHALLENGES.path)
                .child(chall.guid)

            // insert challenge remotely
            suspendCoroutine<Unit> { cont ->
                chalRef.setValue(chall, { err, ref ->
                    cont.resume(Unit)
                })
            }
            // retrieve challenge
            suspendCoroutine<Unit> {
                // insert challenge
                chalRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {}
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val remote = snapshot.getValue<Challenge>()
                        assertNotNull(remote)
                        assertEquals(chall, remote)
                        it.resume(Unit)
                    }
                })
            }
            // remove challenge
            suspendCoroutine<Unit> {
                chalRef.setValue(null) { a, b ->
                    assertNull(a)
                    it.resume(Unit)
                }
            }
            // check removed
            suspendCoroutine<Unit> {
                chalRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {}
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val remote = snapshot.getValue<Challenge>()
                        assertNull(remote)
                        it.resume(Unit)
                    }
                })
            }
        }
    }


    @Test
    fun testGame() {
        runBlocking {
            val gameRef = fireDb.getReference(DbFolder.GAMES.path)
                .push()
            val game = Game(gameRef.key!!, "", "")
            // insert remotely
            suspendCoroutine<Unit> { cont ->
                gameRef.setValue(game, { err, ref ->
                    cont.resume(Unit)
                })
            }
            // retrieve
            suspendCoroutine<Unit> {
                gameRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {}
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val remote = snapshot.getValue<Game>()
                        assertNotNull(remote)
                        assertEquals(game, remote)
                        assertEquals(game.currentStep, remote?.currentStep)
                        it.resume(Unit)
                    }
                })
            }
            // update game
            val updated = game.copy(
                currentStep = Challenge.Step.CREATE_STORYBOARD,
                sessionStartMillis = System.currentTimeMillis(), storyDesc = "asdf1231232"
            )

            suspendCoroutine<Unit> {
                gameRef.setValue(updated) { a, b ->
                    assertNull(a)
                    it.resume(Unit)
                }
            }

            // retrieve
            suspendCoroutine<Unit> {
                gameRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {}
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val remote = snapshot.getValue<Game>()
                        assertNotNull(remote)
                        assertEquals(updated, remote)
                        it.resume(Unit)
                    }
                })
            }

            // remove challenge
            suspendCoroutine<Unit> {
                gameRef.setValue(null) { a, b ->
                    assertNull(a)
                    it.resume(Unit)
                }
            }
            // check removed
            suspendCoroutine<Unit> {
                gameRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {}
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val remote = snapshot.getValue<Game>()
                        assertNull(remote)
                        it.resume(Unit)
                    }
                })
            }
        }
    }


    @Test
    fun testIdea() {
        val gameRef = fireDb.getReference(DbFolder.GAMES.path)
            .push()
        val game = Game(gameRef.key!!, "", "")

        runBlocking {
            // insert remotely
            suspendCoroutine<Unit> { cont ->
                gameRef.setValue(game) { _, _ ->
                    cont.resume(Unit)
                }
            }

            // add idea
            val ideaRef = fireDb.getReference(DbFolder.IDEAS.path)
                .child(game.guid)
                .push()
            val idea = Idea(ideaRef.key!!, game.guid, "", Idea.Origin.BRAINSTORM)
            suspendCoroutine<Unit> {
                ideaRef.setValue(idea) { e, _ ->
                    assertNull(e)
                    it.resume(Unit)
                }
            }

            // check idea
            suspendCoroutine<Unit> {
                ideaRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {}
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val remote = snapshot.getValue<Idea>()
                        assertNotNull(remote)
                        assertEquals(idea, remote)
                        it.resume(Unit)
                    }
                })
            }

            // update idea
            val before = idea.votes
            suspendCoroutine<Unit> {
                idea.vote()
                ideaRef.setValue(idea) { e, _ ->
                    assertNull(e)
                    it.resume(Unit)
                }
            }

            // check idea
            suspendCoroutine<Unit> {
                ideaRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {}
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val remote = snapshot.getValue<Idea>()
                        assertNotNull(remote)
                        assertEquals(idea, remote)
                        assertEquals(before + 1, remote?.votes)
                        it.resume(Unit)
                    }
                })
            }
        }
    }

    @Test
    fun testSession() {
        val gameRef = fireDb.getReference(DbFolder.GAMES.path)
            .push()
        val game = Game(gameRef.key!!, "", "")

        runBlocking {
            // insert remotely
            suspendCoroutine<Unit> { cont ->
                gameRef.setValue(game) { _, _ ->
                    cont.resume(Unit)
                }
            }

            // add playerSession
            val playerSessionRef = fireDb.getReference(DbFolder.PLAYERSESSION.path)
                .child(game.guid)
                .push()
            val playerSession = PlayerSession(playerSessionRef.key!!, "", game.guid, "barfoo")
            suspendCoroutine<Unit> {
                playerSessionRef.setValue(playerSession) { e, _ ->
                    assertNull(e)
                    it.resume(Unit)
                }
            }

            // check playerSession
            suspendCoroutine<Unit> {
                playerSessionRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {}
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val remote = snapshot.getValue<PlayerSession>()
                        assertNotNull(remote)
                        assertEquals(playerSession, remote)
                        it.resume(Unit)
                    }
                })
            }
        }
    }


}
