package nbtscript.phase

fun interface Phase<A, B> {
    operator fun invoke(messages: Messages, input: A): B
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun <A, B, C> Phase<A, B>.rangeTo(other: Phase<B, C>): Phase<A, C> = Phase { messages, a ->
    other(messages, this(messages, a))
}
