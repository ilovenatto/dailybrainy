package org.chenhome.dailybrainy.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.chenhome.dailybrainy.databinding.ViewGameItemPlayerBinding
import org.chenhome.dailybrainy.repo.PlayerSession
import org.jetbrains.annotations.NotNull

internal class PlayerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var players: MutableList<PlayerSession> = mutableListOf()
    fun setPlayers(value: MutableList<PlayerSession>) {
        players = value
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return PlayerVH(ViewGameItemPlayerBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as PlayerVH).bind(players[position])
    }

    override fun getItemCount(): Int = players.size

    class PlayerVH(val binding: @NotNull ViewGameItemPlayerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(player: PlayerSession) {
            binding.player = player
            binding.playerAvatar.setImageResource(player.avatarImage().imgResId)
        }
    }

}