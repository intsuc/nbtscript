package nbtscript

inline fun <reified A, reified B> cast(a: A): B = a as B

@Suppress("NOTHING_TO_INLINE")
inline fun unreachable(): Nothing = error("unreachable")
