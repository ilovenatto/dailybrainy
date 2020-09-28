package org.chenhome.dailybrainy.ui.story

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.chenhome.dailybrainy.R
import org.chenhome.dailybrainy.databinding.ItemSketchBinding
import org.chenhome.dailybrainy.databinding.ItemStepheaderBinding
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.repo.game.Sketch
import org.chenhome.dailybrainy.ui.PlaceholderDummy
import org.chenhome.dailybrainy.ui.SketchVHListener
import org.chenhome.dailybrainy.ui.bindImage
import org.jetbrains.annotations.NotNull
import timber.log.Timber

internal class StorySketchAdapter(val sketchListener: SketchVHListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var all = mutableListOf<Any>()

    companion object {
        const val TYPE_PLACEHOLDER = 1
        const val TYPE_SKETCH = 2
    }

    fun setGame(cxt: Context, game: FullGame) {
        with(all) {
            clear()
            add(PlaceholderDummy(cxt.getString(R.string.the_setting),
                cxt.getString(R.string.the_setting_desc)))
            addAll(game.ideas(Idea.Origin.STORY_SETTING).map { Sketch(it) })

            add(PlaceholderDummy(cxt.getString(R.string.the_solution),
                cxt.getString(R.string.the_solution_desc)))
            addAll(game.ideas(Idea.Origin.STORY_SOLUTION).map { Sketch(it) })

            add(PlaceholderDummy(cxt.getString(R.string.resolution),
                cxt.getString(R.string.reso_desc)))
            addAll(game.ideas(Idea.Origin.STORY_RESOLUTION).map { Sketch(it) })
        }

        notifyDataSetChanged()

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_SKETCH ->
                SketchVH(ItemSketchBinding.inflate(LayoutInflater.from(parent.context),
                    parent,
                    false))
            TYPE_PLACEHOLDER ->
                HeaderVH(ItemStepheaderBinding.inflate(LayoutInflater.from(parent.context),
                    parent,
                    false))
            else -> throw IllegalArgumentException("Encountered unknown view type $viewType")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = all[position]
        when (holder) {
            is SketchVH -> holder.bind(item as Sketch)
            is HeaderVH -> holder.bind(item as PlaceholderDummy)
        }
    }

    override fun getItemCount(): Int = all.size

    override fun getItemViewType(position: Int): Int =
        when (all[position]) {
            is PlaceholderDummy -> TYPE_PLACEHOLDER
            is Sketch -> TYPE_SKETCH
            else -> throw java.lang.IllegalArgumentException("Unrecognized type")

        }


    inner class HeaderVH(val binding: @NotNull ItemStepheaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(dummy: PlaceholderDummy) {
            binding.dummy = dummy
        }
    }

    inner class SketchVH(val binding: @NotNull ItemSketchBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(sketch: Sketch) {
            Timber.d("Binding sketch $sketch")
            binding.sketch = sketch
            binding.listener = sketchListener
            binding.votes.visibility = View.GONE
            // specially handle the image
            sketch.imgUri?.let {
                bindImage(binding.imageSketch, it)
            }
        }
    }
}