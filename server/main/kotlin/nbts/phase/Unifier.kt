package nbts.phase

import nbts.ast.Core.Kind.Sem
import nbts.ast.Core.Kind.Syn
import nbts.ast.Core.TermS
import nbts.ast.Core.TypeZ

class Unifier {
    private val metas: MutableList<TermS<Sem>?> = mutableListOf()

    operator fun get(index: Int): TermS<Sem>? = metas.getOrNull(index)

    fun fresh(): TermS<Syn> {
        metas += null
        return TermS.Meta(metas.lastIndex, TermS.UniverseType)
    }

    fun subTypeZ(
        type1: TypeZ<Sem>,
        type2: TypeZ<Sem>,
    ): Boolean = when {
        type1 is TypeZ.EndType -> true
        type2 is TypeZ.EndType -> false
        type2 is TypeZ.CollectionType -> when (type1) {
            is TypeZ.CollectionType -> subTypeZ(type1.element, type2.element)
            is TypeZ.ByteArrayType -> subTypeZ(TypeZ.ByteType, type2.element)
            is TypeZ.IntArrayType -> subTypeZ(TypeZ.IntType, type2.element)
            is TypeZ.LongArrayType -> subTypeZ(TypeZ.LongType, type2.element)
            is TypeZ.ListType -> subTypeZ(type1.element, type2.element)
            else -> false
        }

        type1 is TypeZ.ListType && type2 is TypeZ.ListType -> subTypeZ(type1.element, type2.element)
        type1 is TypeZ.CompoundType && type2 is TypeZ.CompoundType -> {
            type1.elements.keys == type2.elements.keys && type1.elements.all {
                subTypeZ(it.value, type2.elements[it.key]!!)
            }
        }

        else -> unifyTypeZ(type1, type2)
    }

    private fun unifyTypeZ(
        type1: TypeZ<Sem>,
        type2: TypeZ<Sem>,
    ): Boolean = when {
        type1 is TypeZ.EndType && type2 is TypeZ.EndType -> true
        type1 is TypeZ.ByteType && type2 is TypeZ.ByteType -> true
        type1 is TypeZ.ShortType && type2 is TypeZ.ShortType -> true
        type1 is TypeZ.IntType && type2 is TypeZ.IntType -> true
        type1 is TypeZ.LongType && type2 is TypeZ.LongType -> true
        type1 is TypeZ.FloatType && type2 is TypeZ.FloatType -> true
        type1 is TypeZ.DoubleType && type2 is TypeZ.DoubleType -> true
        type1 is TypeZ.StringType && type2 is TypeZ.StringType -> true
        type1 is TypeZ.CollectionType && type2 is TypeZ.CollectionType -> unifyTypeZ(type1.element, type2.element)
        type1 is TypeZ.ByteArrayType && type2 is TypeZ.ByteArrayType -> true
        type1 is TypeZ.IntArrayType && type2 is TypeZ.IntArrayType -> true
        type1 is TypeZ.LongArrayType && type2 is TypeZ.LongArrayType -> true
        type1 is TypeZ.ListType && type2 is TypeZ.ListType -> unifyTypeZ(type1.element, type2.element)
        type1 is TypeZ.CompoundType && type2 is TypeZ.CompoundType -> {
            type1.elements.keys == type2.elements.keys && type1.elements.all {
                unifyTypeZ(it.value, type2.elements[it.key]!!)
            }
        }

        type1 is TypeZ.Splice && type2 is TypeZ.Splice -> unifyTermS(0, type1.element, type2.element)
        else -> false
    }

