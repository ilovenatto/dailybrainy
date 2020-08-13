package org.chenhome.dailybrainy.repo.remote

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chenhome.dailybrainy.repo.local.Challenge
import org.chenhome.dailybrainy.repo.local.Game
import org.chenhome.dailybrainy.repo.local.Idea
import org.chenhome.dailybrainy.repo.local.LocalDb
import timber.log.Timber


/**
 * Singleton facade over Firebase Database. Singleton b/c it contains a set of Firestore
 * entity handlers (which we don't want duplicates of)
 *
 * Used to read/write entities to remote backend
 */
object RemoteDb {
    private val fireDb: FirebaseDatabase =
        Firebase.database // default database for this FirebaseApp

    private var challengeObserver: RemoteChallengeObserver? = null
//    lateinit private var gameObserver: RemoteGameObserver

    // Can be reinitialized for every new game that comes along
    private var ideaObserver: RemoteIdeaObserver? = null

    /**
     * Register handlers if they are not yet registered. Can be called multiple times.
     * Will ignore subsequent calls if handlers already registered.
     *
     * Once handlers are registered, Firebase will begin to listen to changes in remote database
     * and may immediately download remote data.
     *
     * @return whether registration was executed and successful. Else if registration failed or
     * not executed.
     */
    fun registerRemoteObservers(context: Context, lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun register() {
                if (challengeObserver == null) {
                    challengeObserver = RemoteChallengeObserver(context).register()
                }
                // TODO: 8/12/20 observe remote games that we participated in
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun deregister() {
                challengeObserver?.deregister()
            }
        })
    }


    /**
     * Registers observers for the current game. Observers lives tied to Game's lifecycle.
     *
     * @param context
     * @param gameGuid
     * @param gameLifecycleOwner
     */
    fun registerRemoteGameObservers(
        context: Context,
        gameGuid: String,
        gameLifecycleOwner: LifecycleOwner
    ) {
        gameLifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun register() {
                ideaObserver =
                    RemoteIdeaObserver(context, gameGuid).register() as RemoteIdeaObserver
                Timber.d("Registering remote observer ${ideaObserver}")
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun deregister() {
                ideaObserver?.let {
                    it.deregister()
                }
            }
        })
    }

    internal interface Registrable {
        fun register(): Registrable
        fun deregister()
    }


    /**
     * Updates remote database with new state
     * @param game
     */
    fun updateRemote(game: Game?) {
        game?.let {
            Timber.d("Local game has changed. Updating (or inserting) remote game to $it")
            fireDb.getReference(DbFolder.GAMES.path)
                .child(it.guid)
                .setValue(it, DatabaseReference.CompletionListener { error, ref ->
                    error?.let {
                        Timber.w("Unable to update $it to location $ref. Got $error")
                    }
                })
        }
    }

    /**
     * Add ideas to remote db. Mark each idea with its remote guid before pushing to the remote db.
     * Each idea will live in a folder, where the folder name is the game's guid
     *
     * `<firebasedb-root>/ideas/<game guid>/<idea guid>/..idea..`
     *
     * Set the [Idea.fireGuid] field to be the value
     * of the newly pushed remote entity's key, [DatabaseReference.getKey]
     *
     * @param ideas
     */
    fun addRemote(ideas: List<Idea>?) {
        ideas?.forEach { local ->
            val childRef = fireDb.getReference(DbFolder.IDEAS.path)
                .child(local.gameGuid)
                .push()
            local.fireGuid = childRef.key
            Timber.d("Pushing local idea to remote game folder ${local.gameGuid} : $local")
            childRef.setValue(local, DatabaseReference.CompletionListener { error, ref ->
                error?.let {
                    Timber.w("Unable to add local idea $local, got error $error")
                }
            })
        }
    }
}


enum class DbFolder(val path: String) {
    GAMES("games"),
    CHALLENGES("challenges"),
    PLAYERS("players"),
    IDEAS("ideas")
}

// TODO: 8/3/20 write handler for Idea, Player, Game

/**
 * Handler for [Idea]
 */
