package org.chenhome.dailybrainy.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.chenhome.dailybrainy.databinding.ItemIdeaBinding
import org.chenhome.dailybrainy.databinding.ItemSketchBinding
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

internal class IdeaAdapter(val showVotes: Boolean) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var ideas: List<Idea> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var listener: IdeaListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        IdeaVH(ItemIdeaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as IdeaVH).bind(ideas[position])
    }

    override fun getItemCount(): Int = ideas.size

    inner class IdeaVH(val binding: @NotNull ItemIdeaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(idea: Idea) {
            binding.idea = idea
            binding.votes.visibility = if (showVotes) View.VISIBLE else View.GONE
            this@IdeaAdapter.listener?.let { binding.listener = it }
        }
    }

    class IdeaListener(val listener: (Idea) -> Unit) {
        fun onClick(idea: Idea) = listener(idea)
    }
}

internal class SketchAdapter(val sketchListener: SketchVHListener, val voteEnabled: Boolean) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var sketches: List<Sketch> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        SketchVH(ItemSketchBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as SketchVH).bind(sketches[position])
    }

    override fun getItemCount(): Int = sketches.size

    inner class SketchVH(val binding: @NotNull ItemSketchBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(sketch: Sketch) {
            binding.sketch = sketch
            binding.listener = sketchListener
            binding.votes.visibility = if (voteEnabled) View.VISIBLE else View.GONE

            // specially handle the image
            sketch.imgUri?.let {
                bindImage(binding.imageSketch, it)
            }
        }
    }

    class SketchVHListener(
        val onVoteListener: (Sketch) -> Unit,
        val onViewListener: (Sketch) -> Unit,
    ) {
        fun onVote(sketch: Sketch) {
            onVoteListener(sketch)
        }

        fun onView(sketch: Sketch) {
            onViewListener(sketch)
        }
    }
}
