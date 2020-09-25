package org.chenhome.dailybrainy.ui.game

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.chenhome.dailybrainy.R
import org.chenhome.dailybrainy.databinding.ItemStepBinding
import org.chenhome.dailybrainy.databinding.ItemStepheaderBinding
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.ui.GameStep
import org.chenhome.dailybrainy.ui.PlaceholderDummy
import org.jetbrains.annotations.NotNull

internal class GameStepAdapter(val stepListener: GameStepListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var all = mutableListOf<Any>()

    companion object {
        const val TYPE_PLACEHOLDER = 1
        const val TYPE_GAMESTEP = 2
        const val TYPE_UNKNOWN = 3
    }

    fun setSteps(cxt: Context, steps: List<GameStep>) {
        // create mapping of step -> GameStep
        all.clear()
        val step2Gamestep = steps.associate {
            it.step to it
        }

        // Add data to List<Any> of type, where each type maps to a ViewHolder
        // - PlaceholderDummy -> ItemStepheader
        // - GameStep -> ItemStep

        // Brainstorming
        all.add(PlaceholderDummy(
            cxt.getString(R.string.step, 1),
            cxt.getString(R.string.brainstorm)))
        step2Gamestep[Challenge.Step.GEN_IDEA]?.let { all.add(it) }
        step2Gamestep[Challenge.Step.VOTE_IDEA]?.let { all.add(it) }

        // Sketching
        all.add(PlaceholderDummy(
            cxt.getString(R.string.step, 2),
            cxt.getString(R.string.sketch)))
        step2Gamestep[Challenge.Step.GEN_SKETCH]?.let { all.add(it) }
        step2Gamestep[Challenge.Step.VOTE_SKETCH]?.let { all.add(it) }

        // Storyboard
        all.add(PlaceholderDummy(
            cxt.getString(R.string.step, 3),
            cxt.getString(R.string.storyboard)))
        step2Gamestep[Challenge.Step.CREATE_STORYBOARD]?.let { all.add(it) }
        step2Gamestep[Challenge.Step.VIEW_STORYBOARD]?.let { all.add(it) }

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_GAMESTEP ->
                StepVH(ItemStepBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            TYPE_PLACEHOLDER ->
                HeaderVH(ItemStepheaderBinding.inflate(LayoutInflater.from(parent.context),
                    parent,
                    false))
            else -> throw IllegalArgumentException("Encountered unknown view type $viewType")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = all[position]
        when (holder) {
            is StepVH -> holder.bind(item as GameStep)
            is HeaderVH -> holder.bind(item as PlaceholderDummy)
        }
    }

    override fun getItemCount(): Int = all.size

    override fun getItemViewType(position: Int): Int =
        when (all[position]) {
            is PlaceholderDummy -> TYPE_PLACEHOLDER
            is GameStep -> TYPE_GAMESTEP
            else -> TYPE_UNKNOWN
        }

    inner class HeaderVH(val binding: @NotNull ItemStepheaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(dummy: PlaceholderDummy) {
            binding.dummy = dummy
        }
    }

    inner class StepVH(val binding: @NotNull ItemStepBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(step: GameStep) {
            binding.gameStep = step
            binding.listener = stepListener
        }
    }

    class GameStepListener(val listener: (Challenge.Step) -> Unit) {
        fun onClick(step: Challenge.Step) {
            listener(step)
        }
    }
}