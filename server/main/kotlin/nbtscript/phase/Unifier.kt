package nbtscript.phase

import kotlinx.collections.immutable.persistentListOf
import nbtscript.ast.Core.*

class Unifier {
    private val metas: MutableList<VTermS?> = mutableListOf()

    operator fun get(index: Int): VTermS? = metas.getOrNull(index)

    fun fresh(
        type: VTermS,
    ): TermS {
        metas += null
        return TermS.Meta(metas.lastIndex, type)
    }

    fun subTypeZ(
        type1: TypeZ,
        type2: TypeZ,
    ): Boolean = when {
        type1 is TypeZ.EndType -> true
        type2 is TypeZ.EndType -> false
        type1 is TypeZ.ByteType && type2 is TypeZ.ByteType -> true
        type1 is TypeZ.ShortType && type2 is TypeZ.ShortType -> true
        type1 is TypeZ.IntType && type2 is TypeZ.IntType -> true
        type1 is TypeZ.LongType && type2 is TypeZ.LongType -> true
        type1 is TypeZ.FloatType && type2 is TypeZ.FloatType -> true
        type1 is TypeZ.DoubleType && type2 is TypeZ.DoubleType -> true
        type1 is TypeZ.StringType && type2 is TypeZ.StringType -> true
        type1 is TypeZ.ByteArrayType && type2 is TypeZ.ByteArrayType -> true
        type1 is TypeZ.IntArrayType && type2 is TypeZ.IntArrayType -> true
        type1 is TypeZ.LongArrayType && type2 is TypeZ.LongArrayType -> true
        type1 is TypeZ.ListType && type2 is TypeZ.ListType -> subTypeZ(type1.element, type2.element) // sound?
        type1 is TypeZ.CollectionType && type2 is TypeZ.CollectionType && type2::class == TypeZ.CollectionType::class -> subTypeZ(type1.element, type2.element)
        type1 is TypeZ.CompoundType && type2 is TypeZ.CompoundType -> {
            type1.elements.keys == type2.elements.keys && type1.elements.all {
                subTypeZ(it.value, type2.elements[it.key]!!)
            }
        }

        type1 is TypeZ.Splice && type2 is TypeZ.Splice -> {
            val env: Environment = persistentListOf()
            unifyValue(env.size, reflect(env, type1.element), reflect(env, type2.element))
        }

        else -> false
    }

