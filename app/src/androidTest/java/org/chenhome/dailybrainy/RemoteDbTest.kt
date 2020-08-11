package org.chenhome.dailybrainy

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.chenhome.dailybrainy.repo.BrainyRepo
import org.chenhome.dailybrainy.repo.local.Challenge
import org.chenhome.dailybrainy.repo.local.Idea
import org.chenhome.dailybrainy.repo.local.LocalDb
import org.chenhome.dailybrainy.repo.local.genGuid
import org.chenhome.dailybrainy.repo.remote.DbFolder
import org.chenhome.dailybrainy.repo.remote.RemoteDb
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
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

    @Before
    fun before() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        fireDb = RemoteDb
        localDb = LocalDb.singleton(appContext)
        remoteDb = Firebase.database
        brainyRepo = BrainyRepo.singleton(appContext)

        localDb.clearAllTables()
        localDb.challengeDAO.insert(egChall1)
    }

    @After
    fun after() {
        localDb.clearAllTables()
    }

    @Test
    fun testAdd() {
        val c1 = egChall1
        runBlocking {
            val game = brainyRepo.insertLocalGame(c1.guid)
            brainyRepo.registerGameObservers(game?.guid!!, ProcessLifecycleOwner.get())

            delay(3000)
            assertEquals(6, localDb.challengeDAO.getAll().size)

            // add new remote idea
            val idea1 =
                egIdea.copy(gameGuid = game.guid, guid = genGuid(), origin = Idea.Origin.BRAINSTORM)
            fireDb.addRemote(listOf(idea1))

            // remote observer should have tried to insert remote entity in local db
            delay(3000)
            assertEquals(
                1, localDb.ideaDAO
                    .getByOriginLive(game.guid, Idea.Origin.BRAINSTORM).blockingObserve()?.size
            )

            // remove remobte idea
            // remote observer should have honored that and removed from local db
            val removeRef = remoteDb.getReference(DbFolder.IDEAS.path)
                .child(idea1.gameGuid)
                .child(idea1.fireGuid!!)
            Timber.d("Removing ${idea1.guid}, ${removeRef.path}")
            removeRef.setValue(null)

            delay(3000)
            assertEquals(
                0, localDb.ideaDAO
                    .getByOriginLive(game.guid, Idea.Origin.BRAINSTORM).blockingObserve()?.size
            )

            assertEquals(game, localDb.gameDAO.get(game.guid))

            // update local game
            game.currentStep = Challenge.Step.VIEW_STORYBOARD
            game.sessionStartMillis = System.currentTimeMillis()
            game.storyDesc = "foasba"
            game.storyTitle = "dfasdf"
            Timber.d("updating game $game")
            assert(brainyRepo.updateLocalGame(game))
            delay(5000)
            Timber.d("finished waiting")
        }
    }

}
