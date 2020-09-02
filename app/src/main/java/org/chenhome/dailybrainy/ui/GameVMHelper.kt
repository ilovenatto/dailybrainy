package org.chenhome.dailybrainy.ui

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.FullGameRepo
import org.chenhome.dailybrainy.repo.Idea
import java.util.concurrent.TimeUnit

class GenerateVMHelper {
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
}

class VoteVMHelper(val fullGameRepo: FullGameRepo) {

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
    val votesLeft: LiveData<Int>
        get() = _vote_votesLeft


    /**
     * Increments vote for this idea and updates the remote database
     *
     * @param idea
     */
    fun incrementVoteRemotely(idea: Idea) {
        val updated = idea.copy()
        updated.vote()
        val votes = _vote_votesLeft.value?.dec() ?: 0
        _vote_votesLeft.value = if (votes < 0) 0 else votes
        fullGameRepo.updateRemote(updated)
    }

}