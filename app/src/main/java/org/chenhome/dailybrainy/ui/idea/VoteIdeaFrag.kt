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
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.databinding.VoteIdeaFragBinding
import org.chenhome.dailybrainy.databinding.VoteIdeaItemIdeaBinding
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.ui.Event
import org.chenhome.dailybrainy.ui.GameVMFactory
import org.chenhome.dailybrainy.ui.PlayerAdapter
import org.jetbrains.annotations.NotNull
import timber.log.Timber

@AndroidEntryPoint
class VoteIdeaFrag : Fragment() {

    private val args: VoteIdeaFragArgs by navArgs()
    private val vm: IdeaVM by viewModels {
        GameVMFactory(requireContext(), args.gameGuid)
    }

    private val playerAdap = PlayerAdapter()
    private val ideaAdap = VoteIdeaAdapter(VoteIdeaAdapter.Listener { idea ->
        vm.vote.incrementVoteRemotely(idea)
        Timber.d("voted for idea $idea")
    })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val binding = VoteIdeaFragBinding.inflate(LayoutInflater.from(context), container, false)
        binding.vm = vm
        binding.listIdeas.adapter = ideaAdap
        binding.listPlayers.adapter = playerAdap
        binding.lifecycleOwner = viewLifecycleOwner

        binding.executePendingBindings()

        initAdapterObservers(vm.fullGame, ideaAdap, playerAdap)
        initNavObserver(vm.navToNext)
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

    private fun initNavObserver(navToNext: LiveData<Event<Boolean>>) {
        navToNext.observe(viewLifecycleOwner, {
            it.contentIfNotHandled()?.run {
                findNavController().popBackStack()
            }
        })
    }

    private fun initAdapterObservers(
        fullGame: LiveData<FullGame>,
        voteIdeaAdap: VoteIdeaAdapter,
        playerAdap: PlayerAdapter,
    ) {
        fullGame.observe(viewLifecycleOwner, {
            it?.let {
                voteIdeaAdap.ideas = it.ideas(Idea.Origin.BRAINSTORM)
                playerAdap.players = it.players
            }
        })
    }

}

internal class VoteIdeaAdapter(val ideaListener: Listener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var ideas: List<Idea> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        IdeaVH(VoteIdeaItemIdeaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as IdeaVH).bind(ideas[position])
    }

    override fun getItemCount(): Int = ideas.size

    inner class IdeaVH(val binding: @NotNull VoteIdeaItemIdeaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(idea: Idea) {
            binding.idea = idea
            binding.listener = ideaListener
        }
    }

    class Listener(val listener: (Idea) -> Unit) {
        fun onClick(idea: Idea) {
            listener(idea)
        }
    }
}