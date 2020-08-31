package org.chenhome.dailybrainy.repo

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.repo.game.GameObserver
import org.chenhome.dailybrainy.repo.game.IdeaObserver
import org.chenhome.dailybrainy.repo.game.PlayerSessionObserver
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
    private var _challengeImgUri = MutableLiveData<Uri>()
    private val gameObs = GameObserver(context, gameGuid, _fullGame, _challengeImgUri)
    private val ideaObs = IdeaObserver(context, gameGuid, _fullGame)
    private val playerObs = PlayerSessionObserver(context, gameGuid, _fullGame)

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
     * challengeImgUri is downloadable URI of the challenge hero image.
     * The value is available after the Game's challenge information has been retrieved.
     *
     * Observers wait for the value.
     */
    val challengeImgUri: LiveData<Uri>
        get() = _challengeImgUri


    /**
     * Insert into [FullGame] instance managed by [FullGameRepo] as well
     * as updating the remote database at the location:
     * - `/ideas/<gameGuid>/<new idea>`
     *
     * On remote database change, [GameObserver] will ignore this
     * idea since it's already in the [FullGame] instance.
     *
     * @param idea
     */
    fun insertRemote(idea: Idea) = ideaObs.insertRemote(idea)

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
    fun updateRemote(idea: Idea) {
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