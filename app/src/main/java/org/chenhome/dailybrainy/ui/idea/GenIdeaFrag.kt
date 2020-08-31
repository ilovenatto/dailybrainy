package org.chenhome.dailybrainy.ui.idea

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.databinding.GenIdeaFragBinding
import org.chenhome.dailybrainy.databinding.GenIdeaItemIdeaBinding
import org.chenhome.dailybrainy.repo.Idea
import org.chenhome.dailybrainy.repo.game.FullGame
import org.chenhome.dailybrainy.ui.Event
import org.chenhome.dailybrainy.ui.PlayerAdapter
import org.jetbrains.annotations.NotNull
import timber.log.Timber

@AndroidEntryPoint
class GenIdeaFrag : Fragment() {

    private val args: GenIdeaFragArgs by navArgs()
    private val vm: GenIdeaVM by viewModels {
        GenIdeaVMFactory(requireContext(), args.gameGuid)
    }

    private val playerAdap = PlayerAdapter()
    private val ideaAdap = IdeaAdapter(IdeaAdapter.Listener {
        Timber.d("selected idea ${it.title}") // do nothing
    })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val binding = GenIdeaFragBinding.inflate(LayoutInflater.from(context), container, false)
        binding.vm = vm
        binding.listIdeas.adapter = ideaAdap
        binding.listPlayers.adapter = playerAdap
        binding.executePendingBindings()

        initAdapterObservers(vm.fullGame, ideaAdap, playerAdap)
        initNavObserver(vm.navToNext)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        Timber.d("Starting timer")
        vm.countdownTimer.start()
    }

    override fun onPause() {
        super.onPause()
        vm.countdownTimer.cancel()
    }

    private fun initNavObserver(navToNext: LiveData<Event<Boolean>>) {
        navToNext.observe(viewLifecycleOwner, Observer {
            it.contentIfNotHandled()?.run {
                findNavController().navigate(GenIdeaFragDirections.actionGenIdeaFragToVoteIdeaFrag(
                    args.gameGuid))
            }
        })
    }

    private fun initAdapterObservers(
        fullGame: LiveData<FullGame>,
        ideaAdap: IdeaAdapter,
        playerAdap: PlayerAdapter,
    ) {
        fullGame.observe(viewLifecycleOwner, Observer {
            it?.let {
                Timber.d("Got challenge ${it.challenge.title}")
                ideaAdap.ideas = it.ideas
                playerAdap.setPlayers(it.players)
            }
        })
    }

}

internal class IdeaAdapter(val ideaListener: Listener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var ideas: MutableList<Idea> = mutableListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        IdeaVH(GenIdeaItemIdeaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as IdeaVH).bind(ideas[position])
    }

    override fun getItemCount(): Int = ideas.size

    inner class IdeaVH(val binding: @NotNull GenIdeaItemIdeaBinding) :
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