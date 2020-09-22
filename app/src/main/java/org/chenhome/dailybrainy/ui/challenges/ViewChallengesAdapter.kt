package org.chenhome.dailybrainy.ui.challenges

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.chenhome.dailybrainy.databinding.CardGameBinding
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.game.GameStub
import org.jetbrains.annotations.NotNull
import timber.log.Timber


/**
 * Listener invoked when item managed by [ViewChallengesAdapter] is clicked.
 * Typically used within a lambda in the view xml.
 * `        android:onClick="@{()->clickListener.onClick(challenge)}"`
 *
 * @property clickListener
 */
class ChallengeListener(
    val joinListener: (challengeGuid: String) -> Unit,
    val newGameListener: (challengeGuid: String) -> Unit,
) {
    fun onJoinGame(challenge: Challenge) =
        if (challenge.category == Challenge.Category.CHALLENGE) joinListener(challenge.guid)
        else Timber.w("Challenge is the wrong category")

    fun onNewGame(challenge: Challenge) =
        if (challenge.category == Challenge.Category.CHALLENGE) newGameListener(challenge.guid)
        else Timber.w("Wrong category")
}

class LessonListener(val viewListener: (challengeGuid: String) -> Unit) {
    fun onViewLesson(challenge: Challenge) =
        if (challenge.category == Challenge.Category.LESSON) viewListener(challenge.guid)
        else Timber.w("Wrong category")
}

class GameListener(val clickListener: (gameStub: GameStub) -> Unit) {
    fun onClick(gameStub: GameStub) = clickListener(gameStub)
}


class ViewGamesAdapter(
    private val gameListener: GameListener,
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var games = listOf<GameStub>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        Timber.d("Creating Game VH")
        return GameViewHolder(CardGameBinding
            .inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as GameViewHolder).bind(games[position], gameListener)
    }

    override fun getItemCount(): Int = games.size

    fun setGames(gameStubs: List<GameStub>) {
        this.games = gameStubs
        notifyDataSetChanged()
    }

    inner class GameViewHolder(val binding: @NotNull CardGameBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(gameStub: GameStub, listener: GameListener) {
            Timber.d("Binding $gameStub")
            binding.gameStub = gameStub
            binding.listener = listener
            binding.executePendingBindings()
        }
    }

}