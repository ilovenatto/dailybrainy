package org.chenhome.dailybrainy.ui.game

import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import dagger.hilt.android.qualifiers.ApplicationContext
import org.chenhome.dailybrainy.repo.BrainyRepo
import org.chenhome.dailybrainy.repo.PlayerSession
import org.chenhome.dailybrainy.repo.UserRepo
import org.chenhome.dailybrainy.repo.helper.notifyObserver
import org.chenhome.dailybrainy.repo.image.AvatarImage
import org.chenhome.dailybrainy.ui.Event
import timber.log.Timber

class NewGameVM @ViewModelInject constructor(
    val userRepo: UserRepo, // injected by Hilt
    @ApplicationContext context: Context
) : ViewModel() {
    private val brainyRepo = BrainyRepo.singleton(context)

    // Current player, changeable by View, via 2-way data binding
    var player: MutableLiveData<PlayerSession> = MutableLiveData(PlayerSession())

    /**
     * Called by Fragment to persist the new game in remote database
     *
     * @param challengeGuid Challenge that this [Game] is for.
     * @return gameGuid of the newly created game
     */
    private fun persistNewGame(challengeGuid: String): String {
        // Create game
        val gameGuid = "foobar" //brainyRepo.insertGame(challengeGuid, userRepo.currentPlayerGuid)

        // Create player
        player.value?.gameGuid = gameGuid
        player.value?.userGuid = userRepo.currentPlayerGuid
        //brainyRepo.insertPlayer(_player.value)
        return gameGuid
    }

    // Whether all form values are set
    val valid = MediatorLiveData<Boolean>().apply {
        addSource(player, Observer {
            value = it.name.isNotEmpty() &&
                    (it.imgFn?.isNotEmpty() ?: false)
            Timber.d("Got valid $value")
        })
    }

    /**
     * NAVIGATE: Navigate to Existing Game (gameGuid required)
     *
     * @param gameGuid Guid for existing [Game]
     */
    fun navToGame(challengeGuid: String) {
        // persist a new game
        // TODO: 8/27/20 check insert successful
        val gameGuid = persistNewGame(challengeGuid)
        _navToGame.value = Event(gameGuid)
    }
    private val _navToGame = MutableLiveData<Event<String>>()

    // Fragment listens to this variable and will navigate on state change of this var.
    val navToGame: LiveData<Event<String>>
        get() = _navToGame


    fun onAvatarSelected(avatarImage: AvatarImage) {
        player.value?.imgFn = avatarImage.name
        player.notifyObserver()
    }

}