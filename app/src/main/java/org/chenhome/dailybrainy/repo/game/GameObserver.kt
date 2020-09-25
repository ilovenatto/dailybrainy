package org.chenhome.dailybrainy.repo.game

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.chenhome.dailybrainy.repo.BrainyRepo
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.DbFolder
import org.chenhome.dailybrainy.repo.Game
import org.chenhome.dailybrainy.repo.image.RemoteImage
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Observes /games/<gameGuid> for changes
 */
class GameObserver(
    val context: Context,
    val gameGuid: String,
    val fullGame: MutableLiveData<FullGame>,
) : ValueEventListener {
    /**
     * Private
     *
     */
    // Define this class entry point to access its dependency, BrainyRepo
    @EntryPoint
    @InstallIn(ApplicationComponent::class)
    interface FullGameObserverEP {
        fun brainyRepo(): BrainyRepo
    }

    private val remoteImage = RemoteImage()
    private val fireDb = FirebaseDatabase.getInstance()
    private val fireRef = fireDb.getReference(DbFolder.GAMES.path)
        .child(gameGuid)
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Public
     */
    override fun onCancelled(error: DatabaseError) =
        Timber.d("$error")

    override fun onDataChange(snapshot: DataSnapshot) {
        scope.launch {
            try {
                snapshot.getValue<Game>()?.let { game ->
                    Timber.d("Remote game $gameGuid changed to $game.")
                    fullGame.value?.game = game

                    // Set challenge
                    fullGame.value?.challenge = getChallenge(game) ?: Challenge()
                    fullGame.postValue(fullGame.value)
                }
            } catch (e: Exception) {
                Timber.e("Unable to observe game $fireRef, $e")
            }
        }
    }

    private suspend fun getChallenge(game: Game): Challenge? {
        val challengesRef = fireDb.getReference(DbFolder.CHALLENGES.path)
        return suspendCoroutine<Challenge?> { cont ->
            challengesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.getValue<Map<String, Challenge>>()?.values?.firstOrNull {
                        it.guid == game.challengeGuid
                    }?.let { challenge ->
                        Timber.d("For game ${game.guid} loading challenge $challenge")
                        cont.resume(challenge)
                    } ?: run {
                        Timber.w("Unable to obtain challenges ${snapshot.ref}")
                        cont.resume(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.w("Get challenge cancelled, ${error.message}")
                    cont.resume(null)
                }
            })
        }?.let {
            it.imageUri = getImgUri(it.imgFn)
            Timber.d("Got challenge URI ${it.imageUri}")
            it
        }
    }

    private suspend fun getImgUri(path: String?): Uri? =
        path?.let {
            remoteImage.getValidStorageRef(it)?.let { storageRef ->
                remoteImage.getDownloadUri(storageRef)
            }
        }

    fun register() = fireRef.addValueEventListener(this)
    fun deregister() = fireRef.removeEventListener(this)


    fun updateRemote(game: Game) {
        if (game.guid.isNotEmpty()
            && game.guid == gameGuid
        ) {
            // Update remotely at /game/<gameGuid>
            val update = fireDb.getReference(DbFolder.GAMES.path).child(game.guid)
            // check that it's there
            update.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) = Timber.d("$error")
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        if (!snapshot.exists()) {
                            Timber.w("Unable to update a non-existent game, ${game.guid} at $update")
                            return
                        }
                        update.setValue(game) { error, ref ->
                            error?.let {
                                Timber.w("Unable to update game at $ref")
                            } ?: Timber.d("Updated game ${game} at $ref")
                        }
                    } catch (e: Exception) {
                        Timber.e("Unable to update game $game, $e")
                    }
                }
            })
        }
    }
}