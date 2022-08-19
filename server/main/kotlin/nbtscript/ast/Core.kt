package nbtscript.ast

import kotlinx.collections.immutable.PersistentList
import nbtscript.ast.Core.Kind.Sem
import nbtscript.ast.Core.Kind.Syn

sealed interface Core {
    class Root(val body: TermZ) : Core

    sealed interface Kind {
        object Syn : Kind
        object Sem : Kind
    }

    sealed interface ObjZ<out K : Kind> : Core

    sealed interface TypeZ<out K : Kind> : ObjZ<K> {
        class EndType<K : Kind> : TypeZ<K>
        class ByteType<K : Kind> : TypeZ<K>
        class ShortType<K : Kind> : TypeZ<K>
        class IntType<K : Kind> : TypeZ<K>
        class LongType<K : Kind> : TypeZ<K>
        class FloatType<K : Kind> : TypeZ<K>
        class DoubleType<K : Kind> : TypeZ<K>
        class StringType<K : Kind> : TypeZ<K>
        open class CollectionType<K : Kind>(open val element: TypeZ<K>) : TypeZ<K>
        class ByteArrayType<K : Kind> : CollectionType<K>(ByteType())
        class IntArrayType<K : Kind> : CollectionType<K>(IntType())
        class LongArrayType<K : Kind> : CollectionType<K>(LongType())
        class ListType<K : Kind>(override val element: TypeZ<K>) : CollectionType<K>(element)
        class CompoundType<K : Kind>(val elements: Map<String, TypeZ<K>>) : TypeZ<K>
        class Splice<K : Kind>(val element: TermS<K>) : TypeZ<K>
        class Hole<K : Kind> : TypeZ<K>
    }

    sealed interface TermZ : ObjZ<Syn> {
        val type: TypeZ<Sem>

        class ByteTag(val data: Byte, override val type: TypeZ<Sem>) : TermZ
        class ShortTag(val data: Short, override val type: TypeZ<Sem>) : TermZ
        class IntTag(val data: Int, override val type: TypeZ<Sem>) : TermZ
        class LongTag(val data: Long, override val type: TypeZ<Sem>) : TermZ
        class FloatTag(val data: Float, override val type: TypeZ<Sem>) : TermZ
        class DoubleTag(val data: Double, override val type: TypeZ<Sem>) : TermZ
        class StringTag(val data: String, override val type: TypeZ<Sem>) : TermZ
        class ByteArrayTag(val elements: List<TermZ>, override val type: TypeZ<Sem>) : TermZ
        class IntArrayTag(val elements: List<TermZ>, override val type: TypeZ<Sem>) : TermZ
        class LongArrayTag(val elements: List<TermZ>, override val type: TypeZ<Sem>) : TermZ
        class ListTag(val elements: List<TermZ>, override val type: TypeZ<Sem>) : TermZ
        class CompoundTag(val elements: Map<String, TermZ>, override val type: TypeZ<Sem>) : TermZ
        class Function(val name: String, val body: TermZ, val next: TermZ, override val type: TypeZ<Sem>) : TermZ
        class Run(val name: String, override val type: TypeZ<Sem>) : TermZ
        class Splice(val element: TermS<Syn>, override val type: TypeZ<Sem>) : TermZ
        class Hole(override val type: TypeZ<Sem>) : TermZ
    }

    sealed interface TermS<out K : Kind> : Core {
        val type: TermS<Sem>

        class UniverseType<K : Kind> : TermS<K> {
            override val type: TermS<Sem> get() = UniverseType()
        }

