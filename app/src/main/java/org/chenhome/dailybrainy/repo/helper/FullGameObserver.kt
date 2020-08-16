package org.chenhome.dailybrainy.repo.helper

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    // TODO: 8/15/20 inject brainyrepo
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
                Timber.d("Remote game $gameGuid changed to $game.")
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


        fun updateRemote(game: Game) {
            if (game.guid.isNotEmpty()
                && game.guid == gameGuid
            ) {
                // Update remotely at /game/<gameGuid>
                val update = fireRef.child(game.guid)
                // check that it's there
                update.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) = Timber.d("$error")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.getValue<Game>()?.let {
                            update.setValue(game) { error, ref ->
                                error?.let {
                                    Timber.w("Unable to update game at $ref")
                                } ?: Timber.d("Updated game ${game.guid} at $ref")
                            }
                        }
                            ?: Timber.w("Unable to update a non-existent game, ${game.guid} at $update")
                    }
                })
            }
        }
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
                        Timber.d("Remote copy changed. Replacing local ${match.guid} with remote ${changed.guid}")
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

        /**
         * @param idea The [Idea.guid] will not be set for locally created Ideas. So just blindly add to
         * remote database.
         */
        fun insertRemote(idea: Idea) {
            // Add remotely to /ideas/<gameGuid>/<new idea>
            val created = fireRef.push()
            created.key?.let {
                val copy = idea.copy(guid = created.key!!)
                created.setValue(copy) { error, ref ->
                    error?.let {
                        Timber.w("Unable to add to $ref, idea $copy, got $error")
                    } ?: Timber.d("Inserted idea ${copy.guid} at $ref")
                }
            } ?: Timber.w("Unable to insert idea to location ${created}")
        }

        fun updateRemote(idea: Idea) {
            if (idea.guid.isNotEmpty()
                && idea.gameGuid == gameGuid
            ) {
                // Update remotely at /ideas/<gameGuid>/<idea guid>
                val update = fireRef.child(idea.guid)
                // check that it's there
                update.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) = Timber.d("$error")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.getValue<Idea>()?.let {
                            update.setValue(idea) { error, ref ->
                                error?.let {
                                    Timber.w("Unable to update idea at $ref")
                                } ?: Timber.d("Updated session ${idea.guid} at $ref")
                            }
                        }
                            ?: Timber.w("Unable to update a non-existent idea, ${idea.guid} at $update")
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
                        Timber.d("Remote session ${changed.guid} changed. Replacing local version")
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
         * @param session local playerSession where [PlayerSession.guid] is not set
         */
        fun insertRemote(session: PlayerSession) {
            // Add remotely to /playersessions/<gameGuid>/<new session>
            val created = fireRef.push()
            created.key?.let {
                val copy = session.copy(guid = created.key!!)
                created.setValue(copy) { error, ref ->
                    error?.let {
                        Timber.w("Unable to add to $ref, session $copy. Got $error")
                    } ?: Timber.d("Inserting session at $ref")
                }
            } ?: Timber.w("Unable to insert session to location ${created}")
        }

        /**
         * Updates remote copy of this [PlayerSession]
         *
         * @param player
         */
        fun updateRemote(player: PlayerSession) {
            if (player.guid.isNotEmpty()
                && player.gameGuid == gameGuid
            ) {
                // Update remotely at /playersessions/<gameGuid>/<session guid>
                val update = fireRef.child(player.guid)
                // check that it's there
                update.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) = Timber.d("$error")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.getValue<PlayerSession>()?.let {
                            update.setValue(player) { error, ref ->
                                error?.let {
                                    Timber.w("Unable to update session at $ref")
                                } ?: Timber.d("Updated session ${player.guid} at $ref")
                            }
                        }
                            ?: Timber.w("Unable to update a non-existent session, ${player.guid} at $update")
                    }
                })
            }
        }
    }


}