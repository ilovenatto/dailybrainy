package org.chenhome.dailybrainy.repo.helper

import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.DbFolder
import org.chenhome.dailybrainy.repo.Game
import org.chenhome.dailybrainy.repo.PlayerSession
import org.chenhome.dailybrainy.repo.game.GameStub
import org.chenhome.dailybrainy.repo.image.RemoteImage
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Listens for Challenges offered by DailyBrainy in remote database. Challenges
 * will only change if app publishes new challenges (rarely).
 *
 * Lifecycle is dictated by a lifecycle owner. This instance should be added as an
 * observer of a lifecycle.
 */
internal class ChallengeObserver : ValueEventListener, LifecycleObserver {
    private val fireDb = FirebaseDatabase.getInstance()
    private val fireRef = fireDb.getReference(DbFolder.CHALLENGES.path)
    private val remoteImage = RemoteImage()

    /**
     * List of challenges offered by DailyBrainy
     */
    // mutable private challenges
    var _challenges: MutableLiveData<List<Challenge>> = MutableLiveData(listOf())
    val scope = CoroutineScope(Dispatchers.IO)

    override fun onCancelled(error: DatabaseError) {
        Timber.w("onCancelled $error")
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        Timber.d("Challenges data has changed at location ${snapshot.key}")
        scope.launch {
            try {
                snapshot.getValue<Map<String, Challenge>>()?.let {
                    Timber.d("${it.size} challenges encountered. Replacing all existing challenges.")
                    // calling [MutableLiveData.value] will inform observers of the data change
                    _challenges.postValue(it.map { entry ->
                        decorateWithUri(entry.value)
                    })
                } ?: Timber.w("No challenges found in snapshot ${snapshot.key}")
            } catch (ex: Exception) {
                Timber.e("Unable to observe challenges $ex")
            }
        }

    }

    private suspend fun decorateWithUri(challenge: Challenge): Challenge {
        // get download URI for this challenge
        if (challenge.imgFn.isNotEmpty()) {
            remoteImage.getValidStorageRef(challenge.imgFn)?.let { storageRef ->
                val uri = suspendCoroutine<Uri?> { cont ->
                    storageRef.downloadUrl.addOnSuccessListener {
                        cont.resume(it)
                    }
                    storageRef.downloadUrl.addOnFailureListener {
                        cont.resume(null)
                    }
                }
                return challenge.copy(imageUri = uri)
            } ?: Timber.w("No storage ref found for imgFn ${challenge.imgFn}")
        }
        return challenge
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun register() = fireRef.addValueEventListener(this)

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun deregister() = fireRef.removeEventListener(this)
}

/**
 * Listens for new games in /playersessions/.. and look for
 * playersessions in /playersession/<new game guid>/<session that started by [userGuid]>.
 *
 * Add these new sessions to [_gameStubs] and expose.
 *
 * Lifecycle is dictated by a lifecycle owner. This instance should be added as an
 * observer of a lifecycle.
 */

internal class GameStubObserver(private val userGuid: String) : ValueEventListener,
    LifecycleObserver {

    private val fireDb = FirebaseDatabase.getInstance()
    private val fireRef = fireDb
        .getReference(DbFolder.PLAYERSESSION.path)
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * List of Games that this user has participated in.
     */
    var _gameStubs: MutableLiveData<List<GameStub>> = MutableLiveData(listOf())

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun register() = fireRef.addValueEventListener(this)

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun deregister() = fireRef.removeEventListener(this)

    override fun onCancelled(error: DatabaseError) = Timber.d(error.message)
    override fun onDataChange(snapshot: DataSnapshot) {
        // Newly updated values from remote db
        var updatedGameStubs: List<GameStub>
        scope.launch {
            try {
                var gameMap: Map<String, Game> = mutableMapOf()

                // Get all the games and cache in memory. They'll be referred
                // to later when matching PlayerSessions to their Game
                suspendCoroutine<Unit> { cont ->
                    fireDb.getReference(DbFolder.GAMES.path)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onCancelled(error: DatabaseError) =
                                Timber.d(error.message)

                            override fun onDataChange(snapshot: DataSnapshot) {
                                gameMap =
                                    snapshot.getValue<Map<String, Game>>() ?: mutableMapOf()
                                cont.resume(Unit)
                            }
                        })
                }
                Timber.d("Obtained Game cache with ${gameMap.size} elements")

                // Returns a map of <gameGuid> -> Map<SessionGuid, Session>
                //
                // /playersession/<gameGuid>/<sessionGuid>/session
                //
                snapshot.getValue<Map<String, Map<String, PlayerSession>>>()?.let { orig ->
                    // for each game, look at each session and add any session where session.playerGuid == userGuid
                    updatedGameStubs = orig.flatMap { it.value.values }
                        // Reduce to sessions this user has participated in
                        .filter {
                            it.userGuid == userGuid
                        }
                        // Transform to a list of GameStubs
                        .mapNotNull { session ->
                            gameMap[session.gameGuid]?.let { game ->
                                GameStub(game, session)
                            }
                        }

                    // Call on UI thread b/c _gameStubs.setValue can not be called on background thread
                    withContext(Dispatchers.Main) {
                        // calling [MutableLiveData.value] will notify observers of the LiveData
                        Timber.d("Found ${updatedGameStubs.size} sessions that user $userGuid participated in")
                        _gameStubs.value = updatedGameStubs
                    }

                } ?: handleNoSessions()
            } catch (ex: Exception) {
                Timber.e("Unable to process game stubs $ex")
            }
        }
    }

    private suspend fun handleNoSessions() {
        withContext(Dispatchers.Main) {
            Timber.w("No remote player sessions found. Nuking all game stubs locally")
            _gameStubs.value = listOf()
        }
    }
}

