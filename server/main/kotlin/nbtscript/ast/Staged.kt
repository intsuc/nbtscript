package nbtscript.ast

sealed interface Staged {
    data class Root(val body: Term) : Staged

    sealed interface Term : Staged {
        data class ByteTag(val data: Byte) : Term
        data class ShortTag(val data: Short) : Term
        data class IntTag(val data: Int) : Term
        data class LongTag(val data: Long) : Term
        data class FloatTag(val data: Float) : Term
        data class DoubleTag(val data: Double) : Term
        data class StringTag(val data: String) : Term
        data class ByteArrayTag(val elements: List<Term>) : Term
        data class IntArrayTag(val elements: List<Term>) : Term
        data class LongArrayTag(val elements: List<Term>) : Term
        data class ListTag(val elements: List<Term>) : Term
        data class CompoundTag(val elements: Map<String, Term>) : Term
        data class IndexedElement(val target: Term, val index: Int) : Term
        data class Function(val name: String, val body: Term, val next: Term) : Term
        data class Run(val name: String) : Term
        object Hole : Term
    }
}
