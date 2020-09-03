package org.chenhome.dailybrainy.repo.game

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ApplicationComponent
import org.chenhome.dailybrainy.repo.BrainyRepo
import org.chenhome.dailybrainy.repo.DbFolder
import org.chenhome.dailybrainy.repo.Game
import org.chenhome.dailybrainy.repo.helper.notifyObserver
import timber.log.Timber

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

    private val brainyRepo = EntryPointAccessors.fromApplication(context,
        FullGameObserverEP::class.java)
        .brainyRepo()

    private val fireDb = FirebaseDatabase.getInstance()
    private val fireRef = fireDb.getReference(DbFolder.GAMES.path)
        .child(gameGuid)

    /**
     * Public
     */
    override fun onCancelled(error: DatabaseError) =
        Timber.d("$error")

    override fun onDataChange(snapshot: DataSnapshot) {
        try {
            snapshot.getValue<Game>()?.let { game ->
                Timber.d("Remote game $gameGuid changed to $game. There are ${brainyRepo.challenges.value?.size} challenges.")
                fullGame.value?.game = game

                // Set challenge
                brainyRepo.challenges.value?.firstOrNull {
                    it.guid == game.challengeGuid
                }?.let { challenge ->
                    Timber.d("For game ${game.guid} loading challenge ${challenge}")
                    fullGame.value?.challenge = challenge
                    fullGame.notifyObserver()
                }

            }
        } catch (e: Exception) {
            Timber.e("Unable to observe game $fireRef, $e")
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
                    try {
                        snapshot.getValue<Game>()?.let {
                            update.setValue(game) { error, ref ->
                                error?.let {
                                    Timber.w("Unable to update game at $ref")
                                } ?: Timber.d("Updated game ${game.guid} at $ref")
                            }
                        }
                            ?: Timber.w("Unable to update a non-existent game, ${game.guid} at $update")
                    } catch (e: Exception) {
                        Timber.e("Unable to update game $game, $e")
                    }
                }
            })
        }
    }
}