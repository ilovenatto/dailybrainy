package org.chenhome.dailybrainy

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.chenhome.dailybrainy.repo.BrainyRepo
import org.chenhome.dailybrainy.repo.local.*
import org.chenhome.dailybrainy.repo.remote.DbFolder
import org.chenhome.dailybrainy.repo.remote.RemoteDb
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
class RemoteDbTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var appContext: Context
    lateinit var fireDb: RemoteDb
    lateinit var localDb: LocalDb
    lateinit var remoteDb: FirebaseDatabase
    lateinit var brainyRepo: BrainyRepo
    lateinit var lifecycleOwner: TestLifecycleOwner

    class TestLifecycleOwner : LifecycleOwner {
        val reg = LifecycleRegistry(this)
        override fun getLifecycle(): Lifecycle = reg
    }

    @Before
    fun before() {
        Timber.d("@before")
        appContext = InstrumentationRegistry.getInstrumentation().targetContext

        fireDb = RemoteDb
        localDb = LocalDb.singleton(appContext)
        remoteDb = Firebase.database
        brainyRepo = BrainyRepo.singleton(appContext)

        localDb.clearAllTables()
        localDb.challengeDAO.insert(egChall1)
        lifecycleOwner = TestLifecycleOwner()
        lifecycleOwner.reg.currentState = Lifecycle.State.INITIALIZED
    }

    @After
    fun after() {

        Timber.d("@after")
        localDb.clearAllTables()
        lifecycleOwner.reg.currentState = Lifecycle.State.DESTROYED

        // remove all games and ideas
        runBlocking {
            remoteDb.getReference(DbFolder.GAMES.path).setValue(null)
            remoteDb.getReference(DbFolder.IDEAS.path).setValue(null)
            delay(DEL)
        }
    }

    val DEL = 1500L

    @Test
    @Ignore
    fun testChallenges() {
        val c1 = egChall1
        runBlocking {
            brainyRepo.registerRemoteChallengeAndGameObservers(lifecycleOwner)
            lifecycleOwner.reg.currentState = Lifecycle.State.CREATED
            delay(DEL * 3)
            assertEquals(6, localDb.challengeDAO.getAll().size)
        }
    }

    @Test
    @Ignore
    fun testIdeas() {
        val c1 = egChall1
        runBlocking {
            // insert local game
            val game = egGame.copy(challengeGuid = c1.guid)
            localDb.gameDAO.insert(game)

            // register observers of local and remote data for this specific game
            brainyRepo.registerGameObservers(game.guid, lifecycleOwner)
            lifecycleOwner.reg.currentState = Lifecycle.State.CREATED
            delay(DEL)
            lifecycleOwner.reg.currentState = Lifecycle.State.STARTED

            // add new remote idea
            val idea1 =
                egIdea.copy(gameGuid = game.guid, guid = genGuid(), origin = Idea.Origin.BRAINSTORM)
            fireDb.addRemote(listOf(idea1))
            Timber.d("idea1.fireGuid ${idea1.fireGuid}")
            // wait for remote to get updated
            delay(DEL * 2)

            // check tthat remote idea got added to remote db
            launch {
                assertRemoteIdea(idea1.fireGuid!!, idea1.gameGuid, idea1)
            }

            // remote observer should have tried to insert remote entity in local db
            delay(DEL)
            val recentlyadded = localDb.ideaDAO
                .getByOriginLive(game.guid, Idea.Origin.BRAINSTORM).blockingObserve()
            assertEquals(1, recentlyadded?.size)
            assertEquals(idea1, recentlyadded?.get(0))

            // remove remobte idea
            val removeRef = remoteDb.getReference(DbFolder.IDEAS.path)
                .child(idea1.gameGuid)
                .child(idea1.fireGuid!!)
            Timber.d("Removing ${idea1.guid}, ${removeRef.path}")
            removeRef.setValue(null)
            delay(DEL)

            // remote observer should have honored that and removed from local db
            assertEquals(
                0, localDb.ideaDAO
                    .getByOriginLive(game.guid, Idea.Origin.BRAINSTORM).blockingObserve()?.size
            )

            // add local idea and check it got to remote db
            val idea2 =
                egIdea.copy(gameGuid = game.guid, guid = genGuid(), origin = Idea.Origin.BRAINSTORM)
            Timber.d("Insert new local idea $idea2")
            localDb.ideaDAO.insert(idea2)
            delay(DEL * 3)
            // get fireGuid
            val updatedIdea = localDb.ideaDAO.get(idea2.guid)
            Timber.d("Got updated idea $updatedIdea")
            delay(DEL * 2)

            launch {
                assertRemoteIdea(updatedIdea?.fireGuid!!, game.guid, updatedIdea)
            }
            delay(DEL)

            Timber.d("Finished test")
        }
    }

    @Test
    fun testGame() {

        runBlocking {
            // insert local game
            val game = egGame.copy(challengeGuid = egChall1.guid)
            localDb.gameDAO.insert(game)

            brainyRepo.registerGameObservers(game.guid, lifecycleOwner)
            // register loca db observers
            lifecycleOwner.reg.currentState = Lifecycle.State.CREATED
            delay(DEL)
            // LiveData waiting for STARTED
            lifecycleOwner.reg.currentState = Lifecycle.State.STARTED

            // update local game
            game.currentStep = Challenge.Step.VIEW_STORYBOARD
            game.sessionStartMillis = System.currentTimeMillis()
            game.storyDesc = "foasba"
            game.storyTitle = "dfasdf"
            assert(brainyRepo.updateLocalGame(game))
            delay(DEL * 2)

            // find way to check that remote game got updated
            launch {
                assertRemoteGame("updated", game.guid, game)
            }
            delay(DEL * 2)

            // delete local game
            Timber.d("Deleting ${game.guid}")
            assertEquals(1, localDb.gameDAO.delete(game.guid))
            delay(DEL * 2)

            // find way to check that remote game got deleted
            launch {
                assertRemoteGame("deleted", game.guid, null)
            }

            // create new game remotely
            // local db should be updated
            val game2 = egGame.copy(challengeGuid = egChall1.guid, guid = genGuid())
            fireDb.updateRemote(game2)
            delay(DEL)
            val game2_updated = localDb.gameDAO.get(game2.guid)
            assertEquals(game2.copy(fireGuid = game2_updated?.fireGuid), game2_updated)

            // update new game remotely
            val game3 = game2_updated?.copy(currentStep = Challenge.Step.VOTE_IDEA)
            fireDb.updateRemote(game3)
            delay(DEL)
            // local db should be updated
            assertEquals(game3, localDb.gameDAO.get(game3!!.guid))
            lifecycleOwner.reg.currentState = Lifecycle.State.DESTROYED
        }
    }


    fun assertRemoteGame(caller: String, gameGuid: String, target: Game?) {
        remoteDb.getReference(DbFolder.GAMES.path).child(gameGuid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    Timber.d("$caller -> launch-ondata-changed $snapshot")
                    assertEquals(target, snapshot.getValue<Game>())
                }
            })
    }

    fun assertRemoteIdea(ideaFireGuid: String, gameGuid: String, target: Idea) {
        remoteDb.getReference(DbFolder.IDEAS.path)
            .child(gameGuid)
            .child(ideaFireGuid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    Timber.d("Assert remote idea $target")
                    assertEquals(target, snapshot.getValue<Idea>())
                }
            })
    }

}
