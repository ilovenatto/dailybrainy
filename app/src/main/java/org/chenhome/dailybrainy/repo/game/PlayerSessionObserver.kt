package org.chenhome.dailybrainy.repo.game

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import org.chenhome.dailybrainy.repo.DbFolder
import org.chenhome.dailybrainy.repo.PlayerSession
import org.chenhome.dailybrainy.repo.helper.notifyObserver
import timber.log.Timber

/**
 * Observes /playersessions/<gameGuid> for child-related changes
 */
class PlayerSessionObserver(
    val context: Context,
    val gameGuid: String,
    val fullGame: MutableLiveData<FullGame>,
) :
    ChildEventListener {
    private val fireDb = FirebaseDatabase.getInstance()
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
        try {
            snapshot.getValue<PlayerSession>()?.let { added ->
                fullGame.value?.players?.firstOrNull { added.guid == it.guid } ?: run {
                    Timber.d("Adding playersession $added to list of size ${fullGame.value?.players?.size}")
                    fullGame.value?.players?.add(added)
                    fullGame.notifyObserver()
                }
            }
        } catch (e: Exception) {
            Timber.e("Unable to add player $fireRef, $e")
        }

    }

    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
        try {
            snapshot.getValue<PlayerSession>()?.let { changed ->
                // find existing playersession in list
                fullGame.value?.players
                    ?.indexOfFirst { changed.guid == it.guid }
                    ?.let { index ->
                        if (index >= 0) {
                            Timber.d("Remote session ${changed.guid} changed. Replacing local version")
                            fullGame.value?.players?.set(index, changed)
                            fullGame.notifyObserver()
                        }
                    }
            }
        } catch (e: Exception) {
            Timber.e("Unable to modify player $fireRef, $e")
        }

    }

    override fun onChildRemoved(snapshot: DataSnapshot) {
        try {
            snapshot.getValue<PlayerSession>()?.let {
                Timber.d("Removing playersession $it")
                fullGame.value?.players?.remove(it)
                fullGame.notifyObserver()
            }
        } catch (e: Exception) {
            Timber.e("Unable to remove player $fireRef, $e")
        }
    }

    /**
     * Insert session into [FullGame] instance and update remote db.
     *
     * @param session local playerSession where [PlayerSession.guid] is not set
     */
    fun insertRemote(session: PlayerSession) {
        // Add remotely to /playersessions/<gameGuid>/<new session>
        try {
            val created = fireRef.push()
            created.key?.let {
                val copy = session.copy(guid = created.key!!)
                created.setValue(copy) { error, ref ->
                    error?.let {
                        Timber.w("Unable to add to $ref, session $copy. Got $error")
                    } ?: Timber.d("Inserting session at $ref")
                }
            } ?: Timber.w("Unable to insert session to location $created")
        } catch (e: Exception) {
            Timber.e("Unable to insert player $session, $e")
        }
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
            try {
                update.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) = Timber.d("$error")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            update.setValue(player) { error, ref ->
                                error?.let {
                                    Timber.w("Unable to update session at $ref")
                                } ?: Timber.d("Updated session ${player.guid} at $ref")
                            }
                        } else {
                            Timber.w("Unable to update a non-existent session, ${player.guid} at $update")
                        }
                    }
                })
            } catch (e: Exception) {
                Timber.w("Unable to update player $player, $e")
            }
        }
    }
}