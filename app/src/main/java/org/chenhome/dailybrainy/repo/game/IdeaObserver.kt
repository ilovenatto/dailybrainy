package org.chenhome.dailybrainy.repo.game

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import org.chenhome.dailybrainy.repo.DbFolder
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.helper.notifyObserver
import timber.log.Timber

/**
 * Observes /ideas/<gameGuid> for child-related changes
 */
class IdeaObserver(
    val context: Context,
    val gameGuid: String,
    val fullGame: MutableLiveData<FullGame>,
) : ChildEventListener {

    private val fireDb = FirebaseDatabase.getInstance()
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
        try {
            snapshot.getValue<Idea>()?.let { added ->
                // only add item if it's not already in list
                var ideaList = if (added.isSketch()) fullGame.value?.sketches else
                    fullGame.value?.ideas
                ideaList?.firstOrNull { added.guid == it.guid } ?: ideaList?.let {
                    Timber.d("Adding idea $added to list of size ${it.size}")
                    it.add(added)
                    fullGame.notifyObserver()
                } ?: Timber.w("Null list. Unable to add idea to it")
            }
        } catch (e: Exception) {
            Timber.e("Unable to add idea $fireRef, $e")
        }
    }

    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
        try {
            snapshot.getValue<Idea>()?.let { changed ->
                // find existing idea in list
                var ideaList = if (changed.isSketch()) fullGame.value?.sketches else
                    fullGame.value?.ideas
                ideaList?.indexOfFirst { changed.guid == it.guid }
                    ?.let { index ->
                        if (index >= 0) {
                            Timber.d("Remote copy changed. Replacing with remote ${changed.guid}")
                            ideaList.set(index, changed)
                            fullGame.notifyObserver()
                        }
                    }
            }
        } catch (e: Exception) {
            Timber.e("Unable to modify idea $fireRef, $e")
        }
    }

    override fun onChildRemoved(snapshot: DataSnapshot) {
        // should never happen b/c app is not designed for any user to remove ideas
        // but including it in case it does (or if testing requires it)
        try {
            snapshot.getValue<Idea>()?.let {
                Timber.d("Removing idea $it")
                fullGame.value?.ideas?.remove(it)
                fullGame.value?.sketches?.remove(it)
                fullGame.notifyObserver()
            }
        } catch (e: Exception) {
            Timber.e("Unable to remove idea $fireRef, $e")
        }
    }

    /**
     * @param idea The [Idea.guid] will not be set for locally created Ideas. So just blindly add to
     * remote database.
     */
    fun insertRemote(idea: Idea) {
        // Add name to Idea
        val namedIdea = addNameToCopy(idea)

        // Add remotely to /ideas/<gameGuid>/<new idea>
        try {
            val created = fireRef.push()
            created.key?.let {
                val copy = namedIdea.copy(guid = created.key!!)
                created.setValue(copy) { error, ref ->
                    error?.let {
                        Timber.w("Unable to add to $ref, idea $copy, got $error")
                    } ?: Timber.d("Inserted idea ${copy.guid} at $ref")
                }
            } ?: Timber.w("Unable to insert idea to location $created")
        } catch (e: Exception) {
            Timber.e("Unable to insert idea $fireRef, $e")
        }
    }

    private fun addNameToCopy(idea: Idea): Idea {
        Timber.d("Got idea $idea and players ${fullGame.value?.players}")
        val copy = idea.copy(playerName = fullGame.value?.players?.firstOrNull { session ->
            session.userGuid == idea.playerGuid
        }?.name)
        Timber.d("Added name ${copy.playerName} to ${idea.guid}")
        return copy
    }

    fun updateRemote(idea: Idea) {
        if (idea.guid.isNotEmpty()
            && idea.gameGuid == gameGuid
        ) {
            // Update remotely at /ideas/<gameGuid>/<idea guid>
            val update = fireRef.child(idea.guid)
            // check that it's there
            try {
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
            } catch (e: Exception) {
                Timber.e("Unable to update idea $fireRef, $e")
            }
        }
    }
}