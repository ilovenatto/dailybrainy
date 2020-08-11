package org.chenhome.dailybrainy.repo

/**
 * @param T type of the singleton
 * @param A parameter that's being injected
 * @constructor
 *
 * @param creator lambda that instantiates the singleton type
 */
open class SingletonHolder<out T : Any, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator
    @Volatile
    private var instance: T? = null

    fun singleton(arg: A): T {
        val i = instance
        // use smart cast to check it's null and of the right singleton type
        if (i != null) return i

        // Synchronize on static companion object (there's only one)
        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }
}