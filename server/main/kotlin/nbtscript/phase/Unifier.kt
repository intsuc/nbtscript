package nbtscript.phase

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
        type1: TypeZ<Kind.Sem>,
        type2: TypeZ<Kind.Sem>,
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

        type1 is TypeZ.VSplice && type2 is TypeZ.VSplice -> unifyValue(0, type1.element, type2.element)
        else -> false
    }

    fun unifyValue(
        lvl: Int,
        term1: VTermS,
        term2: VTermS,
    ): Boolean {
        @Suppress("NAME_SHADOWING") val term1 = force(term1)
        @Suppress("NAME_SHADOWING") val term2 = force(term2)
        return when {
            term1 is VTermS.Meta -> solve(lvl, term1.index, term2)
            term2 is VTermS.Meta -> solve(lvl, term2.index, term1)

            term1 is VTermS.UniverseType && term2 is VTermS.UniverseType -> true
            term1 is VTermS.EndType && term2 is VTermS.EndType -> true
            term1 is VTermS.ByteType && term2 is VTermS.ByteType -> true
            term1 is VTermS.ShortType && term2 is VTermS.ShortType -> true
            term1 is VTermS.IntType && term2 is VTermS.IntType -> true
            term1 is VTermS.LongType && term2 is VTermS.LongType -> true
            term1 is VTermS.FloatType && term2 is VTermS.FloatType -> true
            term1 is VTermS.DoubleType && term2 is VTermS.DoubleType -> true
            term1 is VTermS.StringType && term2 is VTermS.StringType -> true
            term1 is VTermS.ByteArrayType && term2 is VTermS.ByteArrayType -> true
            term1 is VTermS.IntArrayType && term2 is VTermS.IntArrayType -> true
            term1 is VTermS.LongArrayType && term2 is VTermS.LongArrayType -> true
            term1 is VTermS.ListType && term2 is VTermS.ListType -> unifyValue(lvl, term1.element.value, term2.element.value)
            term1 is VTermS.CompoundType && term2 is VTermS.CompoundType -> {
                term1.elements.keys == term2.elements.keys && term1.elements.all {
                    unifyValue(lvl, it.value.value, term2.elements[it.key]!!.value)
                }
            }

            term1 is VTermS.IndexedElement && term2 is VTermS.IndexedElement -> false // ?
            term1 is VTermS.FunctionType && term2 is VTermS.FunctionType -> {
                unifyValue(lvl, term1.dom.value, term2.dom.value) && lazyOf(VTermS.Var(null, lvl, term1.dom)).let { operand ->
                    unifyValue(lvl.inc(), term1.cod(this, operand), term2.cod(this, operand))
                }
            }

            term1 is VTermS.TypeType && term2 is VTermS.TypeType -> true
            term1 is VTermS.EndTag && term2 is VTermS.EndTag -> true
            term1 is VTermS.ByteTag && term2 is VTermS.ByteTag -> term1.data == term2.data
            term1 is VTermS.ShortTag && term2 is VTermS.ShortTag -> term1.data == term2.data
            term1 is VTermS.IntTag && term2 is VTermS.IntTag -> term1.data == term2.data
            term1 is VTermS.LongTag && term2 is VTermS.LongTag -> term1.data == term2.data
            term1 is VTermS.FloatTag && term2 is VTermS.FloatTag -> term1.data == term2.data
            term1 is VTermS.DoubleTag && term2 is VTermS.DoubleTag -> term1.data == term2.data
            term1 is VTermS.StringTag && term2 is VTermS.StringTag -> term1.data == term2.data
            term1 is VTermS.ByteArrayTag && term2 is VTermS.ByteArrayTag -> {
                (term1.elements zip term2.elements).all { (element1, element2) -> unifyValue(lvl, element1.value, element2.value) }
            }

            term1 is VTermS.IntArrayTag && term2 is VTermS.IntArrayTag -> {
                (term1.elements zip term2.elements).all { (element1, element2) -> unifyValue(lvl, element1.value, element2.value) }
            }

            term1 is VTermS.LongArrayTag && term2 is VTermS.LongArrayTag -> {
                (term1.elements zip term2.elements).all { (element1, element2) -> unifyValue(lvl, element1.value, element2.value) }
            }

            term1 is VTermS.ListTag && term2 is VTermS.ListTag -> {
                (term1.elements zip term2.elements).all { (element1, element2) -> unifyValue(lvl, element1.value, element2.value) }
            }

            term1 is VTermS.CompoundTag && term2 is VTermS.CompoundTag -> {
                term1.elements.keys == term2.elements.keys && term1.elements.all {
                    unifyValue(lvl, it.value.value, term2.elements[it.key]!!.value)
                }
            }

            term1 is VTermS.Abs && term2 is VTermS.Abs -> {
                val operand = lazyOf(VTermS.Var(null, lvl, term1.anno))
                unifyValue(lvl.inc(), term1.body(this, operand), term2.body(this, operand))
            }

            term1 is VTermS.Apply && term2 is VTermS.Apply -> {
                unifyValue(lvl, term1.operator, term2.operator) && unifyValue(lvl, term1.operand.value, term2.operand.value)
            }

            term1 is VTermS.QuoteType && term2 is VTermS.QuoteType -> false // ?
            term1 is VTermS.QuoteTerm && term2 is VTermS.QuoteTerm -> false // ?
            term1 is VTermS.Var && term2 is VTermS.Var -> term1.level == term2.level
            term1 is VTermS.Hole && term2 is VTermS.Hole -> false // ?
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
