package org.chenhome.dailybrainy.ui.challenges

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.chenhome.dailybrainy.databinding.ViewChallengesItemChallengeBinding
import org.chenhome.dailybrainy.databinding.ViewChallengesItemGameBinding
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.GameStub
import org.jetbrains.annotations.NotNull


/**
 * Listener invoked when item managed by [ViewChallengesAdapter] is clicked.
 * Typically used within a lambda in the view xml.
 * `        android:onClick="@{()->clickListener.onClick(challenge)}"`
 *
 * @property clickListener
 */
class ChallengeListener(val clickListener: (challengeGuid: String, category: Challenge.Category) -> Unit) {
    fun onClick(challenge: Challenge) = clickListener(challenge.guid, challenge.category)
}


class ViewChallengesAdapter(val challengeListener: ChallengeListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var merged = listOf<Any>()
    private var challenges = listOf<Challenge>()
    private var games = listOf<GameStub>()

    private val TYPE_UNDEFINED = -1
    private val TYPE_CHALL = 0
    private val TYPE_GAME = 1

    override fun getItemViewType(position: Int): Int {
        merged[position].let {
            val type = when (it::class) {
                GameStub::class -> TYPE_GAME
                Challenge::class -> TYPE_CHALL
                else -> TYPE_UNDEFINED
            }
            return type
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_GAME -> GameViewHolder(
                ViewChallengesItemGameBinding
                    .inflate(inflater, parent, false)
            )
            TYPE_CHALL -> ChallViewHolder(
                ViewChallengesItemChallengeBinding
                    .inflate(inflater, parent, false)
            )
            else -> ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ChallViewHolder -> {
                holder.bind(merged[position] as Challenge, challengeListener)
            }
            is GameViewHolder -> {
                holder.bind(merged[position] as GameStub)
            }
            is ViewHolder -> {
                holder.text1.text = "Unrecognized item"
            }
        }
    }

    override fun getItemCount(): Int = merged.size


    fun setChallenges(challenges: List<Challenge>) {
        merged = listOf(challenges, games).flatten()
        this.challenges = challenges
        notifyDataSetChanged()
    }

    fun setGames(gameStubs: List<GameStub>) {
        merged = listOf(challenges, games).flatten()
        this.games = gameStubs
        notifyDataSetChanged()
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text1: TextView = view.findViewById(android.R.id.text1)
    }

    inner class ChallViewHolder(val binding: @NotNull ViewChallengesItemChallengeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(challenge: Challenge, challengeListener: ChallengeListener) {
            binding.challenge = challenge
            binding.clickListener = challengeListener
            binding.executePendingBindings()
        }
    }

    inner class GameViewHolder(val binding: @NotNull ViewChallengesItemGameBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(gameStub: GameStub) {
            binding.game = gameStub
            binding.executePendingBindings()
        }
    }

}