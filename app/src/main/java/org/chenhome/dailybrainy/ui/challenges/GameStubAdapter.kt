package org.chenhome.dailybrainy.ui.challenges

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.chenhome.dailybrainy.databinding.CardGameBinding
import org.chenhome.dailybrainy.repo.game.GameStub

class GameStubAdapter(
    private val gameListener: GameStubListener,
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var games = listOf<GameStub>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return CardGameVH(CardGameBinding
            .inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as CardGameVH).bind(games[position], gameListener)
    }

    override fun getItemCount(): Int = games.size

    fun setGames(gameStubs: List<GameStub>) {
        this.games = gameStubs
        notifyDataSetChanged()
    }

}