package org.chenhome.dailybrainy

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import androidx.test.runner.AndroidJUnitRunner
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.chenhome.dailybrainy.repo.*
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


// Lifecycle fixture
class TestLifecycleOwner : LifecycleOwner {
    val reg = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = reg
}


/**
 * Helper function to observe LiveData response. Used
 * for unit tests.
 */
fun <T> LiveData<T>.blockingObserve(): T? {
    var value: T? = null
    val latch = CountDownLatch(1)

    val observer = Observer<T> { t ->
        value = t
        latch.countDown()
    }

    observeForever(observer)

    latch.await(2, TimeUnit.SECONDS)
    return value
}

// A custom runner to set up the instrumented application class for tests.
class CustomTestRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        Timber.plant(Timber.DebugTree())
        Timber.d("Starting CustomTestRunner")
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}

fun createFullGame(context: Context) {
    val userId = UserRepo(context).currentPlayerGuid
    val fireDb = FirebaseDatabase.getInstance()

    runBlocking {
        lateinit var challenge: Challenge
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

        lateinit var game: Game
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
            val idea = Idea(
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
            val session = PlayerSession(
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