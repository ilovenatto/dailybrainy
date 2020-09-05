package org.chenhome.dailybrainy.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.chenhome.dailybrainy.databinding.GenIdeaItemIdeaBinding
import org.chenhome.dailybrainy.databinding.GenSketchItemBinding
import org.chenhome.dailybrainy.databinding.ViewGameItemPlayerBinding
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.PlayerSession
import org.chenhome.dailybrainy.repo.game.Sketch
import org.jetbrains.annotations.NotNull

internal class PlayerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var players: MutableList<PlayerSession> = mutableListOf()
        set(value) {
            field = value
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

internal class IdeaAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var ideas: MutableList<Idea> = mutableListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        IdeaVH(GenIdeaItemIdeaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as IdeaVH).bind(ideas[position])
    }

    override fun getItemCount(): Int = ideas.size

    class IdeaVH(val binding: @NotNull GenIdeaItemIdeaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(idea: Idea) {
            binding.idea = idea
        }
    }
}

internal class SketchAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var sketches: MutableList<Sketch> = mutableListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        SketchVH(GenSketchItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as SketchVH).bind(sketches[position])
    }

    override fun getItemCount(): Int = sketches.size

    class SketchVH(val binding: @NotNull GenSketchItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(sketch: Sketch) {
            binding.sketch = sketch

            // specially handle the image
            sketch.imgUri?.let {
                bindImage(binding.imageSketch, it)
            }
        }
    }
}
