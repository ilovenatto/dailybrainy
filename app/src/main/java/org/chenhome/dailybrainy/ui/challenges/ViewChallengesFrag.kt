package org.chenhome.dailybrainy.ui.challenges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.R
import org.chenhome.dailybrainy.databinding.ViewChallengesFragBinding
import org.chenhome.dailybrainy.ui.game.NewGameFrag
import timber.log.Timber

@AndroidEntryPoint // for injecting ViewModel "by viewModels()"
class ViewChallengesFrag : Fragment() {

    private val vm: ViewChallengesVM by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = ViewChallengesFragBinding.inflate(LayoutInflater.from(requireContext()),
            container,
            false)

        // Previous games
        val gamesAdapter = ViewGamesAdapter(GameListener { stub, view ->
            val txnName = getString(R.string.transition_gamecard)
            val extras = FragmentNavigatorExtras(view to txnName)
            val dir =
                ViewChallengesFragDirections.actionViewChallengesFragToViewGameFrag(stub.game.guid)
            findNavController().navigate(dir, extras)
        })
        binding.listGames.adapter = gamesAdapter

        // Today challenge
        binding.vm = vm
        binding.listenerChallenge = ChallengeListener(
            { guid -> // onJoin
                vm.navToJoinGame(guid)
            },
            { guid -> // newGame
                vm.navToNewGame(guid)
            }
        )
        binding.listenerLesson = LessonListener { guid ->
            vm.navToLesson(guid)
        }

        // observe ViewModel
        vm.games.observe(viewLifecycleOwner, Observer {
            it?.let {
                Timber.d("Observed ${it.size} games changed")
                gamesAdapter.setGames(it)
            }
        })

        vm.todayChallenge.observe(viewLifecycleOwner, Observer {
            binding.vm = vm
        })

        vm.todayLesson.observe(viewLifecycleOwner, Observer {
            binding.vm = vm
        })

        vm.navToNewGame.observe(viewLifecycleOwner, {
            // navigate
            it.contentIfNotHandled()?.let { challengeGuid ->
                val dir =
                    ViewChallengesFragDirections.actionViewChallengesFragToNewGameFrag(
                        challengeGuid, NewGameFrag.GUID_CHALLENGE)
                this.findNavController().navigate(dir)
            }
        })

        vm.navToLesson.observe(viewLifecycleOwner, {
            // navigate
            it.contentIfNotHandled()?.let { challengeGuid ->
                val dir =
                    ViewChallengesFragDirections.actionViewChallengesFragToLessonFrag(challengeGuid)
                this.findNavController().navigate(dir)
            }
        })

        vm.navToJoinGame.observe(viewLifecycleOwner, {
            it.contentIfNotHandled()?.let { challengeGuid ->
                this.findNavController()
                    .navigate(ViewChallengesFragDirections.actionViewChallengesFragToJoinGameFrag(
                        challengeGuid))
            }
        })
        binding.executePendingBindings()
        return binding.root
    }
}
