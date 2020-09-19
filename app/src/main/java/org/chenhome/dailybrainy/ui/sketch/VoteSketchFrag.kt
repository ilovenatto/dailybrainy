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
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.databinding.VoteSketchFragBinding
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.repo.game.Sketch
import org.chenhome.dailybrainy.ui.*

@AndroidEntryPoint
class VoteSketchFrag : Fragment() {

    private val args: VoteSketchFragArgs by navArgs()
    private val vm: SketchVM by viewModels {
        GameVMFactory(requireContext(), args.gameGuid)
    }

    private val playerAdap = PlayerAdapter()
    private val ideaAdap = IdeaAdapter()
    private val sketchAdap = SketchAdapter(SketchAdapter.SketchVHListener(
        { sketch -> // onvote
            vm.vote.incrementVoteRemotely(sketch.idea)
        }, { sketch -> // onview
            vm.navToViewSketch(sketch)
        }), true
    )

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
        initNavObserver(vm.navToNext, vm.navToViewSketch)
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

    private fun initNavObserver(
        navToNext: LiveData<Event<Boolean>>,
        navToViewSketch: LiveData<Event<Sketch>>,
    ) {
        navToNext.observe(viewLifecycleOwner, {
            it.contentIfNotHandled()?.run {
                findNavController().popBackStack()
            }
        })
        navToViewSketch.observe(viewLifecycleOwner, {
            it.contentIfNotHandled()?.let { sketch ->
                findNavController()
                    .navigate(VoteSketchFragDirections
                        .actionVoteSketchFragToViewSketchFrag(
                            sketch.idea.gameGuid,
                            sketch.idea.guid))
            }
        })
    }


    private fun initAdapterObservers(
        fullGame: LiveData<FullGame>,
        sketchAdap: SketchAdapter,
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