    fun unifyTermS(
        lvl: Int,
        term1: TermS<Sem>,
        term2: TermS<Sem>,
    ): Boolean {
        @Suppress("NAME_SHADOWING") val term1 = force(term1)
        @Suppress("NAME_SHADOWING") val term2 = force(term2)
        return when {
            term1 is TermS.Meta -> solve(lvl, term1.index, term2)
            term2 is TermS.Meta -> solve(lvl, term2.index, term1)

            term1 is TermS.UniverseType && term2 is TermS.UniverseType -> true
            term1 is TermS.EndType && term2 is TermS.EndType -> true
            term1 is TermS.ByteType && term2 is TermS.ByteType -> true
            term1 is TermS.ShortType && term2 is TermS.ShortType -> true
            term1 is TermS.IntType && term2 is TermS.IntType -> true
            term1 is TermS.LongType && term2 is TermS.LongType -> true
            term1 is TermS.FloatType && term2 is TermS.FloatType -> true
            term1 is TermS.DoubleType && term2 is TermS.DoubleType -> true
            term1 is TermS.StringType && term2 is TermS.StringType -> true
            term1 is TermS.ByteArrayType && term2 is TermS.ByteArrayType -> true
            term1 is TermS.IntArrayType && term2 is TermS.IntArrayType -> true
            term1 is TermS.LongArrayType && term2 is TermS.LongArrayType -> true
            term1 is TermS.VListType && term2 is TermS.VListType -> unifyTermS(lvl, term1.element.value, term2.element.value)
            term1 is TermS.VCompoundType && term2 is TermS.VCompoundType -> {
                term1.elements.keys == term2.elements.keys && term1.elements.all {
                    unifyTermS(lvl, it.value.value, term2.elements[it.key]!!.value)
                }
            }

            term1 is TermS.NodeType && term2 is TermS.NodeType -> true
            term1 is TermS.VFunType && term2 is TermS.VFunType -> {
                unifyTermS(lvl, term1.dom.value, term2.dom.value) && lazyOf(TermS.Var(null, lvl, term1.dom.value)).let { operand ->
                    unifyTermS(lvl.inc(), term1.cod(this, operand), term2.cod(this, operand))
                }
            }

            term1 is TermS.VCodeZType && term2 is TermS.VCodeZType -> unifyTypeZ(term1.element.value, term2.element.value)
            term1 is TermS.VCodeSType && term2 is TermS.VCodeSType -> unifyTermS(lvl, term1.element.value, term2.element.value)
            term1 is TermS.TypeType && term2 is TermS.TypeType -> true
            term1 is TermS.EndTag && term2 is TermS.EndTag -> true
            term1 is TermS.ByteTag && term2 is TermS.ByteTag -> term1.data == term2.data
            term1 is TermS.ShortTag && term2 is TermS.ShortTag -> term1.data == term2.data
            term1 is TermS.IntTag && term2 is TermS.IntTag -> term1.data == term2.data
            term1 is TermS.LongTag && term2 is TermS.LongTag -> term1.data == term2.data
            term1 is TermS.FloatTag && term2 is TermS.FloatTag -> term1.data == term2.data
            term1 is TermS.DoubleTag && term2 is TermS.DoubleTag -> term1.data == term2.data
            term1 is TermS.StringTag && term2 is TermS.StringTag -> term1.data == term2.data
            term1 is TermS.VByteArrayTag && term2 is TermS.VByteArrayTag -> {
                (term1.elements zip term2.elements).all { (element1, element2) -> unifyTermS(lvl, element1.value, element2.value) }
            }

            term1 is TermS.VIntArrayTag && term2 is TermS.VIntArrayTag -> {
                (term1.elements zip term2.elements).all { (element1, element2) -> unifyTermS(lvl, element1.value, element2.value) }
            }

            term1 is TermS.VLongArrayTag && term2 is TermS.VLongArrayTag -> {
                (term1.elements zip term2.elements).all { (element1, element2) -> unifyTermS(lvl, element1.value, element2.value) }
            }

            term1 is TermS.VListTag && term2 is TermS.VListTag -> {
                (term1.elements zip term2.elements).all { (element1, element2) -> unifyTermS(lvl, element1.value, element2.value) }
            }

            term1 is TermS.VCompoundTag && term2 is TermS.VCompoundTag -> {
                term1.elements.keys == term2.elements.keys && term1.elements.all {
                    unifyTermS(lvl, it.value.value, term2.elements[it.key]!!.value)
                }
            }

            term1 is TermS.VMatchObjectNode && term2 is TermS.VMatchObjectNode -> unifyTermS(lvl, term1.pattern.value, term2.pattern.value)
            term1 is TermS.VMatchElementNode && term2 is TermS.VMatchElementNode -> unifyTermS(lvl, term1.pattern.value, term2.pattern.value)
            term1 is TermS.AllElementsNode && term2 is TermS.AllElementsNode -> true
            term1 is TermS.VIndexedElementNode && term2 is TermS.VIndexedElementNode -> unifyTermS(lvl, term1.index.value, term2.index.value)
            term1 is TermS.VCompoundChildNode && term2 is TermS.VCompoundChildNode -> unifyTermS(lvl, term1.name.value, term2.name.value)
            term1 is TermS.VGet && term2 is TermS.VGet -> {
                unifyTermS(lvl, term1.target.value, term2.target.value) && unifyTermS(lvl, term1.path.value, term2.path.value)
            }

            term1 is TermS.VAbs && term2 is TermS.VAbs -> {
                val operand = lazyOf(TermS.Var(null, lvl, term1.anno.value))
                unifyTermS(lvl.inc(), term1.body(this, operand), term2.body(this, operand))
            }

            term1 is TermS.VApply && term2 is TermS.VApply -> {
                unifyTermS(lvl, term1.operator, term2.operator) && unifyTermS(lvl, term1.operand.value, term2.operand.value)
            }

            term1 is TermS.VQuoteTypeZ && term2 is TermS.VQuoteTypeZ -> unifyTypeZ(term1.element.value, term2.element.value)
            term1 is TermS.QuoteTermZ && term2 is TermS.QuoteTermZ -> false // ?
            term1 is TermS.VQuoteTermS && term2 is TermS.VQuoteTermS -> unifyTermS(lvl, term1.element.value, term2.element.value)
            term1 is TermS.VSplice && term2 is TermS.VSplice -> unifyTermS(lvl, term1.element.value, term2.element.value)
            term1 is TermS.Var && term2 is TermS.Var -> term1.level == term2.level
            term1 is TermS.Hole && term2 is TermS.Hole -> false // ?
            else -> false
        }
    }

    tailrec fun force(
        term: TermS<Sem>,
    ): TermS<Sem> = when (term) {
        is TermS.Meta -> {
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
        candidate: TermS<Sem>,
    ): Boolean = when (val meta = metas.getOrNull(index)) {
        null -> {
            metas[index] = candidate
            true
        }

        else -> unifyTermS(lvl, meta, candidate)
    }
}
