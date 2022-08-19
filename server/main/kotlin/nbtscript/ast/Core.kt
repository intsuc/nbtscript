package nbtscript.ast

import kotlinx.collections.immutable.PersistentList

sealed interface Core {
    data class Root(val body: TermZ) : Core

    sealed interface ObjZ : Core

    sealed interface TypeZ : ObjZ {
        object EndType : TypeZ
        object ByteType : TypeZ
        object ShortType : TypeZ
        object IntType : TypeZ
        object LongType : TypeZ
        object FloatType : TypeZ
        object DoubleType : TypeZ
        object StringType : TypeZ
        open class CollectionType(open val element: TypeZ) : TypeZ
        object ByteArrayType : CollectionType(ByteType)
        object IntArrayType : CollectionType(IntType)
        object LongArrayType : CollectionType(LongType)
        data class ListType(override val element: TypeZ) : CollectionType(element)
        data class CompoundType(val elements: Map<String, TypeZ>) : TypeZ
        data class Splice(val element: TermS) : TypeZ
        object Hole : TypeZ
    }

    sealed interface TermZ : ObjZ {
        val type: TypeZ

        data class ByteTag(val data: Byte, override val type: TypeZ) : TermZ
        data class ShortTag(val data: Short, override val type: TypeZ) : TermZ
        data class IntTag(val data: Int, override val type: TypeZ) : TermZ
        data class LongTag(val data: Long, override val type: TypeZ) : TermZ
        data class FloatTag(val data: Float, override val type: TypeZ) : TermZ
        data class DoubleTag(val data: Double, override val type: TypeZ) : TermZ
        data class StringTag(val data: String, override val type: TypeZ) : TermZ
        data class ByteArrayTag(val elements: List<TermZ>, override val type: TypeZ) : TermZ
        data class IntArrayTag(val elements: List<TermZ>, override val type: TypeZ) : TermZ
        data class LongArrayTag(val elements: List<TermZ>, override val type: TypeZ) : TermZ
        data class ListTag(val elements: List<TermZ>, override val type: TypeZ) : TermZ
        data class CompoundTag(val elements: Map<String, TermZ>, override val type: TypeZ) : TermZ
        data class Function(val name: String, val body: TermZ, val next: TermZ, override val type: TypeZ) : TermZ
        data class Run(val name: String, override val type: TypeZ) : TermZ
        data class Splice(val element: TermS, override val type: TypeZ) : TermZ
        data class Hole(override val type: TypeZ) : TermZ
    }

    sealed interface TermS : Core {
        val type: VTermS

