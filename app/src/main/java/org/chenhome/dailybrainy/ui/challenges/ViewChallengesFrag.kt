package org.chenhome.dailybrainy.ui.challenges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.R
import org.chenhome.dailybrainy.databinding.ViewChallengesFragBinding
import org.chenhome.dailybrainy.repo.game.Lesson
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

        // Listeners
        val listenerChallenge = ChallengeListener(
            { challengeGuid -> // onJoin
                findNavController()
                    .navigate(ViewChallengesFragDirections
                        .actionViewChallengesFragToJoinGameFrag(
                            challengeGuid))
            },
            { challengeGuid -> // newGame
                findNavController().navigate(ViewChallengesFragDirections
                    .actionViewChallengesFragToNewGameFrag(
                        challengeGuid, NewGameFrag.GUID_CHALLENGE))
            }
        )
        val listenerLesson = LessonListener { challengeGuid ->
            findNavController().navigate(ViewChallengesFragDirections
                .actionViewChallengesFragToLessonFrag(challengeGuid))
        }
        val listenerGame = GameStubListener { stub, view ->
            val txnName = getString(R.string.transition_gamecard)
            val extras = FragmentNavigatorExtras(view to txnName)
            val dir =
                ViewChallengesFragDirections.actionViewChallengesFragToViewGameFrag(stub.game.guid)
            findNavController().navigate(dir, extras)
        }

        // Adapter
        val adapter = ViewChallengesAdapter(requireContext(),
            listenerGame,
            listenerLesson,
            listenerChallenge)
        binding.list.adapter = adapter

        // observe ViewModel and update adapter
        vm.games.observe(viewLifecycleOwner, {
            it?.let {
                Timber.d("Observed ${it.size} games changed")
                adapter.setGames(it)
            }
        })

        vm.todayChallenge.observe(viewLifecycleOwner, {
            it?.let { adapter.setTodayChallenge(it) }
        })

        vm.todayLesson.observe(viewLifecycleOwner, {
            it?.let { adapter.setTodayLesson(Lesson(it)) }
        })

        binding.executePendingBindings()
        return binding.root
    }
}
