package org.chenhome.dailybrainy.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.chenhome.dailybrainy.databinding.ItemAvatarthumbBinding
import org.chenhome.dailybrainy.databinding.ItemPlayerBinding
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.PlayerSession
import org.chenhome.dailybrainy.repo.game.FullGame
import org.jetbrains.annotations.NotNull

internal class PlayerSheetAdapter {
    val playerAdapter = PlayerAdapter()
    val thumbAdapter = ThumbAdapter()

    var players = listOf<Player>()

    fun setGame(fullGame: FullGame) {
        // Update players
        players = fullGame.players.map { session ->
            val ideas = fullGame.ideasCount(Idea.Origin.BRAINSTORM, session.userGuid)
            val sketches = fullGame.ideasCount(Idea.Origin.SKETCH, session.userGuid)
            val panels = fullGame.ideasCount(Idea.Origin.STORY_SETTING,
                session.userGuid) + fullGame.ideasCount(Idea.Origin.STORY_SOLUTION,
                session.userGuid) + fullGame.ideasCount(Idea.Origin.STORY_RESOLUTION,
                session.userGuid)
            Player(session, sketches, ideas, panels)
        }.sortedBy { it.session.name }

        playerAdapter.notifyDataSetChanged()
        thumbAdapter.notifyDataSetChanged()
    }


    inner class ThumbAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ThumbVH(ItemAvatarthumbBinding.inflate(LayoutInflater.from(parent.context),
                parent,
                false))

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            (holder as ThumbVH).bind(players[position])

        override fun getItemCount(): Int = players.size

        inner class ThumbVH(val binding: ItemAvatarthumbBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(player: Player) {
                binding.player = player
            }
        }
    }

    inner class PlayerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RecyclerView.ViewHolder {
            return PlayerVH(ItemPlayerBinding.inflate(LayoutInflater.from(parent.context),
                parent,
                false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as PlayerVH).bind(players[position])
        }

        override fun getItemCount(): Int = players.size

        inner class PlayerVH(val binding: @NotNull ItemPlayerBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(player: Player) {
                binding.player = player
            }
        }
    }
}


data class Player(
    val session: PlayerSession,
    val sketches: Int,
    val ideas: Int,
    val storyPanels: Int,
) {
    val points
        get() = sketches + ideas + storyPanels

    fun avatarImage(context: Context): Drawable? = context
        .getDrawable(session.avatarImage().imgResId)
}
