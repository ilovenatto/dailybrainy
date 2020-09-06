package org.chenhome.dailybrainy.ui.sketch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.databinding.GenSketchItemBinding
import org.chenhome.dailybrainy.databinding.VoteSketchFragBinding
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.repo.game.Sketch
import org.chenhome.dailybrainy.ui.*
import org.jetbrains.annotations.NotNull

@AndroidEntryPoint
class VoteSketchFrag : Fragment() {

    private val args: VoteSketchFragArgs by navArgs()
    private val vm: SketchVM by viewModels {
        GameVMFactory(requireContext(), args.gameGuid)
    }

    private val playerAdap = PlayerAdapter()
    private val ideaAdap = IdeaAdapter()
    private val sketchAdap = VoteSketchAdapter(VoteSketchAdapter.Listener { sketch ->
        vm.vote.incrementVoteRemotely(sketch.idea)
    })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val binding = VoteSketchFragBinding.inflate(LayoutInflater.from(context), container, false)
        binding.vm = vm
        binding.listIdeas.adapter = ideaAdap
        binding.listSketches.adapter = sketchAdap
        binding.listPlayers.adapter = playerAdap
        binding.lifecycleOwner = viewLifecycleOwner

        binding.executePendingBindings()

        initAdapterObservers(vm.fullGame, sketchAdap, ideaAdap, playerAdap)
        initNavObserver(vm.navToNext)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        vm.generate.countdownTimer.start()
    }

    override fun onPause() {
        super.onPause()
        vm.generate.countdownTimer.cancel()
    }

    private fun initNavObserver(navToNext: LiveData<Event<Boolean>>) {
        navToNext.observe(viewLifecycleOwner, {
            it.contentIfNotHandled()?.run {
                findNavController().popBackStack()
            }
        })
    }

    private fun initAdapterObservers(
        fullGame: LiveData<FullGame>,
        sketchAdap: VoteSketchAdapter,
        ideaAdap: IdeaAdapter,
        playerAdap: PlayerAdapter,
    ) {
        fullGame.observe(viewLifecycleOwner, {
            it?.let {
                sketchAdap.sketches = it.ideas(Idea.Origin.SKETCH).map { idea -> Sketch(idea) }
                ideaAdap.ideas = it.ideas(Idea.Origin.BRAINSTORM)
                playerAdap.players = it.players
            }
        })
    }
}

internal class VoteSketchAdapter(val sketchListener: Listener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var sketches: List<Sketch> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        IdeaVH(GenSketchItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as IdeaVH).bind(sketches[position])
    }

    override fun getItemCount(): Int = sketches.size

    inner class IdeaVH(val binding: @NotNull GenSketchItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(sketch: Sketch) {
            binding.sketch = sketch
            binding.listener = sketchListener
            bindImage(binding.imageSketch, sketch.imgUri)
        }
    }

    class Listener(val listener: (Sketch) -> Unit) {
        fun onClick(sketch: Sketch) {
            listener(sketch)
        }
    }
}




