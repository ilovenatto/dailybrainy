package org.chenhome.dailybrainy.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.chenhome.dailybrainy.R
import org.chenhome.dailybrainy.databinding.CardAvatarBinding
import org.chenhome.dailybrainy.databinding.NewGameFragBinding
import org.chenhome.dailybrainy.repo.image.AvatarImage


@AndroidEntryPoint
class NewGameFrag : Fragment() {

    /**
     * Denotes the kind of object that the Guid is referring to
     * [NewGameFragArgs.guid]. This value is set as the argument
     * [NewGameFragArgs.guidType]
     */
    companion object {
        const val GUID_CHALLENGE: Int = 0
        const val GUID_GAME: Int = 1
    }

    // data bind the edit text
    private val viewModel: NewGameVM by viewModels()
    private val args: NewGameFragArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val binding = NewGameFragBinding.inflate(inflater, container, false)

        binding.listAvatars.adapter = AvatarAdapter()
        binding.vm = viewModel

        // onBack and onSave
        binding.toolbar.apply {
            setNavigationOnClickListener {
                findNavController().popBackStack()
            }
            setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.menuitem_done) {
                    if (args.guid.isNotEmpty() && args.guidType >= 0) {
                        when (args.guidType) {
                            GUID_CHALLENGE -> viewModel.onNavNewGame(args.guid)
                            GUID_GAME -> viewModel.onNavExistingGame(args.guid)
                        }
                    }
                    true
                } else {
                    false
                }
            }
        }
        // set lifecycle so that 2-way data binding works w/ LiveData
        binding.lifecycleOwner = viewLifecycleOwner
        binding.executePendingBindings()

        with(viewModel) {
            // Only enable Done action when form input is valid
            valid.observe(viewLifecycleOwner, { valid ->
                binding.toolbar.menu.findItem(R.id.menuitem_done)?.isEnabled = valid
            })

            navToGame.observe(viewLifecycleOwner, {
                // navigate
                it.contentIfNotHandled()?.let { gameGuid ->
                    findNavController().navigate(
                        NewGameFragDirections.actionNewGameFragToViewGameFrag(
                            gameGuid
                        )
                    )
                }
            })
            showError.observe(viewLifecycleOwner, {
                it.contentIfNotHandled()?.let { error ->
                    Toast.makeText(context, error.titleResId, Toast.LENGTH_SHORT).show()
                }
            })
        }
        return binding.root
    }


    inner class AvatarAdapter :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val avatars = AvatarImage.values().map {
            SelectableAvatar(it, false)
        }

        inner class AvatarVH(val binding: CardAvatarBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(avatar: SelectableAvatar) {
                binding.avatar = avatar
                binding.avatarCard.isChecked = avatar.checked
                binding.listener = AvatarListener()
                binding.avatarImage.setImageResource(avatar.avatar.imgResId)
            }
        }


        inner class AvatarListener {
            // Called by item's layout XML onClick attribute
            fun onClick(avatar: SelectableAvatar) {
                // select just the clicked avatar
                avatars.forEach {
                    it.checked = it.avatar == avatar.avatar
                }
                // rebind
                viewModel.onAvatarSelected(avatar.avatar)
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return AvatarVH(
                CardAvatarBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as AvatarVH).bind(avatars[position])
        }

        override fun getItemCount(): Int = avatars.size
    }

    // Avatar decorated with isChecked state
    data class SelectableAvatar(
        val avatar: AvatarImage,
        var checked: Boolean,
    )
}
