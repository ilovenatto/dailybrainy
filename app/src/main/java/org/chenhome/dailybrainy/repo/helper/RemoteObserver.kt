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
import java.util.*
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
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * List of challenges offered by DailyBrainy
     */
    // mutable private challenges
    var _challenges: MutableLiveData<List<Challenge>> = MutableLiveData(listOf())
    var _lessons: MutableLiveData<List<Challenge>> = MutableLiveData(listOf())

    var _todayChallenge: MutableLiveData<Challenge> = MutableLiveData()
    var _todayLesson: MutableLiveData<Challenge> = MutableLiveData()

    override fun onCancelled(error: DatabaseError) {
        Timber.w("onCancelled $error")
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        Timber.d("Challenges data has changed at location ${snapshot.key}")
        scope.launch {
            try {
                // Map of challegeGuid -> Challenge
                snapshot.getValue<Map<String, Challenge>>()?.let {
                    Timber.d("${it.size} challenges encountered. Replacing all existing challenges.")

                    var lessons = mutableListOf<Challenge>()
                    var challenges = mutableListOf<Challenge>()
                    it.forEach { entry ->
                        val chall = entry.value

                        decorateWithUri(remoteImage, chall)
                        when (chall.category) {
                            Challenge.Category.LESSON -> lessons.add(chall)
                            Challenge.Category.CHALLENGE -> challenges.add(chall)
                        }
                    }

                    _lessons.postValue(lessons)
                    _challenges.postValue(challenges)

                    if (challenges.isNotEmpty()) {
                        val index =
                            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) % challenges.size
                        if (index >= 0 && index < challenges.size) {
                            val todayChallenge = challenges[index]
                            Timber.d("Today's challenge is $todayChallenge")
                            _todayChallenge.postValue(todayChallenge)
                        }
                    }

                    if (lessons.isNotEmpty()) {
                        val index =
                            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) % lessons.size
                        if (index >= 0 && index < lessons.size) {
                            val todayLesson = lessons[index]
                            Timber.d("Today's lessons is $todayLesson")
                            _todayLesson.postValue(todayLesson)
                        }
                    }


                } ?: Timber.w("No challenges found in snapshot ${snapshot.key}")
            } catch (ex: Exception) {
                Timber.e("Unable to observe challenges $ex")
            }
        }

    }


    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun register() = fireRef.addValueEventListener(this)

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun refresh() = _challenges.notifyObserver()

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun deregister() = fireRef.removeEventListener(this)
}


internal suspend fun decorateWithUri(remoteImage: RemoteImage, challenge: Challenge) {
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
            challenge.imageUri = uri
        } ?: Timber.w("No storage ref found for imgFn ${challenge.imgFn}")
    }
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

    private val remoteImage = RemoteImage()

    private val fireDb = FirebaseDatabase.getInstance()
    private val fireRef = fireDb
        .getReference(DbFolder.PLAYERSESSION.path)
    private val challRef = fireDb.getReference(DbFolder.CHALLENGES.path)
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * List of Games that this user has participated in.
     */
    var _gameStubs: MutableLiveData<List<GameStub>> = MutableLiveData(listOf())
    var _allGameStubs: MutableLiveData<List<GameStub>> = MutableLiveData(listOf())

    /**
     * All challenges.
     */
    private var _guid2Challenge = mapOf<String, Challenge>()


    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun register() {
        // First get all the challenges, then get the GameStubs
        scope.launch {
            suspendCoroutine<Unit> { cont ->
                challRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Map of challegeGuid -> Challenge
                        snapshot.getValue<Map<String, Challenge>>()?.let {
                            Timber.d("${it.size} challenges encountered")
                            _guid2Challenge = it
                            cont.resume(Unit)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) = cont.resume(Unit)
                })
            }
            _guid2Challenge.values.forEach {
                decorateWithUri(remoteImage, it)
            }
            fireRef.addValueEventListener(this@GameStubObserver)
        }

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun refresh() = _gameStubs.notifyObserver()

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun deregister() {
        fireRef.removeEventListener(this)

    }

    override fun onCancelled(error: DatabaseError) = Timber.d(error.message)
    override fun onDataChange(snapshot: DataSnapshot) {
        // Newly updated values from remote db
        var updatedGameStubs: List<GameStub>
        var allGameStubs: List<GameStub>
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
                Timber.d("Obtained Challenge cache with ${_guid2Challenge.keys}")

                // Returns a map of <gameGuid> -> Map<SessionGuid, Session>
                //
                // /playersession/<gameGuid>/<sessionGuid>/session
                //
                snapshot.getValue<Map<String, Map<String, PlayerSession>>>()?.let { orig ->
                    val f = orig.flatMap { it.value.values }
                    allGameStubs = orig.flatMap { it.value.values }
                        // Transform to a list of GameStubs
                        .mapNotNull { session ->
                            gameMap[session.gameGuid]?.let { game ->
                                var stub = GameStub(game, session)
                                _guid2Challenge[game.challengeGuid]?.let {
                                    stub.challenge = it
                                }
                                stub
                            }
                        }

                    // for each game, look at each session and add any session where session.playerGuid == userGuid
                    updatedGameStubs = orig.flatMap { it.value.values }
                        // Reduce to sessions this user has participated in
                        .filter {
                            it.userGuid == userGuid
                        }
                        // TODO: 9/20/20 make this cleaner. merge into one loop with above loop 
                        // Transform to a list of GameStubs
                        .mapNotNull { session ->
                            gameMap[session.gameGuid]?.let { game ->
                                var stub = GameStub(game, session)
                                _guid2Challenge[game.challengeGuid]?.let {
                                    stub.challenge = it
                                }
                                stub
                            }
                        }

                    // Call on UI thread b/c _gameStubs.setValue can not be called on background thread
                    withContext(Dispatchers.Main) {
                        // calling [MutableLiveData.value] will notify observers of the LiveData
                        Timber.d("Found ${updatedGameStubs.size} games that user $userGuid participated in and ${allGameStubs.size} games overall")
                        _gameStubs.value = updatedGameStubs
                        _allGameStubs.value = allGameStubs
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

