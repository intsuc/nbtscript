package nbtscript.phase

@JvmInline
value class Messages(
    private val messages: MutableList<Message> = mutableListOf(),
) {
    operator fun plusAssign(message: Message) {
        messages += message
    }
}
