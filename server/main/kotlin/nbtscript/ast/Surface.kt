package nbtscript.ast

import org.eclipse.lsp4j.Range

sealed interface Surface {
    val range: Range

    class Root(val body: Term, override val range: Range) : Surface

    sealed interface TypeZ : Term
    sealed interface TermZ : Term
    sealed interface TermS : Term
    sealed interface TermM : Term

    sealed interface Term : Surface {
        class UniverseType(override val range: Range) : TermS
        class EndType(override val range: Range) : TermS
        class ByteType(override val range: Range) : TypeZ, TermS
        class ShortType(override val range: Range) : TypeZ, TermS
        class IntType(override val range: Range) : TypeZ, TermS
        class LongType(override val range: Range) : TypeZ, TermS
        class FloatType(override val range: Range) : TypeZ, TermS
        class DoubleType(override val range: Range) : TypeZ, TermS
        class StringType(override val range: Range) : TypeZ, TermS
        class CollectionType(val element: Term, override val range: Range) : TypeZ
        class ByteArrayType(override val range: Range) : TypeZ, TermS
        class IntArrayType(override val range: Range) : TypeZ, TermS
        class LongArrayType(override val range: Range) : TypeZ, TermS
        class ListType(val element: Term, override val range: Range) : TypeZ, TermS
        class CompoundType(val elements: Map<Name, Term>, override val range: Range) : TypeZ, TermS
        class NodeType(override val range: Range) : TermS
        class FunType(val name: Name?, val dom: Term, val cod: Term, override val range: Range) : TermS
        class CodeType(val element: Term, override val range: Range) : TermS
        class MacroType(val element: Term, override val range: Range) : TermM
        class TypeType(override val range: Range) : TermS
        class ByteTag(val data: Byte, override val range: Range) : TermZ, TermS
        class ShortTag(val data: Short, override val range: Range) : TermZ, TermS
        class IntTag(val data: Int, override val range: Range) : TermZ, TermS
        class LongTag(val data: Long, override val range: Range) : TermZ, TermS
        class FloatTag(val data: Float, override val range: Range) : TermZ, TermS
        class DoubleTag(val data: Double, override val range: Range) : TermZ, TermS
        class StringTag(val data: String, override val range: Range) : TermZ, TermS
        class ByteArrayTag(val elements: List<Term>, override val range: Range) : TermZ, TermS
        class IntArrayTag(val elements: List<Term>, override val range: Range) : TermZ, TermS
        class LongArrayTag(val elements: List<Term>, override val range: Range) : TermZ, TermS
        class ListTag(val elements: List<Term>, override val range: Range) : TermZ, TermS
        class CompoundTag(val elements: Map<Name, Term>, override val range: Range) : TermZ, TermS
        class Get(val target: Term, val path: Term, override val range: Range) : TermS
        class Abs(val name: Name, val anno: Term?, val body: Term, override val range: Range) : TermS
        class Apply(val operator: Term, val operand: Term, override val range: Range) : TermS
        class Quote(val element: Term, override val range: Range) : TermS
        class Splice(val element: Term, override val range: Range) : TypeZ, TermZ
        class Lift(val element: Term, override val range: Range) : TermM
        class Unlift(val element: Term, override val range: Range) : TypeZ, TermZ, TermS
        class Fun(val name: Name, val anno: Term?, val body: Term, val next: Term, override val range: Range) : TermZ
        class Let(val name: Name, val anno: Term?, val body: Term, val next: Term, override val range: Range) : TermS
        class Mac(val name: Name, val anno: Term?, val body: Term, val next: Term, override val range: Range) : TermM
        class Var(val name: String, override val range: Range) : TermZ, TermS
        class Hole(override val range: Range) : Term
    }

    class Name(val text: String, override val range: Range) : Surface
}
