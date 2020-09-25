package org.chenhome.dailybrainy.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.databinding.ViewGameFragBinding
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.ui.GameVMFactory
import org.chenhome.dailybrainy.ui.PlayerSheetAdapter
import timber.log.Timber


@AndroidEntryPoint // injecting view models with hilt
class ViewGameFrag : Fragment() {

    private val args: ViewGameFragArgs by navArgs()
    private val vm: ViewGameVM by viewModels {
        GameVMFactory(requireContext(), args.gameGuid)
    }
    private val stepAdap = GameStepAdapter(GameStepAdapter.GameStepListener { step ->
        // Fragment listens to this
        vm.navToStep(step)
    })
    private val playerSheetAdapter = PlayerSheetAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // set up motion
        sharedElementEnterTransition = MaterialContainerTransform()

        val binding = ViewGameFragBinding
            .inflate(LayoutInflater.from(context), container, false)
        with(binding.listSteps) {
            addItemDecoration(DividerItemDecoration(requireContext(),
                DividerItemDecoration.VERTICAL))
            adapter = stepAdap
        }
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // observe steps
        vm.gameSteps.observe(viewLifecycleOwner, { steps ->
            val current = steps.find { it.isCurrentStep }
            Timber.d("Steps updated. Current step is $current")
            stepAdap.setSteps(requireContext(), steps)
        })

        // observe current step & players
        with(binding.avatars) {
            listPlayers.adapter = playerSheetAdapter.playerAdapter
            listThumbs.adapter = playerSheetAdapter.thumbAdapter
        }
        vm.fullGame.observe(viewLifecycleOwner, {
            it?.let {
                playerSheetAdapter.setGame(it)
                binding.vm = vm
                binding.executePendingBindings()
            }
        })

        vm.navToStep.observe(viewLifecycleOwner, {
            it.contentIfNotHandled()?.run {
                val dir: NavDirections = when (this) {
                    Challenge.Step.GEN_IDEA -> ViewGameFragDirections
                        .actionViewGameFragToGenIdeaFrag(vm.gameGuid)
                    Challenge.Step.VOTE_IDEA -> ViewGameFragDirections
                        .actionViewGameFragToVoteIdeaFrag(vm.gameGuid)
                    Challenge.Step.GEN_SKETCH -> ViewGameFragDirections
                        .actionViewGameFragToGenSketchFrag(vm.gameGuid)
                    Challenge.Step.VOTE_SKETCH -> ViewGameFragDirections
                        .actionViewGameFragToVoteSketchFrag(vm.gameGuid)
                    Challenge.Step.CREATE_STORYBOARD -> ViewGameFragDirections
                        .actionViewGameFragToCreateStoryFrag(vm.gameGuid)
                    Challenge.Step.VIEW_STORYBOARD -> ViewGameFragDirections
                        .actionViewGameFragToReviewStoryFrag(vm.gameGuid)
                }
                Timber.d("Navigating to $dir")
                findNavController().navigate(dir)
            }
        })
        binding.executePendingBindings()
        return binding.root
    }
}




