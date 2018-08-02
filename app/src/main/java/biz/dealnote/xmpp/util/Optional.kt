package biz.dealnote.xmpp.util

class Optional<T> private constructor(private val value: T?) {

    val isEmpty: Boolean
        get() = value == null

    fun get(): T? {
        return value
    }

    fun nonEmpty(): Boolean {
        return value != null
    }

    companion object {

        fun <T> wrap(value: T?): Optional<T?> {
            return Optional(value)
        }

        fun <T> empty(): Optional<T?> {
            return Optional(null)
        }
    }
}