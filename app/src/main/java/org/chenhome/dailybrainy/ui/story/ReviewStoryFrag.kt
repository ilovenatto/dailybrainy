package org.chenhome.dailybrainy.ui.story

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.databinding.RevStoryFragBinding
import org.chenhome.dailybrainy.ui.GameVMFactory
import org.chenhome.dailybrainy.ui.SketchAdapter

@AndroidEntryPoint
class ReviewStoryFrag : Fragment() {

    private val args: CreateStoryFragArgs by navArgs()
    private val vm: StoryVM by viewModels {
        GameVMFactory(requireContext(), args.gameGuid)
    }
    //private val playerAdap = PlayerAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val binding = RevStoryFragBinding.inflate(inflater, container, false)

        binding.vm = vm
//        binding.listPlayers.adapter = playerAdap
        binding.listener = SketchAdapter.SketchVHListener(
            {
                // do nothing on vote
            },
            { sketch ->
                vm.navToViewSketch(sketch)
            })

        binding.lifecycleOwner = viewLifecycleOwner
        binding.executePendingBindings()
        vm.fullGame.observe(viewLifecycleOwner, {
            it?.let { game ->
//                playerAdap.players = game.players
            }
        })

        vm.navToNext.observe(viewLifecycleOwner, {
            it.contentIfNotHandled()?.run {
                findNavController().popBackStack()
            }
        })

        vm.navToViewSketch.observe(viewLifecycleOwner, {
            it.contentIfNotHandled()?.let { sketch ->
                findNavController().navigate(
                    ReviewStoryFragDirections.actionReviewStoryFragToViewSketchFrag(
                        sketch.idea.gameGuid, sketch.idea.guid)
                )
            }
        })


        return binding.root

    }


}