package org.chenhome.dailybrainy.repo

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.repo.game.GameObserver
import org.chenhome.dailybrainy.repo.game.IdeaObserver
import org.chenhome.dailybrainy.repo.game.PlayerSessionObserver
import org.chenhome.dailybrainy.repo.image.LocalImageRepo
import org.chenhome.dailybrainy.repo.image.RemoteImage
import org.chenhome.dailybrainy.repo.image.RemoteImageFolder
import timber.log.Timber

/**
 * Observes remote game [FullGame], related [Idea] and [PlayerSession] for remote changes.
 *
 * Also clients can use this class to update the state of the remote, mutable [FullGame].
 * Observers of the [FullGameRepo.fullGame] will be notified when its state changes.
 *
 * Not injectable b/c it requires [gameGuid] parameter to be set at construction time. Cannot
 * lazily initialize [gameGuid] b/c that value is required by the [GameObserver] and others.
 * These observers are created and start listening when this class is instantiated.
 */
class FullGameRepo(
    val context: Context,
    val gameGuid: String,
) {

    /**
     * Private
     */
    private var _fullGame: MutableLiveData<FullGame> = MutableLiveData(FullGame())
    private val gameObs = GameObserver(context, gameGuid, _fullGame)
    private val ideaObs = IdeaObserver(context, gameGuid, _fullGame)
    private val playerObs = PlayerSessionObserver(context, gameGuid, _fullGame)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val remoteImage = RemoteImage()
    private val localImage = LocalImageRepo(context)

    init {
        Timber.d("Registering FullGameObserver for game $gameGuid")
        gameObs.register()
        ideaObs.register()
        playerObs.register()
    }


    /**
     * Clean up and Deregister listeners to FireDB. Should be called
     * by whoever is managing this instance's lifecycle.
     */
    fun onClear() {
        Timber.d("Deregistering FullGameObserver")
        gameObs.deregister()
        ideaObs.deregister()
        playerObs.deregister()
    }

    /**
     * Expose [FullGame] data to be used by clients
     */
    val fullGame: LiveData<FullGame> = _fullGame

    /**
     * Insert into [FullGame] instance managed by [FullGameRepo] as well
     * as updating the remote database at the location:
     * - `/ideas/<gameGuid>/<new idea>`
     *
     * On remote database change, [GameObserver] will ignore this
     * idea since it's already in the [FullGame] instance.
     *
     * @param idea
     * @return Idean GUID or null if insert failed
     */
    suspend fun insertRemote(idea: Idea): String? = ideaObs.insertRemote(idea)


    /**
     * Insert remote idea, then upload the image. After upload, update the remote idea.
     *
     * This allows a idea stub to be shown by the app while the image gets updated.
     *
     * @param sketchImageUri
     * @param currentPlayerGuid
     * @return whether image upload succeeded or not
     */
    fun insertRemoteSketch(sketchImageUri: Uri, currentPlayerGuid: String) {
        if (!localImage.isExist(sketchImageUri)) {
            return
        }

        scope.launch {
            runBlocking {
                val idea = Idea(
                    "", // gets set by FullGameRepo
                    gameGuid,
                    currentPlayerGuid,
                    Idea.Origin.SKETCH)
                val ideaGuid = insertRemote(idea) ?: run {
                    Timber.w("Unable to insert remote idea $idea")
                    return@runBlocking
                }

                val ref = remoteImage.upload(RemoteImageFolder.SKETCHES, sketchImageUri) ?: run {
                    Timber.w("Unable to upload image $sketchImageUri")
                    return@runBlocking
                }

                // get newly inserted idea
                var inserted = ideaObs.getRemote(ideaGuid) ?: run {
                    Timber.w("Unable to get newly inserted idea. Aborting")
                    return@runBlocking
                }

                // update idea with new location of image. launches thread to do work
                inserted.imgFn = ref.path
                ideaObs.updateRemote(inserted)
            }
        }
    }


    /**
     * Insert into [FullGame] instance managed by [FullGameRepo].
     *
     * Method will ensure that no
     * duplicate player sessions are inserted locally or remotely.
     *
     * Also insert into the following remote locations. :
     * - `/playersessions/<gameGuid>/<new session>`
     *
     * @param playerSession
     */
    fun insertRemote(playerSession: PlayerSession) =
        playerObs.insertRemote(playerSession)

    /**
     * Remotely updates Game.
     *
     * @param game Game should be from the [FullGame] instance.
     * [Game.guid] should be set. This method will check for that.
     */
    fun updateRemote(game: Game) {
        gameObs.updateRemote(game)
    }

    /**
     * Remotely updates Idea.
     *
     * @param idea Idea should be from the [FullGame] instance. It should have its [Idea.guid] set and
     * [Idea.gameGuid] set to this Game's guid. This method will check for that.
     */
    suspend fun updateRemote(idea: Idea) {
        ideaObs.updateRemote(idea)
    }

    /**
     * Remotely updates PlayerSession.
     *
     * @param player PlayerSession should be from the [FullGame] instance. It should have its [PlayerSession.guid] set and
     * [PlayerSession.gameGuid] set to this Game's guid. This method will check for that.
     */
    fun updateRemote(player: PlayerSession) {
        playerObs.updateRemote(player)
    }

}