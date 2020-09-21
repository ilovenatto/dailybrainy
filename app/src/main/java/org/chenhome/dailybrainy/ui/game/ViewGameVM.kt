package org.chenhome.dailybrainy.ui.game

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.FullGameRepo
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.ui.Event
import timber.log.Timber

/**
 * Offers current game data and any current players in that game.
 * Instantiate with[ViewGameVMFactory], not with Hilt.
 */
class ViewGameVM(
    val gameGuid: String,
    val context: Context,
) : ViewModel() {

    // Observer gets initialized as soon as ViewGameVM is instantiated
    private val fullGameRepo = FullGameRepo(context, gameGuid)

    /**
     * Full Game state. Read-only
     */
    val fullGame: LiveData<FullGame> = fullGameRepo.fullGame

    override fun onCleared() {
        super.onCleared()
        fullGameRepo.onClear()
    }

    /**
     * navToStep is a external immutable LiveData observable
     * by others
     */
    private var _navToStep = MutableLiveData<Event<Challenge.Step>>()
    val navToStep: LiveData<Event<Challenge.Step>>
        get() = _navToStep

    fun navToStep(step: Challenge.Step) {
        Timber.d("Nav to $step")
        _navToStep.value = Event(step)
    }
}
