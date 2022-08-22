package nbts.ast

import kotlinx.collections.immutable.PersistentList
import nbts.ast.Core.Kind.*

sealed interface Core {
    class Root(val body: TermZ)

    sealed interface Kind {
        sealed interface Syn : Kind
        sealed interface Sem : Kind
        sealed interface End : Syn, Sem
    }

    sealed interface TypeZ<out K : Kind> {
        object EndType : TypeZ<End>
        object ByteType : TypeZ<End>
        object ShortType : TypeZ<End>
        object IntType : TypeZ<End>
        object LongType : TypeZ<End>
        object FloatType : TypeZ<End>
        object DoubleType : TypeZ<End>
        object StringType : TypeZ<End>
        class CollectionType<K : Kind>(val element: TypeZ<K>) : TypeZ<K>
        object ByteArrayType : TypeZ<End>
        object IntArrayType : TypeZ<End>
        object LongArrayType : TypeZ<End>
        class ListType<K : Kind>(val element: TypeZ<K>) : TypeZ<K>
        class CompoundType<K : Kind>(val elements: Map<String, TypeZ<K>>) : TypeZ<K>
        class Splice<K : Kind>(val element: TermS<K>) : TypeZ<K>
        object Hole : TypeZ<End>
    }

    sealed interface TermZ {
        val type: TypeZ<Sem>

        class ByteTag(val data: Byte) : TermZ {
            override val type: TypeZ<Sem> get() = TypeZ.ByteType
        }

        class ShortTag(val data: Short) : TermZ {
            override val type: TypeZ<Sem> get() = TypeZ.ShortType
        }

        class IntTag(val data: Int) : TermZ {
            override val type: TypeZ<Sem> get() = TypeZ.IntType
        }

        class LongTag(val data: Long) : TermZ {
            override val type: TypeZ<Sem> get() = TypeZ.LongType
        }

        class FloatTag(val data: Float) : TermZ {
            override val type: TypeZ<Sem> get() = TypeZ.FloatType
        }

        class DoubleTag(val data: Double) : TermZ {
            override val type: TypeZ<Sem> get() = TypeZ.DoubleType
        }

        class StringTag(val data: String) : TermZ {
            override val type: TypeZ<Sem> get() = TypeZ.StringType
        }

        class ByteArrayTag(val elements: List<TermZ>) : TermZ {
            override val type: TypeZ<Sem> get() = TypeZ.ByteArrayType
        }

        class IntArrayTag(val elements: List<TermZ>) : TermZ {
            override val type: TypeZ<Sem> get() = TypeZ.IntArrayType
        }

        class LongArrayTag(val elements: List<TermZ>) : TermZ {
            override val type: TypeZ<Sem> get() = TypeZ.LongArrayType
        }

        class ListTag(val elements: List<TermZ>, override val type: TypeZ<Sem>) : TermZ

        class CompoundTag(val elements: Map<String, TermZ>, override val type: TypeZ<Sem>) : TermZ

        class Fun(val name: String, val body: TermZ, val next: TermZ, override val type: TypeZ<Sem>) : TermZ

        class Run(val name: String, override val type: TypeZ<Sem>) : TermZ

        class Splice(val element: TermS<Syn>, override val type: TypeZ<Sem>) : TermZ

        class Hole(override val type: TypeZ<Sem>) : TermZ
    }

    sealed interface TermS<out K : Kind> {
        val type: TermS<Sem>

        object UniverseType : TermS<End> {
            override val type: TermS<Sem> get() = this
        }

        object EndType : TermS<End> {
            override val type: TermS<Sem> get() = UniverseType
        }

        object ByteType : TermS<End> {
            override val type: TermS<Sem> get() = UniverseType
        }

        object ShortType : TermS<End> {
            override val type: TermS<Sem> get() = UniverseType
        }

        object IntType : TermS<End> {
            override val type: TermS<Sem> get() = UniverseType
        }

