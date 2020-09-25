package org.chenhome.dailybrainy.ui.game

import android.content.Context
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.FullGameRepo
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.UserRepo
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.ui.Event
import org.chenhome.dailybrainy.ui.GameStep
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
    private val userRepo = UserRepo(context)

    /**
     * Full Game state. Read-only
     */
    val fullGame: LiveData<FullGame> = fullGameRepo.fullGame

    val gameSteps: LiveData<List<GameStep>> = Transformations.map(fullGame) { fullGame ->
        val steps = mutableListOf<GameStep>()
        val current = fullGame.game.currentStep
        Timber.d("Fullgame updated remotely to current step $current")

        Challenge.Step.values().forEach { step ->
            val numItems = when (step) {
                Challenge.Step.GEN_IDEA -> fullGame.ideasCount(Idea.Origin.BRAINSTORM)
                Challenge.Step.VOTE_IDEA -> fullGame.voteCount(Idea.Origin.BRAINSTORM)
                Challenge.Step.GEN_SKETCH -> fullGame.ideasCount(Idea.Origin.SKETCH)
                Challenge.Step.VOTE_SKETCH -> fullGame.voteCount(Idea.Origin.SKETCH)
                Challenge.Step.CREATE_STORYBOARD -> {
                    fullGame.voteCount(Idea.Origin.STORY_SETTING) + fullGame.voteCount(Idea.Origin.STORY_SETTING) + fullGame.voteCount(
                        Idea.Origin.STORY_SETTING)
                }
                Challenge.Step.VIEW_STORYBOARD -> {
                    fullGame.ideasCount(Idea.Origin.STORY_SETTING) + fullGame.ideasCount(Idea.Origin.STORY_SETTING) + fullGame.ideasCount(
                        Idea.Origin.STORY_SETTING)
                }
            }
            steps.add(GameStep(
                step = step,
                isComplete = step.less(current),
                isCurrentStep = step == current, numItems))
        }
        steps.toList()
    }

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
        // also update the current step in FullGame.Game.currentStep
        // if current player is the originator of the game session
        // notify observers
        if (userRepo.currentPlayerGuid == fullGame.value?.game?.playerGuid) {
            Timber.d("Current player originated this game. Updating the current step of the game for everyone to be $step")
            viewModelScope.launch {
                fullGame.value?.game?.let { game ->
                    game.currentStep = step
                    fullGameRepo.updateRemote(game)
                    _navToStep.value = Event(step)
                }
            }
        } else {
            _navToStep.value = Event(step)
        }
    }
}

