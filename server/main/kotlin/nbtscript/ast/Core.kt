package nbtscript.ast

import kotlinx.collections.immutable.PersistentList

sealed interface Core {
    data class Root(val body: TermZ) : Core

    sealed interface TypeZ : Core {
        object EndZ : TypeZ
        object ByteZ : TypeZ
        object ShortZ : TypeZ
        object IntZ : TypeZ
        object LongZ : TypeZ
        object FloatZ : TypeZ
        object DoubleZ : TypeZ
        object StringZ : TypeZ
        object ByteArrayZ : TypeZ
        object IntArrayZ : TypeZ
        object LongArrayZ : TypeZ
        data class ListZ(val element: TypeZ) : TypeZ
        data class CompoundZ(val elements: Map<String, TypeZ>) : TypeZ
        object Hole : TypeZ
    }

    sealed interface TermZ : Core {
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

        data class UniverseS(override val type: Value) : TermS
        data class EndS(override val type: Value) : TermS
        data class ByteS(override val type: Value) : TermS
        data class ShortS(override val type: Value) : TermS
        data class IntS(override val type: Value) : TermS
        data class LongS(override val type: Value) : TermS
        data class FloatS(override val type: Value) : TermS
        data class DoubleS(override val type: Value) : TermS
        data class StringS(override val type: Value) : TermS
        data class ByteArrayS(override val type: Value) : TermS
        data class IntArrayS(override val type: Value) : TermS
        data class LongArrayS(override val type: Value) : TermS
        data class ListS(val element: TermS, override val type: Value) : TermS
        data class CompoundS(val elements: Map<String, TermS>, override val type: Value) : TermS
        data class FunctionS(val name: String?, val dom: TermS, val cod: TermS, override val type: Value) : TermS
        data class CodeS(val element: Core.TypeZ, override val type: Value) : TermS
        data class TypeZ(override val type: Value) : TermS
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
        data class Quote(val element: TermZ, override val type: Value) : TermS
        data class Let(val name: String, val init: TermS, val next: TermS, override val type: Value) : TermS
        data class Var(val name: String?, val level: Int, override val type: Value) : TermS
        data class Meta(val index: Int, override val type: Value) : TermS
        data class Hole(override val type: Value) : TermS
    }

    sealed interface Value : Core {
        object UniverseS : Value
        object EndS : Value
        object ByteS : Value
        object ShortS : Value
        object IntS : Value
        object LongS : Value
        object FloatS : Value
        object DoubleS : Value
        object StringS : Value
        object ByteArrayS : Value
        object IntArrayS : Value
        object LongArrayS : Value
        data class ListS(val element: Lazy<Value>) : Value
        data class CompoundS(val elements: Map<String, Lazy<Value>>) : Value
        data class FunctionS(val name: String?, val dom: Lazy<Value>, val cod: Clos) : Value
        data class CodeS(val element: Core.TypeZ) : Value
        object TypeZ : Value
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
        data class Quote(val element: TermZ) : Value
        data class Var(val name: String?, val level: Int, val type: Lazy<Value>) : Value
        data class Meta(val index: Int, val type: Value) : Value
        object Hole : Value
    }

    data class Clos(val env: PersistentList<Lazy<Value>>, val body: Lazy<TermS>) : Core
}
