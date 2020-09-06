package org.chenhome.dailybrainy.ui.story

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
import org.chenhome.dailybrainy.databinding.RevStoryFragBinding
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.repo.game.Sketch
import org.chenhome.dailybrainy.ui.Event
import org.chenhome.dailybrainy.ui.GameVMFactory
import org.chenhome.dailybrainy.ui.PlayerAdapter
import org.chenhome.dailybrainy.ui.SketchAdapter


@AndroidEntryPoint
class ReviewStoryFrag : Fragment() {

    private val args: CreateStoryFragArgs by navArgs()
    private val vm: StoryVM by viewModels {
        GameVMFactory(requireContext(), args.gameGuid)
    }


    private val playerAdap = PlayerAdapter()
    private val settingAdap = SketchAdapter()
    private val solutionAdap = SketchAdapter()
    private val resolutionAdap = SketchAdapter()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val binding = RevStoryFragBinding.inflate(inflater, container, false)

        binding.vm = vm
        binding.listPlayers.adapter = playerAdap
        binding.listSetting.adapter = settingAdap
        binding.listSolution.adapter = solutionAdap
        binding.listResolution.adapter = resolutionAdap

        binding.lifecycleOwner = viewLifecycleOwner
        binding.executePendingBindings()

        initAdapterObservers(vm.fullGame, playerAdap, settingAdap, solutionAdap, resolutionAdap)
        initNavObserver(vm.navToNext)
        return binding.root

    }

    private fun initAdapterObservers(
        fullGame: LiveData<FullGame>,
        playerAdap: PlayerAdapter,
        settingAdap: SketchAdapter,
        solutionAdap: SketchAdapter,
        resolutionAdap: SketchAdapter,
    ) {
        fullGame.observe(viewLifecycleOwner, {
            it?.let { game ->
                playerAdap.players = game.players
                settingAdap.sketches = game.ideas(Idea.Origin.STORY_SETTING).map { Sketch(it) }
                solutionAdap.sketches = game.ideas(Idea.Origin.STORY_SOLUTION).map { Sketch(it) }
                resolutionAdap.sketches =
                    game.ideas(Idea.Origin.STORY_RESOLUTION).map { Sketch(it) }


            }
        })
    }

    private fun initNavObserver(
        navToNext: LiveData<Event<Boolean>>,
    ) {
        navToNext.observe(viewLifecycleOwner, {
            it.contentIfNotHandled()?.run {
                findNavController().popBackStack()
            }
        })
    }


}