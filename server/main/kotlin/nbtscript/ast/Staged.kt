package nbtscript.ast

sealed interface Staged {
    data class Root(val body: TermZ) : Staged

    sealed interface TermZ : Staged {
        data class ByteTag(val data: Byte) : TermZ
        data class ShortTag(val data: Short) : TermZ
        data class IntTag(val data: Int) : TermZ
        data class LongTag(val data: Long) : TermZ
        data class FloatTag(val data: Float) : TermZ
        data class DoubleTag(val data: Double) : TermZ
        data class StringTag(val data: String) : TermZ
        data class ByteArrayTag(val elements: List<TermZ>) : TermZ
        data class IntArrayTag(val elements: List<TermZ>) : TermZ
        data class LongArrayTag(val elements: List<TermZ>) : TermZ
        data class ListTag(val elements: List<TermZ>) : TermZ
        data class CompoundTag(val elements: Map<String, TermZ>) : TermZ
        data class Function(val name: String, val body: TermZ, val next: TermZ) : TermZ
        data class Run(val name: String) : TermZ
        object Hole : TermZ
    }
}
