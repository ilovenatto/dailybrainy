package org.chenhome.dailybrainy.ui

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.FullGameRepo
import org.chenhome.dailybrainy.repo.Idea
import timber.log.Timber
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
    private var _votesLeft = MutableLiveData<Int>(3)
    val votesLeft: LiveData<Int>
        get() = _votesLeft

    val scope = CoroutineScope(Dispatchers.IO)


    /**
     * Increments vote for this idea and updates the remote database
     *
     * @param idea
     */
    fun incrementVoteRemotely(idea: Idea) {
        if (_votesLeft.value == 0) {
            // ignore
            Timber.w("Ignoring request to vote. Out of votes.")
            return
        }

        val votes = _votesLeft.value?.dec() ?: 0
        _votesLeft.value = if (votes < 0) 0 else votes

        Timber.d("Incremeting vote on idean $idea")
        val updated = idea.copy()
        updated.vote()
        scope.launch {
            fullGameRepo.updateRemote(updated)
        }
    }

}