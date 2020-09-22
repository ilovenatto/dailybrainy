package org.chenhome.dailybrainy.ui.challenges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.databinding.JoinGameFragBinding
import org.chenhome.dailybrainy.repo.UserRepo
import org.chenhome.dailybrainy.ui.ChallengeVMFactory
import org.chenhome.dailybrainy.ui.game.NewGameFrag
import timber.log.Timber


@AndroidEntryPoint
class JoinGameFrag : Fragment() {

    private val args: JoinGameFragArgs by navArgs()
    private val vm: JoinGameVM by viewModels {
        ChallengeVMFactory(requireContext(), args.challengeGuid)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val myPlayerGuid = UserRepo(requireContext()).currentPlayerGuid
        val binding = JoinGameFragBinding.inflate(inflater, container, false)
        binding.vm = vm

        val gamesAdapter = ViewGamesAdapter(GameListener { stub, view ->
            if (stub.players.any { it.userGuid == myPlayerGuid }) {
                // goto existing game directly if currenty user is one of the players in that game
                findNavController().navigate(
                    JoinGameFragDirections.actionJoinGameFragToViewGameFrag(stub.game.guid))
            } else {
                findNavController().navigate(
                    JoinGameFragDirections.actionJoinGameFragToNewGameFrag(
                        stub.game.guid,
                        NewGameFrag.GUID_GAME
                    ))
            }
        })
        binding.listGames.adapter = gamesAdapter
        vm.availGames.observe(viewLifecycleOwner, Observer {
            Timber.d("Available games to join ${it.size} for ${args.challengeGuid}")
            gamesAdapter.setGames(it)
        })

        binding.executePendingBindings()
        return binding.root
    }

}