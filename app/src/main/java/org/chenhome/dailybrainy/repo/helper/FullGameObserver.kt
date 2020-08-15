package org.chenhome.dailybrainy.repo.helper

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.chenhome.dailybrainy.repo.*
import timber.log.Timber

/**
 * Observes remote game, ideas and playersessions for changes.
 */
internal class FullGameObserver(val gameGuid: String, val context: Context) {

    /**
     * Expose [FullGame] data to be used by clients
     */
    var _fullGame: MutableLiveData<FullGame> =
        MutableLiveData(FullGame())

    // TODO: 8/14/20 inject BrainyRepo, UserRepo
    private val userRepo = UserRepo(context)
    private val brainyRepo = BrainyRepo.singleton(context)
    private val fireDb =
        FirebaseDatabase.getInstance()
    private val scope =
        CoroutineScope(Dispatchers.IO)

    fun register() {
        Timber.d("Registering FullGameObserver")
        this.GameObserver().register()
        this.IdeaObserver().register()
        this.PlayerSessionObserver().register()
    }

    fun deregister() {
        Timber.d("Deregistering FullGameObserver")
        this.GameObserver().deregister()
        this.IdeaObserver().deregister()
        this.PlayerSessionObserver().deregister()
    }

    /**
     * Observes /games/<gameGuid> for changes
     */
    inner class GameObserver : ValueEventListener {
        private val fireRef = fireDb.getReference(DbFolder.GAMES.path)
            .child(gameGuid)

        override fun onCancelled(error: DatabaseError) =
            Timber.d("$error")

        override fun onDataChange(snapshot: DataSnapshot) {
            snapshot.getValue<Game>()?.let { game ->
                Timber.d("Game $gameGuid changed to $game. Notifying observers/")
                _fullGame.value?.game = game

                // Set challenge
                brainyRepo.challenges.value?.firstOrNull {
                    it.guid == game.challengeGuid
                }?.let { challenge ->
                    _fullGame.value?.challenge = challenge
                }
                _fullGame.notifyObserver()
            }
        }

        fun register() = fireRef.addValueEventListener(this)
        fun deregister() = fireRef.removeEventListener(this)
    }

    /**
     * Observes /ideas/<gameGuid> for child-related changes
     */
    inner class IdeaObserver : ChildEventListener {
        private val fireRef = fireDb.getReference(DbFolder.IDEAS.path)
            .child(gameGuid)

        fun register() = fireRef.addChildEventListener(this)
        fun deregister() = fireRef.removeEventListener(this)

        override fun onCancelled(error: DatabaseError) =
            Timber.d(error.message)

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) =
            Timber.d("${snapshot.key}")

        // Add to [FullGame] instance
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            snapshot.getValue<Idea>()?.let { added ->
                // only add item if it's not already in list
                _fullGame.value?.ideas?.firstOrNull { added.guid == it.guid } ?: run {
                    Timber.d("Adding idea $added to list of size ${_fullGame.value?.ideas?.size}")
                    _fullGame.value?.ideas?.add(added)
                    _fullGame.notifyObserver()
                }
            }
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            snapshot.getValue<Idea>()?.let { changed ->
                // find existing idea in list
                _fullGame.value?.ideas
                    ?.firstOrNull { changed.guid == it.guid }
                    ?.let { match ->
                        Timber.d("Replacing $match with $changed")
                        _fullGame.value?.ideas?.remove(match)
                        _fullGame.value?.ideas?.add(changed)
                        _fullGame.notifyObserver()
                    }
            }
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            // should never happen b/c app is not designed for any user to remove ideas
            // but including it in case it does (or if testing requires it)
            snapshot.getValue<Idea>()?.let {
                Timber.d("Removing idea $it")
                _fullGame.value?.ideas?.remove(it)
                _fullGame.notifyObserver()
            }
        }

        fun insert(idea: Idea) {
            _fullGame.value?.ideas?.firstOrNull { idea.guid == it.guid } ?: run {
                _fullGame.value?.ideas?.add(idea)
                _fullGame.notifyObserver()

                // Check that it doesn't already exist
                fireRef.child(idea.guid)
                    .addListenerForSingleValueEvent(object :
                        ValueEventListener {
                        override fun onCancelled(error: DatabaseError) =
                            Timber.d(error.message)

                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.getValue<Idea>() ?: run {
                                // Add remotely to /ideas/<gameGuid>/<new idea>
                                Timber.d("Adding idea $idea to remote location ${snapshot.key}")
                                fireRef.push().setValue(idea)
                            }
                        }
                    })
            }
        }
    }


    /**
     * Observes /playersessions/<gameGuid> for child-related changes
     */
    inner class PlayerSessionObserver :
        ChildEventListener {
        private val fireRef = fireDb.getReference(DbFolder.PLAYERSESSION.path)
            .child(gameGuid)

        fun register() = fireRef.addChildEventListener(this)
        fun deregister() = fireRef.removeEventListener(this)

        override fun onCancelled(error: DatabaseError) =
            Timber.d(error.message)

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) =
            Timber.d("${snapshot.key}")

        // Add to [FullGame] instance
        // Ignore any duplicate sessions already in [FullGame]
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            snapshot.getValue<PlayerSession>()?.let { added ->
                _fullGame.value?.players?.firstOrNull { added.guid == it.guid } ?: run {
                    Timber.d("Adding playersession $added to list of size ${_fullGame.value?.players?.size}")
                    _fullGame.value?.players?.add(added)
                    _fullGame.notifyObserver()
                }
            }
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            snapshot.getValue<PlayerSession>()?.let { changed ->
                // find existing playersession in list
                _fullGame.value?.players
                    ?.firstOrNull { changed.guid == it.guid }
                    ?.let { match ->
                        Timber.d("Replacing $match with $changed")
                        _fullGame.value?.players?.remove(match)
                        _fullGame.value?.players?.add(changed)
                        _fullGame.notifyObserver()
                    }
            }
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            snapshot.getValue<PlayerSession>()?.let {
                Timber.d("Removing playersession $it")
                _fullGame.value?.players?.remove(it)
                _fullGame.notifyObserver()
            }
        }

        /**
         * Insert session into [FullGame] instance and update remote db.
         *
         * @param playerSession
         */
        fun insert(playerSession: PlayerSession) {
            // Add to fullGame
            _fullGame.value?.players?.firstOrNull { it.guid == playerSession.guid } ?: run {
                _fullGame.value?.players?.add(playerSession)
                _fullGame.notifyObserver()
            }

            scope.launch {
                // Add child to /playersessions/<gameGuid>/
                fireRef.child(playerSession.guid)
                    // check /playersessions/<gameGuid>/<playerGuid> doesn't already exist
                    .addListenerForSingleValueEvent(object :
                        ValueEventListener {
                        override fun onCancelled(error: DatabaseError) =
                            Timber.d(error.message)

                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.getValue<PlayerSession>() ?: run {
                                Timber.d("Inserting player session $playerSession for game $gameGuid")
                                fireRef.push().setValue(playerSession)
                            }
                        }
                    })
            }
        }


    }
}