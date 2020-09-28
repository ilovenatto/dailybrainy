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
import org.chenhome.dailybrainy.ui.PlayerSheetAdapter
import org.chenhome.dailybrainy.ui.SketchVHListener

@AndroidEntryPoint
class ReviewStoryFrag : Fragment() {

    private val args: CreateStoryFragArgs by navArgs()
    private val vm: StoryVM by viewModels {
        GameVMFactory(requireContext(), args.gameGuid)
    }
    private val playerAdap = PlayerSheetAdapter()

    private val sketchListener = SketchVHListener(
        { // do nothing
        }, { sketch -> // onview
            vm.navToViewSketch(sketch)
        })
    private val sketchAdap = StorySketchAdapter(sketchListener)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val binding = RevStoryFragBinding.inflate(inflater, container, false)

        // bind vars
        binding.vm = vm
        binding.lifecycleOwner = viewLifecycleOwner

        // toolbar
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        with(binding.avatars) {
            listPlayers.adapter = playerAdap.playerAdapter
            listThumbs.adapter = playerAdap.thumbAdapter
        }

        // bind list
        binding.list.adapter = sketchAdap

        // Refresh data
        vm.fullGame.observe(viewLifecycleOwner, {
            it?.let { game ->
                playerAdap.setGame(game)
                sketchAdap.setGame(requireContext(), game)
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

        binding.executePendingBindings()
        return binding.root

    }


}