    fun unifyValue(
        lvl: Int,
        term1: VTermS,
        term2: VTermS,
    ): Boolean {
        @Suppress("NAME_SHADOWING") val value1 = force(term1)
        @Suppress("NAME_SHADOWING") val value2 = force(term2)
        return when {
            value1 is VTermS.Meta -> solve(lvl, value1.index, value2)
            value2 is VTermS.Meta -> solve(lvl, value2.index, value1)

            value1 is VTermS.UniverseType && value2 is VTermS.UniverseType -> true
            value1 is VTermS.EndType && value2 is VTermS.EndType -> true
            value1 is VTermS.ByteType && value2 is VTermS.ByteType -> true
            value1 is VTermS.ShortType && value2 is VTermS.ShortType -> true
            value1 is VTermS.IntType && value2 is VTermS.IntType -> true
            value1 is VTermS.LongType && value2 is VTermS.LongType -> true
            value1 is VTermS.FloatType && value2 is VTermS.FloatType -> true
            value1 is VTermS.DoubleType && value2 is VTermS.DoubleType -> true
            value1 is VTermS.StringType && value2 is VTermS.StringType -> true
            value1 is VTermS.ByteArrayType && value2 is VTermS.ByteArrayType -> true
            value1 is VTermS.IntArrayType && value2 is VTermS.IntArrayType -> true
            value1 is VTermS.LongArrayType && value2 is VTermS.LongArrayType -> true
            value1 is VTermS.ListType && value2 is VTermS.ListType -> unifyValue(lvl, value1.element.value, value2.element.value)
            value1 is VTermS.CompoundType && value2 is VTermS.CompoundType -> {
                value1.elements.keys == value2.elements.keys && value1.elements.all {
                    unifyValue(lvl, it.value.value, value2.elements[it.key]!!.value)
                }
            }

            value1 is VTermS.IndexedElement && value2 is VTermS.IndexedElement -> false // ?
            value1 is VTermS.FunctionType && value2 is VTermS.FunctionType -> {
                unifyValue(lvl, value1.dom.value, value2.dom.value) && lazyOf(VTermS.Var(null, lvl, value1.dom)).let { operand ->
                    unifyValue(lvl.inc(), value1.cod(this, operand), value2.cod(this, operand))
                }
            }

            value1 is VTermS.TypeType && value2 is VTermS.TypeType -> true
            value1 is VTermS.EndTag && value2 is VTermS.EndTag -> true
            value1 is VTermS.ByteTag && value2 is VTermS.ByteTag -> value1.data == value2.data
            value1 is VTermS.ShortTag && value2 is VTermS.ShortTag -> value1.data == value2.data
            value1 is VTermS.IntTag && value2 is VTermS.IntTag -> value1.data == value2.data
            value1 is VTermS.LongTag && value2 is VTermS.LongTag -> value1.data == value2.data
            value1 is VTermS.FloatTag && value2 is VTermS.FloatTag -> value1.data == value2.data
            value1 is VTermS.DoubleTag && value2 is VTermS.DoubleTag -> value1.data == value2.data
            value1 is VTermS.StringTag && value2 is VTermS.StringTag -> value1.data == value2.data
            value1 is VTermS.ByteArrayTag && value2 is VTermS.ByteArrayTag -> {
                (value1.elements zip value2.elements).all { (element1, element2) -> unifyValue(lvl, element1.value, element2.value) }
            }

            value1 is VTermS.IntArrayTag && value2 is VTermS.IntArrayTag -> {
                (value1.elements zip value2.elements).all { (element1, element2) -> unifyValue(lvl, element1.value, element2.value) }
            }

            value1 is VTermS.LongArrayTag && value2 is VTermS.LongArrayTag -> {
                (value1.elements zip value2.elements).all { (element1, element2) -> unifyValue(lvl, element1.value, element2.value) }
            }

            value1 is VTermS.ListTag && value2 is VTermS.ListTag -> {
                (value1.elements zip value2.elements).all { (element1, element2) -> unifyValue(lvl, element1.value, element2.value) }
            }

            value1 is VTermS.CompoundTag && value2 is VTermS.CompoundTag -> {
                value1.elements.keys == value2.elements.keys && value1.elements.all {
                    unifyValue(lvl, it.value.value, value2.elements[it.key]!!.value)
                }
            }

            value1 is VTermS.Abs && value2 is VTermS.Abs -> {
                val operand = lazyOf(VTermS.Var(null, lvl, value1.anno))
                unifyValue(lvl.inc(), value1.body(this, operand), value2.body(this, operand))
            }

            value1 is VTermS.Apply && value2 is VTermS.Apply -> {
                unifyValue(lvl, value1.operator, value2.operator) && unifyValue(lvl, value1.operand.value, value2.operand.value)
            }

            value1 is VTermS.QuoteType && value2 is VTermS.QuoteType -> false // ?
            value1 is VTermS.QuoteTerm && value2 is VTermS.QuoteTerm -> false // ?
            value1 is VTermS.Var && value2 is VTermS.Var -> value1.level == value2.level
            value1 is VTermS.Hole && value2 is VTermS.Hole -> false // ?
            else -> false
        }
    }

    tailrec fun force(
        term: VTermS,
    ): VTermS = when (term) {
        is VTermS.Meta -> {
            when (val meta = metas.getOrNull(term.index)) {
                null -> term
                else -> force(meta)
            }
        }

        else -> term
    }

    private fun solve(
        lvl: Int,
        index: Int,
        candidate: VTermS,
    ): Boolean = when (val meta = metas.getOrNull(index)) {
        null -> {
            metas[index] = candidate
            true
        }

        else -> unifyValue(lvl, meta, candidate)
    }
}
