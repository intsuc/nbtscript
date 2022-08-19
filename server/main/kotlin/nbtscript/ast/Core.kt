package nbtscript.ast

import kotlinx.collections.immutable.PersistentList
import nbtscript.ast.Core.Kind.Sem
import nbtscript.ast.Core.Kind.Syn
import nbtscript.cast

sealed interface Core {
    class Root(val body: TermZ) : Core

    sealed interface Kind {
        object Syn : Kind
        object Sem : Kind
    }

    sealed interface ObjZ<out K : Kind> : Core

    sealed interface TypeZ<out K : Kind> : ObjZ<K> {
        class EndType<K : Kind> private constructor() : TypeZ<K> {
            companion object {
                val Syn: TypeZ<Syn> = EndType()
                val Sem: TypeZ<Sem> = reflect(Syn)
            }
        }

        class ByteType<K : Kind> private constructor() : TypeZ<K> {
            companion object {
                val Syn: TypeZ<Syn> = ByteType()
                val Sem: TypeZ<Sem> = reflect(Syn)
            }
        }

        class ShortType<K : Kind> private constructor() : TypeZ<K> {
            companion object {
                val Syn: TypeZ<Syn> = ShortType()
                val Sem: TypeZ<Sem> = reflect(Syn)
            }
        }

        class IntType<K : Kind> private constructor() : TypeZ<K> {
            companion object {
                val Syn: TypeZ<Syn> = IntType()
                val Sem: TypeZ<Sem> = reflect(Syn)
            }
        }

        class LongType<K : Kind> private constructor() : TypeZ<K> {
            companion object {
                val Syn: TypeZ<Syn> = LongType()
                val Sem: TypeZ<Sem> = reflect(Syn)
            }
        }

        class FloatType<K : Kind> private constructor() : TypeZ<K> {
            companion object {
                val Syn: TypeZ<Syn> = FloatType()
                val Sem: TypeZ<Sem> = reflect(Syn)
            }
        }

        class DoubleType<K : Kind> private constructor() : TypeZ<K> {
            companion object {
                val Syn: TypeZ<Syn> = DoubleType()
                val Sem: TypeZ<Sem> = reflect(Syn)
            }
        }

        class StringType<K : Kind> private constructor() : TypeZ<K> {
            companion object {
                val Syn: TypeZ<Syn> = StringType()
                val Sem: TypeZ<Sem> = reflect(Syn)
            }
        }

        open class CollectionType<K : Kind>(open val element: TypeZ<K>) : TypeZ<K>
        class ByteArrayType<K : Kind> private constructor() : CollectionType<K>(cast(ByteType.Syn)) {
            companion object {
                val Syn: TypeZ<Syn> = ByteArrayType()
                val Sem: TypeZ<Sem> = reflect(Syn)
            }
        }

        class IntArrayType<K : Kind> private constructor() : CollectionType<K>(cast(IntType.Syn)) {
            companion object {
                val Syn: TypeZ<Syn> = IntArrayType()
                val Sem: TypeZ<Sem> = reflect(Syn)
            }
        }

        class LongArrayType<K : Kind> private constructor() : CollectionType<K>(cast(LongType.Syn)) {
            companion object {
                val Syn: TypeZ<Syn> = LongArrayType()
                val Sem: TypeZ<Sem> = reflect(Syn)
            }
        }

        class ListType<K : Kind>(override val element: TypeZ<K>) : CollectionType<K>(element)
        class CompoundType<K : Kind>(val elements: Map<String, TypeZ<K>>) : TypeZ<K>
        class Splice<K : Kind>(val element: TermS<K>) : TypeZ<K>
        class Hole<K : Kind> private constructor() : TypeZ<K> {
            companion object {
                val Syn: TypeZ<Syn> = Hole()
                val Sem: TypeZ<Sem> = reflect(Syn)
            }
        }

        companion object {
            @Suppress("NOTHING_TO_INLINE")
            inline fun reflect(term: TypeZ<Syn>): TypeZ<Sem> = cast(term)

            @Suppress("NOTHING_TO_INLINE")
            inline fun reify(term: TypeZ<Sem>): TypeZ<Syn> = cast(term)
        }
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

