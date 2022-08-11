package nbtscript.ast

import org.eclipse.lsp4j.Range

sealed interface Surface {
    val range: Range

    data class Root(val body: TermZ, override val range: Range) : Surface

    sealed interface TypeZ : Surface {
        data class ByteZ(override val range: Range) : TypeZ
        data class ShortZ(override val range: Range) : TypeZ
        data class IntZ(override val range: Range) : TypeZ
        data class LongZ(override val range: Range) : TypeZ
        data class FloatZ(override val range: Range) : TypeZ
        data class DoubleZ(override val range: Range) : TypeZ
        data class StringZ(override val range: Range) : TypeZ
        data class ByteArrayZ(override val range: Range) : TypeZ
        data class IntArrayZ(override val range: Range) : TypeZ
        data class LongArrayZ(override val range: Range) : TypeZ
        data class ListZ(val element: TypeZ, override val range: Range) : TypeZ
        data class CompoundZ(val elements: Map<String, TypeZ>, override val range: Range) : TypeZ
    }

    sealed interface TermZ : Surface {
        data class ByteTag(val data: Byte, override val range: Range) : TermZ
        data class ShortTag(val data: Short, override val range: Range) : TermZ
        data class IntTag(val data: Int, override val range: Range) : TermZ
        data class LongTag(val data: Long, override val range: Range) : TermZ
        data class FloatTag(val data: Float, override val range: Range) : TermZ
        data class DoubleTag(val data: Double, override val range: Range) : TermZ
        data class StringTag(val data: String, override val range: Range) : TermZ
        data class ByteArrayTag(val elements: List<TermZ>, override val range: Range) : TermZ
        data class IntArrayTag(val elements: List<TermZ>, override val range: Range) : TermZ
        data class LongArrayTag(val elements: List<TermZ>, override val range: Range) : TermZ
        data class ListTag(val elements: List<TermZ>, override val range: Range) : TermZ
        data class CompoundTag(val elements: Map<String, TermZ>, override val range: Range) : TermZ
        data class Function(val name: String, val body: TermZ, val next: TermZ, override val range: Range) : TermZ
        data class Run(val name: String, override val range: Range) : TermZ
        data class Splice(val element: TermS, override val range: Range) : TermZ
    }

    sealed interface TermS : Surface {
        data class TypeS(override val range: Range) : TermS
        data class ByteS(override val range: Range) : TermS
        data class ShortS(override val range: Range) : TermS
        data class IntS(override val range: Range) : TermS
        data class LongS(override val range: Range) : TermS
        data class FloatS(override val range: Range) : TermS
        data class DoubleS(override val range: Range) : TermS
        data class StringS(override val range: Range) : TermS
        data class ByteArrayS(override val range: Range) : TermS
        data class IntArrayS(override val range: Range) : TermS
        data class LongArrayS(override val range: Range) : TermS
        data class ListS(val element: TermS, override val range: Range) : TermS
        data class CompoundS(val elements: Map<String, TermS>, override val range: Range) : TermS
        data class FunctionS(val name: String, val dom: TermS, val cod: TermS, override val range: Range) : TermS
        data class CodeS(val element: Surface.TypeZ, override val range: Range) : TermS
        data class TypeZ(override val range: Range) : TermS
        data class ByteTag(val data: Byte, override val range: Range) : TermS
        data class ShortTag(val data: Short, override val range: Range) : TermS
        data class IntTag(val data: Int, override val range: Range) : TermS
        data class LongTag(val data: Long, override val range: Range) : TermS
        data class FloatTag(val data: Float, override val range: Range) : TermS
        data class DoubleTag(val data: Double, override val range: Range) : TermS
        data class StringTag(val data: String, override val range: Range) : TermS
        data class ByteArrayTag(val elements: List<TermS>, override val range: Range) : TermS
        data class IntArrayTag(val elements: List<TermS>, override val range: Range) : TermS
        data class LongArrayTag(val elements: List<TermS>, override val range: Range) : TermS
        data class ListTag(val elements: List<TermS>, override val range: Range) : TermS
        data class CompoundTag(val elements: Map<String, TermS>, override val range: Range) : TermS
        data class Function(val name: String, val body: TermS, override val range: Range) : TermS
        data class Run(val function: TermS, val argument: TermS, override val range: Range) : TermS
        data class Quote(val element: TermZ, override val range: Range) : TermS
        data class Let(val name: String, val init: TermS, val next: TermS, override val range: Range) : TermS
        data class Var(val name: String, override val range: Range) : TermS
    }
}
