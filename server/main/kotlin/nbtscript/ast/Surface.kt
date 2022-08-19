package nbtscript.ast

import org.eclipse.lsp4j.Range

sealed interface Surface {
    val range: Range

    class Root(val body: Term, override val range: Range) : Surface

    sealed interface Term : Surface {
        class UniverseType(override val range: Range) : Term
        class EndType(override val range: Range) : Term
        class ByteType(override val range: Range) : Term
        class ShortType(override val range: Range) : Term
        class IntType(override val range: Range) : Term
        class LongType(override val range: Range) : Term
        class FloatType(override val range: Range) : Term
        class DoubleType(override val range: Range) : Term
        class StringType(override val range: Range) : Term
        class CollectionType(val element: Term, override val range: Range) : Term
        class ByteArrayType(override val range: Range) : Term
        class IntArrayType(override val range: Range) : Term
        class LongArrayType(override val range: Range) : Term
        class ListType(val element: Term, override val range: Range) : Term
        class CompoundType(val elements: Map<Name, Term>, override val range: Range) : Term
        class FunctionType(val name: Name?, val dom: Term, val cod: Term, override val range: Range) : Term
        class CodeType(val element: Term, override val range: Range) : Term
        class TypeType(override val range: Range) : Term
        class ByteTag(val data: Byte, override val range: Range) : Term
        class ShortTag(val data: Short, override val range: Range) : Term
        class IntTag(val data: Int, override val range: Range) : Term
        class LongTag(val data: Long, override val range: Range) : Term
        class FloatTag(val data: Float, override val range: Range) : Term
        class DoubleTag(val data: Double, override val range: Range) : Term
        class StringTag(val data: String, override val range: Range) : Term
        class ByteArrayTag(val elements: List<Term>, override val range: Range) : Term
        class IntArrayTag(val elements: List<Term>, override val range: Range) : Term
        class LongArrayTag(val elements: List<Term>, override val range: Range) : Term
        class ListTag(val elements: List<Term>, override val range: Range) : Term
        class CompoundTag(val elements: Map<Name, Term>, override val range: Range) : Term
        class IndexedElement(val target: Term, val index: Term, override val range: Range) : Term
        class Abs(val name: Name, val anno: Term?, val body: Term, override val range: Range) : Term
        class Apply(val operator: Term, val operand: Term, override val range: Range) : Term
        class Quote(val element: Term, override val range: Range) : Term
        class Splice(val element: Term, override val range: Range) : Term
        class Let(val name: Name, val anno: Term?, val init: Term, val next: Term, override val range: Range) : Term
        class Function(val name: Name, val anno: Term?, val body: Term, val next: Term, override val range: Range) : Term
        class Var(val name: String, override val range: Range) : Term
        class Hole(override val range: Range) : Term
    }

    class Name(val text: String, override val range: Range) : Surface
}
