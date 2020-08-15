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
    fun insert(idea: Idea) = fullGameObs.IdeaObserver().insert(idea)

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
    fun insert(playerSession: PlayerSession) =
        fullGameObs.PlayerSessionObserver().insert(playerSession)

    // TODO: 8/14/20 update
    /**
     * Updates the mutable parts of the [FullGame], which are
     * - [FullGame.ideas]
     * - [FullGame.game]
     *
     * The list of actual ideas are updated when user adds one locally or
     * ideas are added remotely by other players. When added or removed remotely, the [IdeaObserver]
     * adds/removes them to/from [FullGame].
     *
     * Clients use [insert] to add locally to the [FullGame] instance; the method will also add the new idea remotely.
     *
     * The [FullGame.players] are updated when players are added locally
     * and removed remotely (in which case the observer will update the [FullGame.players] state.
     *
     * The [FullGame.challenge] is immutable.
     *
     * @param fullGame
     */
//    fun update(fullGame: FullGame)


}

