package org.chenhome.dailybrainy.ui.idea

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.FullGame
import org.chenhome.dailybrainy.repo.FullGameObserver
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.ui.Event
import timber.log.Timber
import java.util.concurrent.TimeUnit

class GenIdeaVM(
    context: Context,
    gameGuid: String,
) : ViewModel() {

    /**
     * Expose FullGame
     */
    private val fullGameObs = FullGameObserver(context, gameGuid)
    val fullGame: LiveData<FullGame> = fullGameObs.fullGame

    // Timer
    val countdownTimer = object : CountDownTimer(
        TimeUnit.SECONDS.toMillis(Challenge.Step.GEN_IDEA.allowedSecs),
        TimeUnit.SECONDS.toMillis(1)) {
        override fun onTick(millisUntilFinished: Long) {
            // TODO: 8/30/20 Get this value to update
            _countdown.value = format(millisUntilFinished)
        }

        override fun onFinish() {
            _countdownOver.value = true
        }

        fun format(durationMillis: Long): String =
            String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60,
                TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60)

    }

    /**
     * countdown is a external immutable LiveData observable
     * by others
     */
    private var _countdown = MutableLiveData<String>("N/A")
    val countdown: LiveData<String>
        get() = _countdown

    /**
     * isCountdownOver is a external immutable LiveData observable
     * by others
     */
    private var _countdownOver = MutableLiveData<Boolean>(false)
    val countdownOver: LiveData<Boolean>
        get() = _countdownOver

    /**
     * New idea from user
     */
    var newIdea: MutableLiveData<String> = MutableLiveData("")

    /**
     * navToNext is a external immutable LiveData observable
     * by others
     */
    private var _navToNext = MutableLiveData<Event<Boolean>>()
    val navToNext: LiveData<Event<Boolean>>
        get() = _navToNext

    fun navToNext() {
        _navToNext.value = Event(true)
    }

    /**
     * addIdea is a external immutable LiveData observable
     * by others
     */
    private var _addIdea = MutableLiveData<Event<Boolean>>()
    val addIdea: LiveData<Event<Boolean>>
        get() = _addIdea

    fun addIdea() {
        if (newIdea.value?.isEmpty() != false) {
            Timber.w("No idea to add")
            return
        }
        _addIdea.value = Event(true)

        // init the idea
        fullGame.value?.let {
            val idea = Idea(
                guid = "",
                gameGuid = it.game.guid,
                playerGuid = it.game.playerGuid,
                playerName = "",
                origin = Idea.Origin.BRAINSTORM,
                votes = 0,
                title = newIdea.value,
                imgFn = null
            )
            // attempt to insert
            fullGameObs.IdeaObserver().insertRemote(idea)
        }
    }

    override fun onCleared() {
        fullGameObs.onDestroy()
    }
}


class GenIdeaVMFactory(
    private val context: Context,
    private val gameGuid: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GenIdeaVM::class.java)) {
            return GenIdeaVM(context, gameGuid) as T
        }
        throw IllegalArgumentException("Unsupported GenIdeaVM type $modelClass")
    }
}