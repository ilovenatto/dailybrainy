package org.chenhome.dailybrainy.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.databinding.NewGameFragBinding
import org.chenhome.dailybrainy.databinding.NewGameItemAvatarBinding
import org.chenhome.dailybrainy.repo.image.AvatarImage
import org.jetbrains.annotations.NotNull

@AndroidEntryPoint
class NewGameFrag : Fragment() {

    // data bind the edit text
    private val viewModel: NewGameVM by viewModels()
    private val args: NewGameFragArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = NewGameFragBinding.inflate(inflater, container, false)
        with(binding.listAvatars) {
            layoutManager = GridLayoutManager(context, 2)
            adapter = AvatarAdapter(AvatarAdapter.AvatarListener {
                viewModel.onAvatarSelected(it)
            })
        }
        binding.vm = viewModel
        binding.challengeGuid = args.challengeGuid
        // set lifecycle so that 2-way data binding works w/ LiveData
        binding.lifecycleOwner = viewLifecycleOwner
        binding.executePendingBindings()

        // init view model
        with(viewModel) {
            navToGame.observe(viewLifecycleOwner, Observer {
                // navigate
                it.contentIfNotHandled()?.let { gameGuid ->
                    findNavController().navigate(
                        NewGameFragDirections.actionNewGameFragToViewGameFrag(
                            gameGuid
                        )
                    )
                }
            })
            showError.observe(viewLifecycleOwner, Observer {
                it.contentIfNotHandled()?.let {
                    Toast.makeText(context, it.titleResId, Toast.LENGTH_SHORT).show()
                }
            })
        }
        return binding.root
    }

}

class AvatarAdapter(val listener: AvatarListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val avatars = AvatarImage.values()

    inner class AvatarVH(val binding: @NotNull NewGameItemAvatarBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(avatar: AvatarImage) {
            binding.avatar = avatar
            binding.listener = listener
            binding.avatarImage.setImageResource(avatar.imgResId)
        }
    }

    class AvatarListener(val listener: (avatar: AvatarImage) -> Unit) {
        // Called by item's layout XML onClick attribute
        fun onClick(avatar: AvatarImage) = listener(avatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return AvatarVH(
            NewGameItemAvatarBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as AvatarVH).bind(avatars[position])
    }

    override fun getItemCount(): Int = avatars.size
}
