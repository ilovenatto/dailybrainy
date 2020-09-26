package org.chenhome.dailybrainy.ui.idea

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.databinding.VoteIdeaFragBinding
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.ui.GameVMFactory
import org.chenhome.dailybrainy.ui.IdeaAdapter
import org.chenhome.dailybrainy.ui.PlayerSheetAdapter
import timber.log.Timber

@AndroidEntryPoint
class VoteIdeaFrag : Fragment() {

    private val args: VoteIdeaFragArgs by navArgs()
    private val vm: IdeaVM by viewModels {
        GameVMFactory(requireContext(), args.gameGuid)
    }

    private val playerAdap = PlayerSheetAdapter()
    private val ideaAdap = IdeaAdapter(true)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val binding = VoteIdeaFragBinding.inflate(LayoutInflater.from(context), container, false)
        binding.vm = vm

        // idea list
        ideaAdap.listener = IdeaAdapter.IdeaListener { idea ->
            vm.vote.incrementVoteRemotely(idea)
            Timber.d("voted for idea $idea")
        }
        binding.listIdeas.adapter = ideaAdap

        // player list
        with(binding.avatars) {
            listThumbs.adapter = playerAdap.thumbAdapter
            listPlayers.adapter = playerAdap.playerAdapter
        }
        vm.fullGame.observe(viewLifecycleOwner, {
            it?.let {
                ideaAdap.ideas = it.ideas(Idea.Origin.BRAINSTORM)
                playerAdap.setGame(it)
            }
        })

        // Nav
        binding.lifecycleOwner = viewLifecycleOwner
        with(binding.toolbar) {
            setNavigationOnClickListener { findNavController().popBackStack() }

        }
        binding.executePendingBindings()


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


}