        object LongType : TermS<End> {
            override val type: TermS<Sem> get() = UniverseType
        }

        object FloatType : TermS<End> {
            override val type: TermS<Sem> get() = UniverseType
        }

        object DoubleType : TermS<End> {
            override val type: TermS<Sem> get() = UniverseType
        }

        object StringType : TermS<End> {
            override val type: TermS<Sem> get() = UniverseType
        }

        object ByteArrayType : TermS<End> {
            override val type: TermS<Sem> get() = UniverseType
        }

        object IntArrayType : TermS<End> {
            override val type: TermS<Sem> get() = UniverseType
        }

        object LongArrayType : TermS<End> {
            override val type: TermS<Sem> get() = UniverseType
        }

        class ListType(val element: TermS<Syn>) : TermS<Syn> {
            override val type: TermS<Sem> get() = UniverseType
        }

        class VListType(val element: Lazy<TermS<Sem>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = UniverseType
        }

        class CompoundType(val elements: Map<String, TermS<Syn>>) : TermS<Syn> {
            override val type: TermS<Sem> get() = UniverseType
        }

        class VCompoundType(val elements: Map<String, Lazy<TermS<Sem>>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = UniverseType
        }

        object NodeType : TermS<End> {
            override val type: TermS<Sem> get() = UniverseType
        }

        class FunType(val name: String?, val dom: TermS<Syn>, val cod: TermS<Syn>) : TermS<Syn> {
            override val type: TermS<Sem> get() = UniverseType
        }

        class VFunType(val name: String?, val dom: Lazy<TermS<Sem>>, val cod: Clos) : TermS<Sem> {
            override val type: TermS<Sem> get() = UniverseType
        }

        class CodeZType(val element: TypeZ<Syn>) : TermS<Syn> {
            override val type: TermS<Sem> get() = UniverseType
        }

        class VCodeZType(val element: Lazy<TypeZ<Sem>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = UniverseType
        }

        class CodeSType(val element: TermS<Syn>) : TermS<Syn> {
            override val type: TermS<Sem> get() = UniverseType
        }

        class VCodeSType(val element: Lazy<TermS<Sem>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = UniverseType
        }

        object TypeType : TermS<End> {
            override val type: TermS<Sem> get() = UniverseType
        }

        object EndTag : TermS<End> {
            override val type: TermS<Sem> get() = EndType
        }

        class ByteTag(val data: Byte) : TermS<End> {
            override val type: TermS<Sem> get() = ByteType
        }

        class ShortTag(val data: Short) : TermS<End> {
            override val type: TermS<Sem> get() = ShortType
        }

        class IntTag(val data: Int) : TermS<End> {
            override val type: TermS<Sem> get() = IntType
        }

        class LongTag(val data: Long) : TermS<End> {
            override val type: TermS<Sem> get() = LongType
        }

        class FloatTag(val data: Float) : TermS<End> {
            override val type: TermS<Sem> get() = FloatType
        }

        class DoubleTag(val data: Double) : TermS<End> {
            override val type: TermS<Sem> get() = DoubleType
        }

        class StringTag(val data: String) : TermS<End> {
            override val type: TermS<Sem> get() = StringType
        }

        class ByteArrayTag(val elements: List<TermS<Syn>>) : TermS<Syn> {
            override val type: TermS<Sem> get() = ByteArrayType
        }

        class VByteArrayTag(val elements: List<Lazy<TermS<Sem>>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = ByteArrayType
        }

        class IntArrayTag(val elements: List<TermS<Syn>>) : TermS<Syn> {
            override val type: TermS<Sem> get() = IntArrayType
        }

        class VIntArrayTag(val elements: List<Lazy<TermS<Sem>>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = IntArrayType
        }

        class LongArrayTag(val elements: List<TermS<Syn>>) : TermS<Syn> {
            override val type: TermS<Sem> get() = LongArrayType
        }