class RemoteIdeaObserver(context: Context, val gameGuid: String) : RemoteEntityObserver(context) {
    override fun getPath() = DbFolder.IDEAS.path + "/$gameGuid"

    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
        scope.launch {
            snapshot.getValue<Idea>()?.let { handleChanged(it) }
        }
    }

    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
        scope.launch {
            snapshot.getValue<Idea>()?.let { handleAdd(it, snapshot.key) }
        }
    }

    override fun onChildRemoved(snapshot: DataSnapshot) {
        scope.launch {
            snapshot.getValue<Idea>()?.let { handleRemove(it) }
        }
    }

    private suspend fun handleRemove(entity: Idea) {
        withContext(scope.coroutineContext) {
            Timber.d("Removing local idea $entity")
            if (localDb.ideaDAO.delete(entity.guid) != 1) {
                Timber.w("Unable to delete $entity")
            }
        }
    }

    private suspend fun handleAdd(
        entity: Idea,
        fireGuid: String?
    ) {
        withContext(scope.coroutineContext) {
            if (fireGuid != null && localDb.ideaDAO.countByFireGuid(fireGuid) == 0) {
                Timber.d("Inserting new remote Idea to local db, $entity, with fireGuid $fireGuid")
                localDb.ideaDAO.insert(entity)
            } else {
                Timber.d("Encountered idea that's already been inserted $entity with fireGuid $fireGuid")
            }
        }
    }

    private suspend fun handleChanged(entity: Idea) {
        withContext(scope.coroutineContext) {
            val existing = localDb.ideaDAO.get(entity.guid)
            existing?.let {
                if (localDb.ideaDAO.update(entity) == 0) {
                    Timber.d("Change existing local idea, $existing, to new state $entity")
                } else {
                    Timber.w("Unable to update entity to new state $entity")
                }
            }
        }
    }
}

/**
 * Handler for [Challenge]
 */
class RemoteChallengeObserver(val context: Context) : RemoteDb.Registrable, ValueEventListener {
    override fun onCancelled(error: DatabaseError) = Timber.d("Ignoring onCancelled $error")
    override fun onDataChange(snapshot: DataSnapshot) {
        CoroutineScope(Dispatchers.IO).launch {
            snapshot.getValue<Map<String, Challenge>>()?.let {
                val dao = LocalDb.singleton(context).challengeDAO
                it.forEach { entry ->
                    if (dao.insert(entry.value) == 0L) {
                        Timber.w("Unable to insert challenge $entry.value at key ${entry.key} ")
                    } else {
                        Timber.d("Inserting $entry.value")
                    }
                }
            }
        }
    }

    override fun register(): RemoteChallengeObserver {
        Timber.d("Registered ${this.javaClass.name}")
        Firebase.database.getReference(DbFolder.CHALLENGES.path)
            .addListenerForSingleValueEvent(this)
        return this
    }

    override fun deregister() {
        Timber.d("Deregistered ${this.javaClass.name}")
        Firebase.database.getReference(DbFolder.CHALLENGES.path)
            .removeEventListener(this)
    }
}

/**
 * Base class for ChildEventListeners that handle DailyBrainy entities read and writes
 * to the local db. Provides convenience member variables and default interface implementations
 */
abstract class RemoteEntityObserver(context: Context) : RemoteDb.Registrable, ChildEventListener {
    private val firebaseDb: FirebaseDatabase =
        Firebase.database // default database for this FirebaseApp
    val scope = CoroutineScope(Dispatchers.IO)
    val localDb = LocalDb.singleton(context)

    // DatabaseReference to entity being handled.
    private val dbRef: DatabaseReference by lazy {
        firebaseDb.getReference(getPath())
    }

    // @return itself to allow chain calls
    override fun register(): RemoteEntityObserver {
        Timber.d("Adding RemoteObserver ${this.javaClass.name}")
        dbRef.addChildEventListener(this)
        return this
    }

    // @return itself to allow chain calls
    override fun deregister() {
        Timber.d("Removing RemoteObserver ${this.javaClass.name}")
        dbRef.removeEventListener(this)
    }

    /**
     * Abstract methods for subclasses to define entity-specific behavior
     */
    // Firebase database path to this entity
    abstract fun getPath(): String

    // Subclass can override if they want, otherwise they conveninetly can ignore implementing this method
    override fun onCancelled(error: DatabaseError) =
        Timber.w("Ignoring onCancelled with error ${error.message}")

    // Subclass can override if they want, otherwise they conveninetly can ignore implementing this method
    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) =
        Timber.d("Ignoring on child moved")

    // Subclass can override if they want, otherwise they conveninetly can ignore implementing this method
    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) =
        Timber.d("Ignoring on child moved")

    // Subclass can override if they want, otherwise they conveninetly can ignore implementing this method
    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) =
        Timber.d("Ignoring on child moved")

    // Subclass can override if they want, otherwise they conveninetly can ignore implementing this method
    override fun onChildRemoved(snapshot: DataSnapshot) = Timber.d("Ignoring on child moved")
}
