package nbtscript.ast

import org.eclipse.lsp4j.Range

sealed interface Surface {
    val range: Range

    data class Root(val body: Term, override val range: Range) : Surface

    sealed interface Term : Surface {
        data class UniverseType(override val range: Range) : Term
        data class EndType(override val range: Range) : Term
        data class ByteType(override val range: Range) : Term
        data class ShortType(override val range: Range) : Term
        data class IntType(override val range: Range) : Term
        data class LongType(override val range: Range) : Term
        data class FloatType(override val range: Range) : Term
        data class DoubleType(override val range: Range) : Term
        data class StringType(override val range: Range) : Term
        data class ByteArrayType(override val range: Range) : Term
        data class IntArrayType(override val range: Range) : Term
        data class LongArrayType(override val range: Range) : Term
        data class ListType(val element: Term, override val range: Range) : Term
        data class CompoundType(val elements: Map<Name, Term>, override val range: Range) : Term
        data class FunctionType(val name: Name?, val dom: Term, val cod: Term, override val range: Range) : Term
        data class CodeType(val element: Term, override val range: Range) : Term
        data class TypeType(override val range: Range) : Term
        data class ByteTag(val data: Byte, override val range: Range) : Term
        data class ShortTag(val data: Short, override val range: Range) : Term
        data class IntTag(val data: Int, override val range: Range) : Term
        data class LongTag(val data: Long, override val range: Range) : Term
        data class FloatTag(val data: Float, override val range: Range) : Term
        data class DoubleTag(val data: Double, override val range: Range) : Term
        data class StringTag(val data: String, override val range: Range) : Term
        data class ByteArrayTag(val elements: List<Term>, override val range: Range) : Term
        data class IntArrayTag(val elements: List<Term>, override val range: Range) : Term
        data class LongArrayTag(val elements: List<Term>, override val range: Range) : Term
        data class ListTag(val elements: List<Term>, override val range: Range) : Term
        data class CompoundTag(val elements: Map<Name, Term>, override val range: Range) : Term
        data class IndexedElement(val target: Term, val index: Term, override val range: Range) : Term
        data class Abs(val name: Name, val anno: Term, val body: Term, override val range: Range) : Term
        data class Apply(val operator: Term, val operand: Term, override val range: Range) : Term
        data class Quote(val element: Term, override val range: Range) : Term
        data class Splice(val element: Term, override val range: Range) : Term
        data class Let(val name: Name, val anno: Term?, val init: Term, val next: Term, override val range: Range) : Term
        data class Function(val name: Name, val anno: Term?, val body: Term, val next: Term, override val range: Range) : Term
        data class Var(val name: String, override val range: Range) : Term
        data class Hole(override val range: Range) : Term
    }

    data class Name(val text: String, override val range: Range) : Surface
}