        class VLongArrayTag(val elements: List<Lazy<TermS<Sem>>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = LongArrayType
        }

        class ListTag(val elements: List<TermS<Syn>>, override val type: TermS<Sem>) : TermS<Syn>

        class VListTag(val elements: List<Lazy<TermS<Sem>>>, override val type: TermS<Sem>) : TermS<Sem>

        class CompoundTag(val elements: Map<String, TermS<Syn>>, override val type: TermS<Sem>) : TermS<Syn>

        class VCompoundTag(val elements: Map<String, Lazy<TermS<Sem>>>, override val type: TermS<Sem>) : TermS<Sem>

        class MatchObjectNode(val pattern: TermS<Syn>) : TermS<Syn> {
            override val type: TermS<Sem> get() = NodeType
        }

        class VMatchObjectNode(val pattern: Lazy<TermS<Sem>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = NodeType
        }

        class MatchElementNode(val pattern: TermS<Syn>) : TermS<Syn> {
            override val type: TermS<Sem> get() = NodeType
        }

        class VMatchElementNode(val pattern: Lazy<TermS<Sem>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = NodeType
        }

        object AllElementsNode : TermS<End> {
            override val type: TermS<Sem> get() = NodeType
        }

        class IndexedElementNode(val index: TermS<Syn>) : TermS<Syn> {
            override val type: TermS<Sem> get() = NodeType
        }

        class VIndexedElementNode(val index: Lazy<TermS<Sem>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = NodeType
        }

        class CompoundChildNode(val name: TermS<Syn>) : TermS<Syn> {
            override val type: TermS<Sem> get() = NodeType
        }

        class VCompoundChildNode(val name: Lazy<TermS<Sem>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = NodeType
        }

        class Get(val target: TermS<Syn>, val path: TermS<Syn>, override val type: TermS<Sem>) : TermS<Syn>

        class VGet(val target: Lazy<TermS<Sem>>, val path: Lazy<TermS<Sem>>, override val type: TermS<Sem>) : TermS<Sem>

        class Abs(val name: String, val anno: TermS<Syn>, val body: TermS<Syn>, override val type: TermS<Sem>) : TermS<Syn>

        class VAbs(val name: String, val anno: Lazy<TermS<Sem>>, val body: Clos, override val type: TermS<Sem>) : TermS<Sem>

        class Apply(val operator: TermS<Syn>, val operand: TermS<Syn>, override val type: TermS<Sem>) : TermS<Syn>

        class VApply(val operator: TermS<Sem>, val operand: Lazy<TermS<Sem>>, override val type: TermS<Sem>) : TermS<Sem>

        class QuoteTypeZ(val element: TypeZ<Syn>) : TermS<Syn> {
            override val type: TermS<Sem> get() = TypeType
        }

        class VQuoteTypeZ(val element: Lazy<TypeZ<Sem>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = TypeType
        }

        class QuoteTermZ(val element: TermZ, override val type: TermS<Sem>) : TermS<End>

        class QuoteTermS(val element: TermS<Syn>, override val type: TermS<Sem>) : TermS<Syn>

        class VQuoteTermS(val element: Lazy<TermS<Sem>>, override val type: TermS<Sem>) : TermS<Sem>

        class Splice(val element: TermS<Syn>, override val type: TermS<Sem>) : TermS<Syn>

        class VSplice(val element: Lazy<TermS<Sem>>, override val type: TermS<Sem>) : TermS<Sem>

        class Let(val name: String, val init: TermS<Syn>, val next: TermS<Syn>, override val type: TermS<Sem>) : TermS<Syn>

        class Var(val name: String?, val level: Int, override val type: TermS<Sem>) : TermS<End>

        class Meta(val index: Int, override val type: TermS<Sem>) : TermS<End>

        class Hole(override val type: TermS<Sem>) : TermS<End>
    }

    class Clos(val env: PersistentList<Lazy<TermS<Sem>>>, val body: Lazy<TermS<Syn>>)
}
