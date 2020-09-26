package org.chenhome.dailybrainy.ui.idea

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
import org.chenhome.dailybrainy.databinding.GenIdeaFragBinding
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.ui.GameVMFactory
import org.chenhome.dailybrainy.ui.IdeaAdapter
import org.chenhome.dailybrainy.ui.PlayerSheetAdapter
import timber.log.Timber

@AndroidEntryPoint
class GenIdeaFrag : Fragment() {

    private val args: GenIdeaFragArgs by navArgs()
    private val vm: IdeaVM by viewModels {
        GameVMFactory(requireContext(), args.gameGuid)
    }

    private val playerAdap = PlayerSheetAdapter()
    private val ideaAdap = IdeaAdapter(false)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val binding = GenIdeaFragBinding.inflate(LayoutInflater.from(context), container, false)
        binding.vm = vm
        binding.listIdeas.adapter = ideaAdap
        with(binding.avatars) {
            listPlayers.adapter = playerAdap.playerAdapter
            listThumbs.adapter = playerAdap.thumbAdapter
        }
        binding.lifecycleOwner = viewLifecycleOwner
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.executePendingBindings()

        initAdapterObservers(vm.fullGame, ideaAdap, playerAdap)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        Timber.d("Starting timer")
        vm.generate.countdownTimer.start()
    }

    override fun onPause() {
        super.onPause()
        vm.generate.countdownTimer.cancel()
    }


    private fun initAdapterObservers(
        fullGame: LiveData<FullGame>,
        ideaAdap: IdeaAdapter,
        playerAdap: PlayerSheetAdapter,
    ) {
        fullGame.observe(viewLifecycleOwner, {
            it?.let {
                Timber.d("Got challenge ${it.challenge.title}")
                ideaAdap.ideas = it.ideas(Idea.Origin.BRAINSTORM)
                playerAdap.setGame(it)
            }
        })
    }

}
