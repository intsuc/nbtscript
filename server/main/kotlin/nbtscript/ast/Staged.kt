package nbtscript.ast

sealed interface Staged {
    class Root(val body: Term)

    sealed interface Term {
        class ByteTag(val data: Byte) : Term
        class ShortTag(val data: Short) : Term
        class IntTag(val data: Int) : Term
        class LongTag(val data: Long) : Term
        class FloatTag(val data: Float) : Term
        class DoubleTag(val data: Double) : Term
        class StringTag(val data: String) : Term
        class ByteArrayTag(val elements: List<Term>) : Term
        class IntArrayTag(val elements: List<Term>) : Term
        class LongArrayTag(val elements: List<Term>) : Term
        class ListTag(val elements: List<Term>) : Term
        class CompoundTag(val elements: Map<String, Term>) : Term
        class IndexedElement(val target: Term, val index: Int) : Term
        class Fun(val name: String, val body: Term, val next: Term) : Term
        class Run(val name: String) : Term
        object Hole : Term
    }
}
