package org.chenhome.dailybrainy.ui.idea

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.FullGameRepo
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.ui.Event
import timber.log.Timber
import java.util.concurrent.TimeUnit

class IdeaVM(
    context: Context,
    gameGuid: String,
) : ViewModel() {

    /**
     * =======================================
     * Common UI state across Generate, Vote and Review Idea
     * =======================================
     *
     *
     */

    /**
     * Expose FullGame
     */
    internal val fullGameRepo = FullGameRepo(context, gameGuid)
    val fullGame: LiveData<FullGame> = fullGameRepo.fullGame

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

    override fun onCleared() {
        fullGameRepo.onClear()
    }


    /**
     * =======================================
     * Used for Generating Idea "gen_" UI
     * =======================================
     *
     *
     */
    // Timer
    val gen_countdownTimer = object : CountDownTimer(
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
    val gen_countdown: LiveData<String>
        get() = _countdown

    /**
     * isCountdownOver is a external immutable LiveData observable
     * by others
     */
    private var _countdownOver = MutableLiveData<Boolean>(false)
    val gen_countdownOver: LiveData<Boolean>
        get() = _countdownOver

    /**
     * New idea from user
     */
    var gen_newIdea: MutableLiveData<String> = MutableLiveData("")


    /**
     * addIdea is a external immutable LiveData observable
     * by others
     */
    private var _addIdea = MutableLiveData<Event<Boolean>>()
    val gen_addIdea: LiveData<Event<Boolean>>
        get() = _addIdea

    fun gen_addIdea() {
        if (gen_newIdea.value?.isEmpty() != false) {
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
                title = gen_newIdea.value,
                imgFn = null
            )
            // attempt to insert
            fullGameRepo.insertRemote(idea)
        }
    }

    /**
     * =======================================
     * Used for Voting Idea "vote_" UI
     * =======================================
     *
     */
    /**
     * vote_votesLeft is a external immutable LiveData observable
     * by others
     */
    private var _vote_votesLeft = MutableLiveData<Int>(3)
    val vote_votesLeft: LiveData<Int>
        get() = _vote_votesLeft


    /**
     * Increments vote for this idea and updates the remote database
     *
     * @param idea
     */
    fun vote_incrementVoteRemotely(idea: Idea) {
        val updated = idea.copy()
        updated.vote()
        val votes = _vote_votesLeft.value?.dec() ?: 0
        _vote_votesLeft.value = if (votes < 0) 0 else votes
        fullGameRepo.updateRemote(updated)
    }


    /**
     * =======================================
     * Used for Review Idea "rev_" UI
     * =======================================
     *
     */
}