        class EndType<K : Kind>(override val type: TermS<Sem>) : TermS<K>
        class ByteType<K : Kind>(override val type: TermS<Sem>) : TermS<K>
        class ShortType<K : Kind>(override val type: TermS<Sem>) : TermS<K>
        class IntType<K : Kind>(override val type: TermS<Sem>) : TermS<K>
        class LongType<K : Kind>(override val type: TermS<Sem>) : TermS<K>
        class FloatType<K : Kind>(override val type: TermS<Sem>) : TermS<K>
        class DoubleType<K : Kind>(override val type: TermS<Sem>) : TermS<K>
        class StringType<K : Kind>(override val type: TermS<Sem>) : TermS<K>
        class ByteArrayType<K : Kind>(override val type: TermS<Sem>) : TermS<K>
        class IntArrayType<K : Kind>(override val type: TermS<Sem>) : TermS<K>
        class LongArrayType<K : Kind>(override val type: TermS<Sem>) : TermS<K>
        class ListType(val element: TermS<Syn>, override val type: TermS<Sem>) : TermS<Syn>
        class VListType(val element: Lazy<TermS<Sem>>, override val type: TermS<Sem>) : TermS<Sem>
        class CompoundType(val elements: Map<String, TermS<Syn>>, override val type: TermS<Sem>) : TermS<Syn>
        class VCompoundType(val elements: Map<String, Lazy<TermS<Sem>>>, override val type: TermS<Sem>) : TermS<Sem>
        class FunctionType(val name: String?, val dom: TermS<Syn>, val cod: TermS<Syn>, override val type: TermS<Sem>) : TermS<Syn>
        class VFunctionType(val name: String?, val dom: Lazy<TermS<Sem>>, val cod: Clos, override val type: TermS<Sem>) : TermS<Sem>
        class CodeType(val element: TypeZ<Syn>, override val type: TermS<Sem>) : TermS<Syn>
        class VCodeType(val element: Lazy<TypeZ<Sem>>, override val type: TermS<Sem>) : TermS<Sem>
        class TypeType<K : Kind>(override val type: TermS<Sem>) : TermS<K>
        class EndTag<K : Kind>(override val type: TermS<Sem>) : TermS<K>
        class ByteTag<K : Kind>(val data: Byte, override val type: TermS<Sem>) : TermS<K>
        class ShortTag<K : Kind>(val data: Short, override val type: TermS<Sem>) : TermS<K>
        class IntTag<K : Kind>(val data: Int, override val type: TermS<Sem>) : TermS<K>
        class LongTag<K : Kind>(val data: Long, override val type: TermS<Sem>) : TermS<K>
        class FloatTag<K : Kind>(val data: Float, override val type: TermS<Sem>) : TermS<K>
        class DoubleTag<K : Kind>(val data: Double, override val type: TermS<Sem>) : TermS<K>
        class StringTag<K : Kind>(val data: String, override val type: TermS<Sem>) : TermS<K>
        class ByteArrayTag(val elements: List<TermS<Syn>>, override val type: TermS<Sem>) : TermS<Syn>
        class VByteArrayTag(val elements: List<Lazy<TermS<Sem>>>, override val type: TermS<Sem>) : TermS<Sem>
        class IntArrayTag(val elements: List<TermS<Syn>>, override val type: TermS<Sem>) : TermS<Syn>
        class VIntArrayTag(val elements: List<Lazy<TermS<Sem>>>, override val type: TermS<Sem>) : TermS<Sem>
        class LongArrayTag(val elements: List<TermS<Syn>>, override val type: TermS<Sem>) : TermS<Syn>
        class VLongArrayTag(val elements: List<Lazy<TermS<Sem>>>, override val type: TermS<Sem>) : TermS<Sem>
        class ListTag(val elements: List<TermS<Syn>>, override val type: TermS<Sem>) : TermS<Syn>
        class VListTag(val elements: List<Lazy<TermS<Sem>>>, override val type: TermS<Sem>) : TermS<Sem>
        class CompoundTag(val elements: Map<String, TermS<Syn>>, override val type: TermS<Sem>) : TermS<Syn>
        class VCompoundTag(val elements: Map<String, Lazy<TermS<Sem>>>, override val type: TermS<Sem>) : TermS<Sem>
        class IndexedElement(val target: TermZ, val index: TermS<Syn>, override val type: TermS<Sem>) : TermS<Syn>
        class VIndexedElement(val target: TermZ, val index: Lazy<TermS<Sem>>, override val type: TermS<Sem>) : TermS<Sem>
        class Abs(val name: String, val anno: TermS<Syn>, val body: TermS<Syn>, override val type: TermS<Sem>) : TermS<Syn>
        class VAbs(val name: String, val anno: Lazy<TermS<Sem>>, val body: Clos, override val type: TermS<Sem>) : TermS<Sem>
        class Apply(val operator: TermS<Syn>, val operand: TermS<Syn>, override val type: TermS<Sem>) : TermS<Syn>
        class VApply(val operator: TermS<Sem>, val operand: Lazy<TermS<Sem>>, override val type: TermS<Sem>) : TermS<Sem>
        class QuoteType(val element: TypeZ<Syn>, override val type: TermS<Sem>) : TermS<Syn>
        class VQuoteType(val element: Lazy<TypeZ<Sem>>, override val type: TermS<Sem>) : TermS<Sem>
        class QuoteTerm<K : Kind>(val element: TermZ, override val type: TermS<Sem>) : TermS<K>
        class Let(val name: String, val init: TermS<Syn>, val next: TermS<Syn>, override val type: TermS<Sem>) : TermS<Syn>
        class Var<K : Kind>(val name: String?, val level: Int, override val type: TermS<Sem>) : TermS<K>
        class Meta<K : Kind>(val index: Int, override val type: TermS<Sem>) : TermS<K>
        class Hole<K : Kind>(override val type: TermS<Sem>) : TermS<K>
    }

    class Clos(val env: PersistentList<Lazy<TermS<Sem>>>, val body: Lazy<TermS<Syn>>) : Core
}
