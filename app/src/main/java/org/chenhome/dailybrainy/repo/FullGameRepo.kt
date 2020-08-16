package org.chenhome.dailybrainy.repo

import android.content.Context
import androidx.lifecycle.*
import org.chenhome.dailybrainy.repo.helper.FullGameObserver

/**
 * Repository to manage the data related to a full game [FullGame]
 *
 * @property context
 * @property lifecycleOwner
 */
class FullGameRepo(
    val context: Context,
    val gameGuid: String,
    val lifecycleOwner: LifecycleOwner
) : LifecycleObserver {

    /**
     * Observe the [FullGame] exposed by this observer.
     * Also use observer to update the state of the mutable [FullGame].
     * Observers of the [FullGame] will be notified when its state changes.
     */
    private val fullGameObs: FullGameObserver =
        FullGameObserver(gameGuid, context)

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    // Register/deregister FullGameObserver
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun onCreate() = fullGameObs.register()

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun onDestroy() = fullGameObs.deregister()


    /**
     * Expose [FullGame], data related to the whole game.
     */
    val fullGame: LiveData<FullGame> = fullGameObs._fullGame

    /**
     * Insert into [FullGame] instance managed by [FullGameObserver] as well
     * as updating the remote database at the location:
     * - `/ideas/<gameGuid>/<new idea>`
     *
     * On remote database change, [GameObserver] will ignore this
     * idea since it's already in the [FullGame] instance.
     *
     * @param idea
     */
    fun insertRemote(idea: Idea) = fullGameObs.IdeaObserver().insertRemote(idea)

    /**
     * Insert into [FullGame] instance managed by [FullGameObserver].
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
        fullGameObs.PlayerSessionObserver().insertRemote(playerSession)

    /**
     * Remotely updates Game.
     *
     * @param player Game should be from the [FullGame] instance. It should have its [Game.guid] set and
     * [Game.gameGuid] set to this Game's guid. This method will check for that.
     */
    fun updateRemote(game: Game) {
        fullGameObs.GameObserver().updateRemote(game)
    }


    /**
     * Remotely updates Idea.
     *
     * @param player Idea should be from the [FullGame] instance. It should have its [Idea.guid] set and
     * [Idea.gameGuid] set to this Game's guid. This method will check for that.
     */
    fun updateRemote(idea: Idea) {
        fullGameObs.IdeaObserver().updateRemote(idea)
    }

    /**
     * Remotely updates PlayerSession.
     *
     * @param player PlayerSession should be from the [FullGame] instance. It should have its [PlayerSession.guid] set and
     * [PlayerSession.gameGuid] set to this Game's guid. This method will check for that.
     */
    fun updateRemote(player: PlayerSession) {
        fullGameObs.PlayerSessionObserver().updateRemote(player)
    }


}

