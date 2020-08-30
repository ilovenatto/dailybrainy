package org.chenhome.dailybrainy.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.databinding.ViewGameFragBinding
import org.chenhome.dailybrainy.databinding.ViewGameItemPlayerBinding
import org.chenhome.dailybrainy.databinding.ViewGameItemStepBinding
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.PlayerSession
import org.chenhome.dailybrainy.ui.bindImage
import org.jetbrains.annotations.NotNull
import timber.log.Timber


@AndroidEntryPoint // injecting viewmodels with hilt
class ViewGameFrag : Fragment() {

    private val args: ViewGameFragArgs by navArgs()
    private val vm: ViewGameVM by viewModels {
        ViewGameVMFactory(requireContext(), args.gameGuid)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val stepAdap = StepAdapter(StepAdapter.StepListener { step ->
            // Fragment listens to this
            vm.navToStep(step)
        })
        val playerAdap = PlayerAdapter()

        val binding = ViewGameFragBinding.inflate(LayoutInflater.from(context), container, false)

        with(binding.listSteps) {
            adapter = stepAdap
            layoutManager = LinearLayoutManager(context)
        }
        with(binding.listPlayers) {
            adapter = playerAdap
            layoutManager = GridLayoutManager(context, 5)
        }

        // observe current step & players
        vm.fullGame.observe(viewLifecycleOwner, Observer {
            it?.let {
                stepAdap.setCurrentStep(it.game.currentStep)
                playerAdap.setPlayers(it.players)
                binding.vm = vm
                binding.executePendingBindings()

            }
        })
        vm.challengeImgUri.observe(viewLifecycleOwner) {
            Timber.d("Observed challenge img uri $it")
            // specially handle the image
            bindImage(binding.imageChallenge, it)
        }

        vm.navToStep.observe(viewLifecycleOwner, Observer {
            it.contentIfNotHandled()?.run {
                val dir: NavDirections = when (this) {
                    Challenge.Step.GEN_IDEA -> ViewGameFragDirections.actionViewGameFragToGenIdeaFrag(
                        vm.gameGuid)
                    // TODO: 8/29/20  put in other directions
                    else -> throw Exception("Encountered unsupported Challenge step $this")
                }
                Timber.d("Navigating to $dir")
                findNavController().navigate(dir)
            }
        })
        binding.executePendingBindings()
        return binding.root
    }

}

internal class PlayerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var players: MutableList<PlayerSession> = mutableListOf()
    fun setPlayers(value: MutableList<PlayerSession>) {
        players = value
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return PlayerVH(ViewGameItemPlayerBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as PlayerVH).bind(players[position])
    }

    override fun getItemCount(): Int = players.size

    class PlayerVH(val binding: @NotNull ViewGameItemPlayerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(player: PlayerSession) {
            binding.player = player
            binding.playerAvatar.setImageResource(player.avatarImage().imgResId)
        }
    }

}

internal class StepAdapter(val stepListener: StepListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val steps = Challenge.Step.values()
    private var currentStep: Challenge.Step = Challenge.Step.GEN_IDEA

    fun setCurrentStep(step: Challenge.Step) {
        currentStep = step
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return StepVH(
            ViewGameItemStepBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // assume position lines up with Enum's ordinal number
        (holder as StepVH).bind(steps[position])
    }

    override fun getItemCount(): Int = steps.size


    inner class StepVH(val binding: @NotNull ViewGameItemStepBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(step: Challenge.Step) {
            binding.step = step
            binding.listener = stepListener
        }
    }

    class StepListener(val listener: (Challenge.Step) -> Unit) {
        // Called by item's layout XML onClick attribute
        fun onClick(step: Challenge.Step) {
            listener(step)
        }
    }
}


