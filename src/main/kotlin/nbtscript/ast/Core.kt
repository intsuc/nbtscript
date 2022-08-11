package nbtscript.ast

sealed interface Core {
    data class Root(val body: TermZ) : Core

    sealed interface TypeZ : Core {
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
        data class ListZ(val elements: List<TypeZ>) : TypeZ
        data class CompoundZ(val elements: Map<String, TypeZ>) : TypeZ
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
    }

    sealed interface TermS : Core {
        val type: ValueS

        data class TypeS(override val type: ValueS) : TermS
        data class ByteS(override val type: ValueS) : TermS
        data class ShortS(override val type: ValueS) : TermS
        data class IntS(override val type: ValueS) : TermS
        data class LongS(override val type: ValueS) : TermS
        data class FloatS(override val type: ValueS) : TermS
        data class DoubleS(override val type: ValueS) : TermS
        data class StringS(override val type: ValueS) : TermS
        data class ByteArrayS(override val type: ValueS) : TermS
        data class IntArrayS(override val type: ValueS) : TermS
        data class LongArrayS(override val type: ValueS) : TermS
        data class ListS(val elements: List<TermS>, override val type: ValueS) : TermS
        data class CompoundS(val elements: Map<String, TermS>, override val type: ValueS) : TermS
        data class FunctionS(val name: String, val dom: TermS, val cod: TermS, override val type: ValueS) : TermS
        data class CodeS(val element: Core.TypeZ, override val type: ValueS) : TermS
        data class TypeZ(override val type: ValueS) : TermS
        data class ByteTag(val data: Byte, override val type: ValueS) : TermS
        data class ShortTag(val data: Short, override val type: ValueS) : TermS
        data class IntTag(val data: Int, override val type: ValueS) : TermS
        data class LongTag(val data: Long, override val type: ValueS) : TermS
        data class FloatTag(val data: Float, override val type: ValueS) : TermS
        data class DoubleTag(val data: Double, override val type: ValueS) : TermS
        data class StringTag(val data: String, override val type: ValueS) : TermS
        data class ByteArrayTag(val elements: List<TermS>, override val type: ValueS) : TermS
        data class IntArrayTag(val elements: List<TermS>, override val type: ValueS) : TermS
        data class LongArrayTag(val elements: List<TermS>, override val type: ValueS) : TermS
        data class ListTag(val elements: List<TermS>, override val type: ValueS) : TermS
        data class CompoundTag(val elements: Map<String, TermS>, override val type: ValueS) : TermS
        data class Function(val name: String, val body: TermS, override val type: ValueS) : TermS
        data class Run(val function: TermS, val argument: TermS, override val type: ValueS) : TermS
        data class Quote(val element: TermZ, override val type: ValueS) : TermS
        data class Let(val name: String, val init: TermS, val next: TermS, override val type: ValueS) : TermS
        data class Var(val name: String, override val type: ValueS) : TermS
    }

    sealed interface ValueS : Core {
        object TypeS : ValueS
        object ByteS : ValueS
        object ShortS : ValueS
        object IntS : ValueS
        object LongS : ValueS
        object FloatS : ValueS
        object DoubleS : ValueS
        object StringS : ValueS
        object ByteArrayS : ValueS
        object IntArrayS : ValueS
        object LongArrayS : ValueS
        data class ListS(val elements: List<Lazy<ValueS>>) : ValueS
        data class CompoundS(val elements: Map<String, Lazy<ValueS>>) : ValueS
        data class FunctionS(val name: String, val dom: Lazy<ValueS>, val cod: TermS) : ValueS
        data class CodeS(val element: Core.TypeZ) : ValueS
        object TypeZ : ValueS
        data class ByteTag(val data: Byte) : ValueS
        data class ShortTag(val data: Short) : ValueS
        data class IntTag(val data: Int) : ValueS
        data class LongTag(val data: Long) : ValueS
        data class FloatTag(val data: Float) : ValueS
        data class DoubleTag(val data: Double) : ValueS
        data class StringTag(val data: String) : ValueS
        data class ByteArrayTag(val elements: List<Lazy<ValueS>>) : ValueS
        data class IntArrayTag(val elements: List<Lazy<ValueS>>) : ValueS
        data class LongArrayTag(val elements: List<Lazy<ValueS>>) : ValueS
        data class ListTag(val elements: List<Lazy<ValueS>>) : ValueS
        data class CompoundTag(val elements: Map<String, Lazy<ValueS>>) : ValueS
        data class Function(val name: String, val body: TermS) : ValueS
        data class Run(val function: Lazy<ValueS>, val argument: Lazy<ValueS>) : ValueS
        data class Quote(val element: TermZ) : ValueS
        data class Var(val name: String) : ValueS
    }
}