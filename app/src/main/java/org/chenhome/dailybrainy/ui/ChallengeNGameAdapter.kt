package org.chenhome.dailybrainy.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.chenhome.dailybrainy.R
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.GameStub

class ChallengeNGameAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
        return when (viewType) {
            TYPE_GAME -> GameViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_challenges_item_game, parent, false)
            )
            TYPE_CHALL -> ChallViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_challenges_item_challenge, parent, false)
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
                val content = merged[position] as Challenge
                holder.title.text = content.title
                holder.descrip.text = content.desc
            }
            is GameViewHolder -> {
                val content = merged[position] as GameStub
                holder.guid.text = content.game.guid
                holder.pin.text = content.game.pin
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

    inner class ChallViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title)
        val descrip: TextView = view.findViewById(R.id.descrip)
    }

    inner class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val guid: TextView = view.findViewById(R.id.guid)
        val pin: TextView = view.findViewById(R.id.pin)
    }

}