        class UniverseType<K : Kind> private constructor() : TermS<K> {
            override val type: TermS<Sem> get() = Sem

            companion object {
                val Syn: TermS<Syn> = UniverseType()
                val Sem: TermS<Sem> = reflect(Syn)
            }
        }

        class EndType<K : Kind> private constructor() : TermS<K> {
            override val type: TermS<Sem> get() = UniverseType.Sem

            companion object {
                val Syn: TermS<Syn> = EndType()
                val Sem: TermS<Sem> = reflect(Syn)
            }
        }

        class ByteType<K : Kind> private constructor() : TermS<K> {
            override val type: TermS<Sem> get() = UniverseType.Sem

            companion object {
                val Syn: TermS<Syn> = ByteType()
                val Sem: TermS<Sem> = reflect(Syn)
            }
        }

        class ShortType<K : Kind> private constructor() : TermS<K> {
            override val type: TermS<Sem> get() = UniverseType.Sem

            companion object {
                val Syn: TermS<Syn> = ShortType()
                val Sem: TermS<Sem> = reflect(Syn)
            }
        }

        class IntType<K : Kind> private constructor() : TermS<K> {
            override val type: TermS<Sem> get() = UniverseType.Sem

            companion object {
                val Syn: TermS<Syn> = IntType()
                val Sem: TermS<Sem> = reflect(Syn)
            }
        }

        class LongType<K : Kind> private constructor() : TermS<K> {
            override val type: TermS<Sem> get() = UniverseType.Sem

            companion object {
                val Syn: TermS<Syn> = LongType()
                val Sem: TermS<Sem> = reflect(Syn)
            }
        }

        class FloatType<K : Kind> private constructor() : TermS<K> {
            override val type: TermS<Sem> get() = UniverseType.Sem

            companion object {
                val Syn: TermS<Syn> = FloatType()
                val Sem: TermS<Sem> = reflect(Syn)
            }
        }

        class DoubleType<K : Kind> private constructor() : TermS<K> {
            override val type: TermS<Sem> get() = UniverseType.Sem

            companion object {
                val Syn: TermS<Syn> = DoubleType()
                val Sem: TermS<Sem> = reflect(Syn)
            }
        }

        class StringType<K : Kind> private constructor() : TermS<K> {
            override val type: TermS<Sem> get() = UniverseType.Sem

            companion object {
                val Syn: TermS<Syn> = StringType()
                val Sem: TermS<Sem> = reflect(Syn)
            }
        }

        class ByteArrayType<K : Kind> private constructor() : TermS<K> {
            override val type: TermS<Sem> get() = UniverseType.Sem

            companion object {
                val Syn: TermS<Syn> = ByteArrayType()
                val Sem: TermS<Sem> = reflect(Syn)
            }
        }

        class IntArrayType<K : Kind> private constructor() : TermS<K> {
            override val type: TermS<Sem> get() = UniverseType.Sem

            companion object {
                val Syn: TermS<Syn> = IntArrayType()
                val Sem: TermS<Sem> = reflect(Syn)
            }
        }

        class LongArrayType<K : Kind> private constructor() : TermS<K> {
            override val type: TermS<Sem> get() = UniverseType.Sem

            companion object {
                val Syn: TermS<Syn> = LongArrayType()
                val Sem: TermS<Sem> = reflect(Syn)
            }
        }

        class ListType(val element: TermS<Syn>) : TermS<Syn> {
            override val type: TermS<Sem> get() = UniverseType.Sem
        }

        class VListType(val element: Lazy<TermS<Sem>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = UniverseType.Sem
        }

        class CompoundType(val elements: Map<String, TermS<Syn>>) : TermS<Syn> {
            override val type: TermS<Sem> get() = UniverseType.Sem
        }

        class VCompoundType(val elements: Map<String, Lazy<TermS<Sem>>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = UniverseType.Sem
        }

        class FunctionType(val name: String?, val dom: TermS<Syn>, val cod: TermS<Syn>) : TermS<Syn> {
            override val type: TermS<Sem> get() = UniverseType.Sem
        }

        class VFunctionType(val name: String?, val dom: Lazy<TermS<Sem>>, val cod: Clos) : TermS<Sem> {
            override val type: TermS<Sem> get() = UniverseType.Sem
        }

