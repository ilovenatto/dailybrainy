package org.chenhome.dailybrainy.repo

import android.content.Context
import androidx.lifecycle.*
import org.chenhome.dailybrainy.repo.helper.ChallengeObserver
import org.chenhome.dailybrainy.repo.helper.GameStubObserver
import org.chenhome.dailybrainy.repo.helper.SingletonHolder
import timber.log.Timber

/**
 * Singleton that offers observable domain objects like [Challenge] to clients. These domain objects will
 * notify their observers when their state has changed.
 *b9
 * Get singleton by:
 * `BrainyRepo.singleton(context)`
 *
 * This singleton's setup and teardown lifecycle is dictated by [ProcessLifecycleOwner]. Data is only available
 * after the lifecycle has begun.
 *
 */
class BrainyRepo
private constructor(
    val context: Context
) : LifecycleObserver {
    /**
     * Private
     */
    private val lifecycleOwner = ProcessLifecycleOwner.get()

    /**
     * Remote data being observed
     */
    private val challengeObs =
        ChallengeObserver()
    private val gameStubObs =
        GameStubObserver(context)

    /**
     * Access singleton with
     * `BrainyRepo.singleton(context)`
     */
    companion object : SingletonHolder<BrainyRepo, Context>({
        val instance = BrainyRepo(it)
        instance.lifecycleOwner.lifecycle.addObserver(instance)
        instance
    })

    /**
     * Lifecycle
     * - onCreate: register observers of the Firebase remote database
     * - onStop: deregister them
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun onCreate() {
        Timber.d("Challenge and GameStub remote observers registered")
        challengeObs.register()
        gameStubObs.register()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop() {
        Timber.d("Challenge and GameStub remote observers deregistered")
        challengeObs.deregister()
        gameStubObs.deregister()
    }

    /**
     * List of challenges offered by DailyBrainy. They only change
     * when the app has published new challenges.
     */
    val challenges: LiveData<List<Challenge>> = challengeObs._challenges

    /**
     * List of [GameStub] started by the current user. [GameStub] are
     * game session that the user has participated in.
     */
    val gameStubs: LiveData<List<GameStub>> = gameStubObs._gameStubs

}
