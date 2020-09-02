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
import org.chenhome.dailybrainy.databinding.GenSketchFragBinding
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.ui.*
import timber.log.Timber

@AndroidEntryPoint
class GenSketchFrag : Fragment() {

    private val args: GenSketchFragArgs by navArgs()
    private val vm: SketchVM by viewModels {
        GameVMFactory(requireContext(), args.gameGuid)
    }

    private val playerAdap = PlayerAdapter()
    private val ideaAdap = IdeaAdapter()
    private val sketchAdap = SketchAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val binding = GenSketchFragBinding.inflate(LayoutInflater.from(context), container, false)
        binding.vm = vm
        binding.listIdeas.adapter = ideaAdap
        binding.listPlayers.adapter = playerAdap
        binding.listSketches.adapter = sketchAdap
        binding.lifecycleOwner = viewLifecycleOwner
        binding.executePendingBindings()

        initAdapterObservers(vm.fullGame, ideaAdap, playerAdap, sketchAdap)
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

    private fun initNavObserver(
        navToNext: LiveData<Event<Boolean>>,
    ) {
        navToNext.observe(viewLifecycleOwner, {
            it.contentIfNotHandled()?.run {
                findNavController().popBackStack()
            }
        })
    }


    private fun initAdapterObservers(
        fullGame: LiveData<FullGame>,
        ideaAdap: IdeaAdapter,
        playerAdap: PlayerAdapter,
        sketchAdap: SketchAdapter,
    ) {
        fullGame.observe(viewLifecycleOwner, {
            it?.let {
                Timber.d("Got challenge ${it.challenge.title}")
                ideaAdap.ideas = it.ideas
                playerAdap.players = it.players
                sketchAdap.sketches = it.sketches
            }
        })
    }

}

