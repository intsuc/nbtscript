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
        val type: Value

        data class UniverseType(override val type: Value) : TermS
        data class EndType(override val type: Value) : TermS
        data class ByteType(override val type: Value) : TermS
        data class ShortType(override val type: Value) : TermS
        data class IntType(override val type: Value) : TermS
        data class LongType(override val type: Value) : TermS
        data class FloatType(override val type: Value) : TermS
        data class DoubleType(override val type: Value) : TermS
        data class StringType(override val type: Value) : TermS
        data class ByteArrayType(override val type: Value) : TermS
        data class IntArrayType(override val type: Value) : TermS
        data class LongArrayType(override val type: Value) : TermS
        data class ListType(val element: TermS, override val type: Value) : TermS
        data class CompoundType(val elements: Map<String, TermS>, override val type: Value) : TermS
        data class FunctionType(val name: String?, val dom: TermS, val cod: TermS, override val type: Value) : TermS
        data class CodeType(val element: TypeZ, override val type: Value) : TermS
        data class TypeType(override val type: Value) : TermS
        data class EndTag(override val type: Value) : TermS
        data class ByteTag(val data: Byte, override val type: Value) : TermS
        data class ShortTag(val data: Short, override val type: Value) : TermS
        data class IntTag(val data: Int, override val type: Value) : TermS
        data class LongTag(val data: Long, override val type: Value) : TermS
        data class FloatTag(val data: Float, override val type: Value) : TermS
        data class DoubleTag(val data: Double, override val type: Value) : TermS
        data class StringTag(val data: String, override val type: Value) : TermS
        data class ByteArrayTag(val elements: List<TermS>, override val type: Value) : TermS
        data class IntArrayTag(val elements: List<TermS>, override val type: Value) : TermS
        data class LongArrayTag(val elements: List<TermS>, override val type: Value) : TermS
        data class ListTag(val elements: List<TermS>, override val type: Value) : TermS
        data class CompoundTag(val elements: Map<String, TermS>, override val type: Value) : TermS
        data class IndexedElement(val target: TermZ, val index: TermS, override val type: Value) : TermS
        data class Abs(val name: String, val anno: TermS, val body: TermS, override val type: Value) : TermS
        data class Apply(val operator: TermS, val operand: TermS, override val type: Value) : TermS
        data class QuoteType(val element: TypeZ, override val type: Value) : TermS
        data class QuoteTerm(val element: TermZ, override val type: Value) : TermS
        data class Let(val name: String, val init: TermS, val next: TermS, override val type: Value) : TermS
        data class Var(val name: String?, val level: Int, override val type: Value) : TermS
        data class Meta(val index: Int, override val type: Value) : TermS
        data class Hole(override val type: Value) : TermS
    }

    sealed interface Value : Core {
        object UniverseType : Value
        object EndType : Value
        object ByteType : Value
        object ShortType : Value
        object IntType : Value
        object LongType : Value
        object FloatType : Value
        object DoubleType : Value
        object StringType : Value
        object ByteArrayType : Value
        object IntArrayType : Value
        object LongArrayType : Value
        data class ListType(val element: Lazy<Value>) : Value
        data class CompoundType(val elements: Map<String, Lazy<Value>>) : Value
        data class FunctionType(val name: String?, val dom: Lazy<Value>, val cod: Clos) : Value
        data class CodeType(val element: TypeZ) : Value
        object TypeType : Value
        object EndTag : Value
        data class ByteTag(val data: Byte) : Value
        data class ShortTag(val data: Short) : Value
        data class IntTag(val data: Int) : Value
        data class LongTag(val data: Long) : Value
        data class FloatTag(val data: Float) : Value
        data class DoubleTag(val data: Double) : Value
        data class StringTag(val data: String) : Value
        data class ByteArrayTag(val elements: List<Lazy<Value>>) : Value
        data class IntArrayTag(val elements: List<Lazy<Value>>) : Value
        data class LongArrayTag(val elements: List<Lazy<Value>>) : Value
        data class ListTag(val elements: List<Lazy<Value>>) : Value
        data class CompoundTag(val elements: Map<String, Lazy<Value>>) : Value
        data class IndexedElement(val target: TermZ, val index: Lazy<Value>, val type: Value) : Value
        data class Abs(val name: String, val anno: Lazy<Value>, val body: Clos) : Value
        data class Apply(val operator: Value, val operand: Lazy<Value>) : Value
        data class QuoteType(val element: TypeZ) : Value
        data class QuoteTerm(val element: TermZ) : Value
        data class Var(val name: String?, val level: Int, val type: Lazy<Value>) : Value
        data class Meta(val index: Int, val type: Value) : Value
        object Hole : Value
    }

    data class Clos(val env: PersistentList<Lazy<Value>>, val body: Lazy<TermS>) : Core
}
