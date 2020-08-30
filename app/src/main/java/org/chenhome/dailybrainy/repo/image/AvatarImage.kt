package org.chenhome.dailybrainy.repo.image

import org.chenhome.dailybrainy.R

/**
 * Facade over local avatar drawables. [AvatarImage.name] will be the unique identifier for each drawable.
 *
 * @property imgResId
 */
enum class AvatarImage(
    val imgResId: Int
) {
    A1(R.drawable.avatar1),
    A2(R.drawable.avatar2),
    A3(R.drawable.avatar3),
    A4(R.drawable.avatar4),
    A5(R.drawable.avatar5),
    A6(R.drawable.avatar6),
    PLACEHOLDER(R.drawable.avatar1)
}
