package org.chenhome.dailybrainy.ui.challenges

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import org.chenhome.dailybrainy.repo.BrainyRepo
import org.chenhome.dailybrainy.repo.UserRepo
import org.chenhome.dailybrainy.repo.game.GameStub
import org.chenhome.dailybrainy.ui.Event


class JoinGameVM(
    private val context: Context,
    private val challengeGuid: String,

    ) : ViewModel() {
    private val brainyRepo: BrainyRepo = BrainyRepo(UserRepo(context))
    val availGames: LiveData<List<GameStub>> = Transformations
        .map(brainyRepo.allGameStubs) { allGames ->
            allGames.filter { stub ->
                stub.game.challengeGuid == challengeGuid
            }
        }

    /**
     * navToGame is a external immutable LiveData observable
     * by others
     */
    private var _navToGame = MutableLiveData<Event<String>>()
    val navToGame: LiveData<Event<String>>
        get() = _navToGame

    fun navToGame(gameGuid: String) {
        _navToGame.value = Event(gameGuid)
    }
}