        data class UniverseType(override val type: VTermS) : TermS
        data class EndType(override val type: VTermS) : TermS
        data class ByteType(override val type: VTermS) : TermS
        data class ShortType(override val type: VTermS) : TermS
        data class IntType(override val type: VTermS) : TermS
        data class LongType(override val type: VTermS) : TermS
        data class FloatType(override val type: VTermS) : TermS
        data class DoubleType(override val type: VTermS) : TermS
        data class StringType(override val type: VTermS) : TermS
        data class ByteArrayType(override val type: VTermS) : TermS
        data class IntArrayType(override val type: VTermS) : TermS
        data class LongArrayType(override val type: VTermS) : TermS
        data class ListType(val element: TermS, override val type: VTermS) : TermS
        data class CompoundType(val elements: Map<String, TermS>, override val type: VTermS) : TermS
        data class FunctionType(val name: String?, val dom: TermS, val cod: TermS, override val type: VTermS) : TermS
        data class CodeType(val element: TypeZ, override val type: VTermS) : TermS
        data class TypeType(override val type: VTermS) : TermS
        data class EndTag(override val type: VTermS) : TermS
        data class ByteTag(val data: Byte, override val type: VTermS) : TermS
        data class ShortTag(val data: Short, override val type: VTermS) : TermS
        data class IntTag(val data: Int, override val type: VTermS) : TermS
        data class LongTag(val data: Long, override val type: VTermS) : TermS
        data class FloatTag(val data: Float, override val type: VTermS) : TermS
        data class DoubleTag(val data: Double, override val type: VTermS) : TermS
        data class StringTag(val data: String, override val type: VTermS) : TermS
        data class ByteArrayTag(val elements: List<TermS>, override val type: VTermS) : TermS
        data class IntArrayTag(val elements: List<TermS>, override val type: VTermS) : TermS
        data class LongArrayTag(val elements: List<TermS>, override val type: VTermS) : TermS
        data class ListTag(val elements: List<TermS>, override val type: VTermS) : TermS
        data class CompoundTag(val elements: Map<String, TermS>, override val type: VTermS) : TermS
        data class IndexedElement(val target: TermZ, val index: TermS, override val type: VTermS) : TermS
        data class Abs(val name: String, val anno: TermS, val body: TermS, override val type: VTermS) : TermS
        data class Apply(val operator: TermS, val operand: TermS, override val type: VTermS) : TermS
        data class QuoteType(val element: TypeZ, override val type: VTermS) : TermS
        data class QuoteTerm(val element: TermZ, override val type: VTermS) : TermS
        data class Let(val name: String, val init: TermS, val next: TermS, override val type: VTermS) : TermS
        data class Var(val name: String?, val level: Int, override val type: VTermS) : TermS
        data class Meta(val index: Int, override val type: VTermS) : TermS
        data class Hole(override val type: VTermS) : TermS
    }

    sealed interface VTermS : Core {
        object UniverseType : VTermS
        object EndType : VTermS
        object ByteType : VTermS
        object ShortType : VTermS
        object IntType : VTermS
        object LongType : VTermS
        object FloatType : VTermS
        object DoubleType : VTermS
        object StringType : VTermS
        object ByteArrayType : VTermS
        object IntArrayType : VTermS
        object LongArrayType : VTermS
        data class ListType(val element: Lazy<VTermS>) : VTermS
        data class CompoundType(val elements: Map<String, Lazy<VTermS>>) : VTermS
        data class FunctionType(val name: String?, val dom: Lazy<VTermS>, val cod: Clos) : VTermS
        data class CodeType(val element: TypeZ) : VTermS
        object TypeType : VTermS
        object EndTag : VTermS
        data class ByteTag(val data: Byte) : VTermS
        data class ShortTag(val data: Short) : VTermS
        data class IntTag(val data: Int) : VTermS
        data class LongTag(val data: Long) : VTermS
        data class FloatTag(val data: Float) : VTermS
        data class DoubleTag(val data: Double) : VTermS
        data class StringTag(val data: String) : VTermS
        data class ByteArrayTag(val elements: List<Lazy<VTermS>>) : VTermS
        data class IntArrayTag(val elements: List<Lazy<VTermS>>) : VTermS
        data class LongArrayTag(val elements: List<Lazy<VTermS>>) : VTermS
        data class ListTag(val elements: List<Lazy<VTermS>>) : VTermS
        data class CompoundTag(val elements: Map<String, Lazy<VTermS>>) : VTermS
        data class IndexedElement(val target: TermZ, val index: Lazy<VTermS>, val type: VTermS) : VTermS
        data class Abs(val name: String, val anno: Lazy<VTermS>, val body: Clos) : VTermS
        data class Apply(val operator: VTermS, val operand: Lazy<VTermS>) : VTermS
        data class QuoteType(val element: TypeZ) : VTermS
        data class QuoteTerm(val element: TermZ) : VTermS
        data class Var(val name: String?, val level: Int, val type: Lazy<VTermS>) : VTermS
        data class Meta(val index: Int, val type: VTermS) : VTermS
        object Hole : VTermS
    }

    data class Clos(val env: PersistentList<Lazy<VTermS>>, val body: Lazy<TermS>) : Core
}