        class CodeType(val element: TypeZ<Syn>) : TermS<Syn> {
            override val type: TermS<Sem> get() = UniverseType.Sem // ?
        }

        class VCodeType(val element: Lazy<TypeZ<Sem>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = UniverseType.Sem // ?
        }

        class TypeType<K : Kind> private constructor() : TermS<K> {
            override val type: TermS<Sem> get() = UniverseType.Sem

            companion object {
                val Syn: TermS<Syn> = TypeType()
                val Sem: TermS<Sem> = reflect(Syn)
            }
        }

        class EndTag<K : Kind> private constructor() : TermS<K> {
            override val type: TermS<Sem> get() = EndType.Sem

            companion object {
                val Syn: TermS<Syn> = EndTag()
                val Sem: TermS<Sem> = reflect(Syn)
            }
        }

        class ByteTag<K : Kind>(val data: Byte) : TermS<K> {
            override val type: TermS<Sem> get() = ByteType.Sem
        }

        class ShortTag<K : Kind>(val data: Short) : TermS<K> {
            override val type: TermS<Sem> get() = ShortType.Sem
        }

        class IntTag<K : Kind>(val data: Int) : TermS<K> {
            override val type: TermS<Sem> get() = IntType.Sem
        }

        class LongTag<K : Kind>(val data: Long) : TermS<K> {
            override val type: TermS<Sem> get() = LongType.Sem
        }

        class FloatTag<K : Kind>(val data: Float) : TermS<K> {
            override val type: TermS<Sem> get() = FloatType.Sem
        }

        class DoubleTag<K : Kind>(val data: Double) : TermS<K> {
            override val type: TermS<Sem> get() = DoubleType.Sem
        }

        class StringTag<K : Kind>(val data: String) : TermS<K> {
            override val type: TermS<Sem> get() = StringType.Sem
        }

        class ByteArrayTag(val elements: List<TermS<Syn>>) : TermS<Syn> {
            override val type: TermS<Sem> get() = ByteArrayType.Sem
        }

        class VByteArrayTag(val elements: List<Lazy<TermS<Sem>>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = ByteArrayType.Sem
        }

        class IntArrayTag(val elements: List<TermS<Syn>>) : TermS<Syn> {
            override val type: TermS<Sem> get() = IntArrayType.Sem
        }

        class VIntArrayTag(val elements: List<Lazy<TermS<Sem>>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = IntArrayType.Sem
        }

        class LongArrayTag(val elements: List<TermS<Syn>>) : TermS<Syn> {
            override val type: TermS<Sem> get() = LongArrayType.Sem
        }

        class VLongArrayTag(val elements: List<Lazy<TermS<Sem>>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = LongArrayType.Sem
        }

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
        class QuoteType(val element: TypeZ<Syn>) : TermS<Syn> {
            override val type: TermS<Sem> get() = TypeType.Sem
        }

        class VQuoteType(val element: Lazy<TypeZ<Sem>>) : TermS<Sem> {
            override val type: TermS<Sem> get() = TypeType.Sem
        }

        class QuoteTerm<K : Kind>(val element: TermZ, override val type: TermS<Sem>) : TermS<K>
        class Let(val name: String, val init: TermS<Syn>, val next: TermS<Syn>, override val type: TermS<Sem>) : TermS<Syn>
        class Var<K : Kind>(val name: String?, val level: Int, override val type: TermS<Sem>) : TermS<K>
        class Meta<K : Kind>(val index: Int, override val type: TermS<Sem>) : TermS<K>
        class Hole<K : Kind>(override val type: TermS<Sem>) : TermS<K> {
            companion object {
                val Syn: TermS<Syn> = Hole(EndType.Sem)
                val Sem: TermS<Sem> = reflect(Syn)
            }
        }

        companion object {
            @Suppress("NOTHING_TO_INLINE")
            inline fun reflect(term: TermS<Syn>): TermS<Sem> = cast(term)

            @Suppress("NOTHING_TO_INLINE")
            inline fun reify(term: TermS<Sem>): TermS<Syn> = cast(term)
        }
    }

    class Clos(val env: PersistentList<Lazy<TermS<Sem>>>, val body: Lazy<TermS<Syn>>) : Core
}
