package org.chenhome.dailybrainy.repo

import android.content.Context
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chenhome.dailybrainy.repo.local.BrainyDb
import org.chenhome.dailybrainy.repo.local.Challenge
import timber.log.Timber


/**
 * Facade over Firebase Database.
 *
 * Singleton. On instantiation, it
 * Used to read/write entities to remote backend
 */
class FireDatabaseRepo(
    val context: Context
) {
    val firebaseDb: FirebaseDatabase = Firebase.database // default database for this FirebaseApp
    val localDb: BrainyDb = BrainyDb.getDb(context)
    val scope = CoroutineScope(Dispatchers.IO)

    // Entity handlers
    val entityHandlers: List<EntityHandler> = mutableListOf()

    /**
     * Register handlers if they are not yet registered. Can be called multiple times.
     * Will ignore subsequent calls if handlers already registered.
     *
     * @return whether registration was executed and successful. Else if registration failed or
     * not executed.
     */
    fun registerHandlers(): Boolean {
        if (entityHandlers.isNotEmpty()) {
            Timber.w("Entity handlers already registered. Ignoring this call")
            return false
        }

        entityHandlers.plus(ChallengeHandler().register())
        return true
    }

    fun deregisterHandlers() {
        entityHandlers.forEach {
            it.deregister()
        }
    }

    // TODO: 8/3/20 write handler for Idea, Player, Game
    inner class ChallengeHandler : EntityHandler() {
        override fun getPath() = "challenges"
        override fun isExistInDb(guid: String): Boolean =
            localDb.challengeDAO.get(guid) == null

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            scope.launch {
                snapshot.getValue<Challenge>()?.let { handleChanged(it) }
            }
        }

        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            scope.launch {
                snapshot.getValue<Challenge>()?.let { handleAdd(it) }
            }
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            scope.launch {
                snapshot.getValue<Challenge>()?.let { handleRemove(it) }
            }
        }

        private suspend fun handleRemove(entity: Challenge) {
            withContext(scope.coroutineContext) {
                if (localDb.challengeDAO.delete(entity.guid) != 1) {
                    Timber.w("Unable to delete $entity")
                }
            }
        }

        private suspend fun handleAdd(entity: Challenge) {
            withContext(scope.coroutineContext) {
                if (isExistInDb(entity.guid)) {
                    localDb.challengeDAO.delete(entity.guid)
                }
                localDb.challengeDAO.insert(entity)
            }
        }

        private suspend fun handleChanged(entity: Challenge) {
            withContext(scope.coroutineContext) {
                if (isExistInDb(entity.guid)) {
                    localDb.challengeDAO.update(entity)
                }
            }
        }
    }

    abstract inner class EntityHandler : ChildEventListener {
        // DatabaseReference to entity being handled.
        val dbRef: DatabaseReference = firebaseDb.getReference(getPath())

        // @return itself to allow chain calls
        fun register(): EntityHandler {
            Timber.d("Adding listener $this")
            dbRef.addChildEventListener(this)
            return this
        }

        // @return itself to allow chain calls
        fun deregister(): EntityHandler {
            dbRef.removeEventListener(this)
            return this
        }

        /**
         * Abstract methods for subclasses to define entity-specific behavior
         */
        // Firebase database path to this entity
        abstract fun getPath(): String

        // @return whether entity with guid already exists in local db or not
        abstract fun isExistInDb(guid: String): Boolean

        // Ignoring
        override fun onCancelled(error: DatabaseError) {
            Timber.w("Handler cancelled with error ${error.message}")
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            Timber.d("Ignoring on child moved")
        }

    }
}