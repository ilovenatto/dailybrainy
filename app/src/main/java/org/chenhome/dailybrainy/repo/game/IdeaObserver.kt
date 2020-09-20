package org.chenhome.dailybrainy.repo.game

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.chenhome.dailybrainy.repo.DbFolder
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.helper.notifyObserver
import org.chenhome.dailybrainy.repo.image.RemoteImage
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    private val scope = CoroutineScope(Dispatchers.IO)
    private val remoteImage = RemoteImage()

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
                fullGame.value?.add(added)
                fullGame.notifyObserver()
            }
        } catch (e: Exception) {
            Timber.e("Unable to add idea $fireRef, $e")
        }
    }

    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
        try {
            snapshot.getValue<Idea>()?.let { changed ->
                fullGame.value?.update(changed)
                fullGame.notifyObserver()
            }
        } catch (e: Exception) {
            Timber.e("Unable to modify idea $fireRef, $e")
        }
    }

    override fun onChildRemoved(snapshot: DataSnapshot) {
        // should never happen b/c app is not designed for any user to remove ideas
        // but including it in case it does (or if testing requires it)
        try {
            snapshot.getValue<Idea>()?.let { removed ->
                fullGame.value?.remove(removed)
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
    suspend fun insertRemote(idea: Idea): String? {

        // Add remotely to /ideas/<gameGuid>/<new idea>
        try {
            val created = fireRef.push()
            created.key?.let {

                // make a copy in case Idea reference gets used later
                val new = idea.copy()
                new.playerName = getPlayerName(new.playerGuid)
                new.imgUri = new.imgFn?.let { getImgUri(it)?.toString() }
                new.guid = created.key!!
                return suspendCoroutine<String?> { cont ->
                    created.setValue(new) { error, ref ->
                        error?.let {
                            Timber.w("Unable to add to $ref, idea $new, got $error")
                            cont.resume(null)
                        } ?: run {
                            Timber.d("Inserted idea ${new} at ${ref.key}")
                            cont.resume(ref.key)
                        }
                    }
                }
            } ?: run {
                Timber.w("Unable to insert idea to location $created")
                return null
            }
        } catch (e: Exception) {
            Timber.e("Unable to insert idea $fireRef, $e")
            return null
        }
    }

    suspend fun getRemote(ideaGuid: String): Idea? {
        if (ideaGuid.isNotEmpty()) {
            return suspendCoroutine<Idea?> { cont ->
                fireRef.child(ideaGuid).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        cont.resume(snapshot.getValue<Idea>())
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Timber.w("Unable to retreive idea $ideaGuid")
                        cont.resume(null)
                    }
                })
            }
        }
        return null
    }


    suspend fun updateRemote(idea: Idea) {
        if (idea.guid.isNotEmpty()
            && idea.gameGuid == gameGuid
        ) {
            // set imgFn and imgUri
            idea.imgFn?.let {
                idea.imgUri = getImgUri(it)?.toString()
            }

            // Update remotely at /ideas/<gameGuid>/<idea guid>
            val update = fireRef.child(idea.guid)
            // check that it's there
            try {
                update.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) = Timber.d("$error")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            update.setValue(idea) { error, ref ->
                                error?.let {
                                    Timber.w("Unable to update idea at $ref, $error")
                                } ?: Timber.d("Updated session ${idea.guid} at $ref")
                            }
                        } else {
                            Timber.w("Unable to update a non-existent idea, ${idea.guid} at $update")
                        }

                    }
                })
            } catch (e: Exception) {
                Timber.e("Unable to update idea $fireRef, $e")
            }
        }
    }


    private fun getPlayerName(playerGuid: String): String =
        fullGame.value?.players?.firstOrNull { session ->
            session.userGuid == playerGuid
        }?.name ?: "Unknown"


    private suspend fun getImgUri(path: String?): Uri? =
        path?.let {
            remoteImage.getValidStorageRef(it)?.let { storageRef ->
                remoteImage.getDownloadUri(storageRef)
            }
        }
}