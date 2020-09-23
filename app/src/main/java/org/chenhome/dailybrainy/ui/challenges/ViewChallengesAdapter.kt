package org.chenhome.dailybrainy.ui.challenges

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.chenhome.dailybrainy.R
import org.chenhome.dailybrainy.databinding.*
import org.chenhome.dailybrainy.repo.Challenge
import org.chenhome.dailybrainy.repo.game.GameStub
import org.chenhome.dailybrainy.repo.game.Lesson
import timber.log.Timber


/**
 * Listener invoked when item managed by [ViewChallengesAdapter] is clicked.
 */
class ChallengeListener(
    val joinListener: (challengeGuid: String) -> Unit,
    val newGameListener: (challengeGuid: String) -> Unit,
) {
    fun onJoinGame(challenge: Challenge) =
        if (challenge.category == Challenge.Category.CHALLENGE) joinListener(challenge.guid)
        else Timber.w("Challenge is the wrong category")

    fun onNewGame(challenge: Challenge) =
        if (challenge.category == Challenge.Category.CHALLENGE) newGameListener(challenge.guid)
        else Timber.w("Wrong category")
}

class LessonListener(val viewListener: (challengeGuid: String) -> Unit) {
    fun onViewLesson(lesson: Lesson) =
        if (lesson.challenge.category == Challenge.Category.LESSON) viewListener(lesson.challenge.guid)
        else Timber.w("Wrong category")
}

class GameStubListener(val clickListener: (gameStub: GameStub, view: View) -> Unit) {
    fun onClick(gameStub: GameStub, view: View) = clickListener(gameStub, view)
}

/**
 * Adapts different kinds of data to a list. They include a text header, GameStub and Challenge and Lesson.
 *
 * @property gameListener
 */
class ViewChallengesAdapter(
    val context: Context,
    private val gameListener: GameStubListener,
    private val lessonListener: LessonListener,
    private val challengeListener: ChallengeListener,
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var challenge: Challenge? = null
    private var lesson: Lesson? = null
    private var games: List<GameStub>? = null

    init {
        // called at instantiation so we can build list with placeholder cards
        setAllItems()
    }

    // Create heterogeneous list of Challenge, Lesson, Headers and Games
    private var allItems: MutableList<Any> = mutableListOf()

    // Called whenever the underlying data has changed
    private fun setAllItems() {
        // List is
        // header: "Today's challenges"
        // challenge
        // lesson
        // header: "Past challenges"
        // games[]
        allItems.apply {
            clear()
            add(context.getString(R.string.today_challenge))
            add(challenge ?: PlaceholderDummy(
                context.getString(R.string.challenge),
                context.getString(R.string.not_available)))
            add(lesson ?: PlaceholderDummy(
                context.getString(R.string.design_lesson),
                context.getString(R.string.not_available)))
            add(context.getString(R.string.past_challenge))
            if (games.isNullOrEmpty()) {
                add(PlaceholderDummy(
                    context.getString(R.string.past_challenge),
                    context.getString(R.string.not_available)))
            } else {
                addAll(games!!)
            }
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            HeaderVH.TYPE -> HeaderVH(CompHeaderBinding.inflate(inflater, parent, false))
            CardPlaceholderVH.TYPE -> CardPlaceholderVH(CardPlaceholderBinding.inflate(inflater,
                parent,
                false))
            CardLessonVH.TYPE -> CardLessonVH(CardLessonBinding.inflate(inflater, parent, false))
            CardChallengeVH.TYPE -> CardChallengeVH(CardChallengeBinding.inflate(inflater,
                parent,
                false))
            CardGameVH.TYPE -> CardGameVH(CardGameBinding.inflate(inflater, parent, false))
            else -> {
                Timber.w("Creating placeholder for unknown/unsupported view type")
                CardPlaceholderVH(CardPlaceholderBinding.inflate(inflater, parent, false))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position < 0 || position >= allItems.size) return

        val item = allItems[position]
        when (holder) {
            is HeaderVH -> holder.bind(item as String)
            is CardChallengeVH -> holder.bind(item as Challenge,
                challengeListener)
            is CardLessonVH -> holder.bind(item as Lesson, lessonListener)
            is CardGameVH -> holder.bind(item as GameStub, gameListener)
            is CardPlaceholderVH -> holder.bind(item as PlaceholderDummy)
            else -> Timber.d("No binding necessary for View holder of type ${holder.javaClass}")
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position >= allItems.size || position < 0) {
            return TYPE_UNKNOWN
        }
        return when (allItems[position]) {
            is String -> HeaderVH.TYPE
            is PlaceholderDummy -> CardPlaceholderVH.TYPE
            is Lesson -> CardLessonVH.TYPE
            is Challenge -> CardChallengeVH.TYPE
            is GameStub -> CardGameVH.TYPE
            else -> TYPE_UNKNOWN
        }
    }

    override fun getItemCount(): Int = allItems.size


    /**
     * Public setters for its data. Adapter notifies observers of these changes
     */
    fun setGames(gameStubs: List<GameStub>) {
        games = gameStubs
        setAllItems()
    }

    fun setTodayChallenge(challenge: Challenge) {
        this.challenge = challenge
        setAllItems()
    }

    fun setTodayLesson(lesson: Lesson) {
        this.lesson = lesson
        setAllItems()
    }

    /**
     * View holders for different kinds of data
     */
    companion object {
        const val TYPE_UNKNOWN = 0
    }
}

class CardLessonVH(val binding: CardLessonBinding) :
    RecyclerView.ViewHolder(binding.root) {
    companion object {
        const val TYPE = 100
    }

    fun bind(lesson: Lesson, listener: LessonListener) {
        binding.lesson = lesson
        binding.listener = listener
        binding.executePendingBindings()
    }
}

class CardChallengeVH(val binding: CardChallengeBinding) :
    RecyclerView.ViewHolder(binding.root) {
    companion object {
        const val TYPE = 101
    }

    fun bind(challenge: Challenge, listener: ChallengeListener) {
        binding.challenge = challenge
        binding.listener = listener
        binding.executePendingBindings()
    }
}

class CardGameVH(val binding: CardGameBinding) :
    RecyclerView.ViewHolder(binding.root) {
    companion object {
        const val TYPE = 102
    }

    fun bind(gameStub: GameStub, listener: GameStubListener) {
        Timber.d("Binding $gameStub")
        binding.gameStub = gameStub
        binding.listener = listener
        binding.executePendingBindings()
    }
}

class HeaderVH(val binding: CompHeaderBinding) :
    RecyclerView.ViewHolder(binding.root) {
    companion object {
        const val TYPE = 103
    }

    fun bind(title: String) {
        binding.title = title
        binding.executePendingBindings()
    }
}

class CardPlaceholderVH(val binding: CardPlaceholderBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(dummy: PlaceholderDummy) {
        binding.dummy = dummy
    }

    companion object {
        const val TYPE = 104
    }
}

// Dummy class to mark a Placeholder data item
data class PlaceholderDummy(
    val title: String,
    val desc: String, // desc optional
)
