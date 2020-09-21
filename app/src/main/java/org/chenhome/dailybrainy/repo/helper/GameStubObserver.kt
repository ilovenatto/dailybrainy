package org.chenhome.dailybrainy.repo.helper

import androidx.lifecycle.*
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


    private var _gameStubs: MutableLiveData<List<GameStub>> = MutableLiveData(listOf())

    /**
     * Private cache of challenges. Used to set challenge info on GameStubs
     */
    private var guid2Challenge = mapOf<String, Challenge>()

    /**
     * List of Games that this user has participated in.
     */
    val myGameStubs = Transformations.map(_gameStubs) {
        it.filter { stub ->
            stub.playerSession.userGuid == userGuid
        }
    }

    /**
     * List of all games from all users
     */
    val allGameStubs = _gameStubs


    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun register() {
        // First get all the challenges, then get the GameStubs. These challenges are generally static and
        // wont' change for life of the app process.
        scope.launch {
            suspendCoroutine<Unit> { cont ->
                challRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Map of challegeGuid -> Challenge
                        snapshot.getValue<Map<String, Challenge>>()?.let {
                            Timber.d("${it.size} challenges encountered")
                            guid2Challenge = it
                            cont.resume(Unit)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) = cont.resume(Unit)
                })
            }
            guid2Challenge.values.forEach { chall ->
                chall.imageUri = remoteImage.getImageUri(chall.imgFn)
            }

            // Then get GameStubs
            // /playersession/<game guid>/all
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

    /**
     * Listen for changes on /playersession/<all games>
     *
     * @param snapshot
     */
    override fun onDataChange(snapshot: DataSnapshot) {
        // Newly updated values from remote db
        var updatedGameStubs: List<GameStub>
        var allGameStubs: List<GameStub>
        scope.launch {
            try {
                // Map of <gameGuid, Game>
                var gameMap: Map<String, Game> = mutableMapOf()

                // Get all the games and cache in memory. They'll be referred
                // to later when matching PlayerSessions to their Game
                suspendCoroutine<Unit> { cont ->

                    // /game/<game guid>/<game>
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
                Timber.d("Obtained Challenge cache with ${guid2Challenge.keys}")

                // getValue() returns a map of <gameGuid> -> Map<SessionGuid, Session>
                //
                // /playersession/<gameGuid>/<sessionGuid>/session
                //
                snapshot.getValue<Map<String, Map<String, PlayerSession>>>()?.let { orig ->
                    allGameStubs = orig.flatMap { it.value.values }
                        // Transform to a list of GameStubs
                        .mapNotNull { session ->
                            gameMap[session.gameGuid]?.let { game ->
                                var stub = GameStub(game, session)
                                guid2Challenge[game.challengeGuid]?.let {
                                    stub.challenge = it
                                }
                                stub
                            }
                        }

                    // Call on UI thread b/c _gameStubs.setValue can not be called on background thread
                    withContext(Dispatchers.Main) {
                        // calling [MutableLiveData.value] will notify observers of the LiveData
                        Timber.d("Found ${allGameStubs.size} games overall")
                        _gameStubs.value = allGameStubs
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

