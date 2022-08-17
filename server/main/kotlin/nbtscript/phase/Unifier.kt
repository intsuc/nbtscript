package nbtscript.phase

import nbtscript.ast.Core.*

class Unifier {
    private val metas: MutableList<Value?> = mutableListOf()

    operator fun get(index: Int): Value? = metas[index]

    fun fresh(
        type: Value,
    ): TermS {
        metas += null
        return TermS.Meta(metas.lastIndex, type)
    }

    fun unifyZ(
        type1: TypeZ,
        type2: TypeZ,
    ): Boolean = when {
        type1 is TypeZ.EndZ && type2 is TypeZ.EndZ -> true
        type1 is TypeZ.ByteZ && type2 is TypeZ.ByteZ -> true
        type1 is TypeZ.ShortZ && type2 is TypeZ.ShortZ -> true
        type1 is TypeZ.IntZ && type2 is TypeZ.IntZ -> true
        type1 is TypeZ.LongZ && type2 is TypeZ.LongZ -> true
        type1 is TypeZ.FloatZ && type2 is TypeZ.FloatZ -> true
        type1 is TypeZ.DoubleZ && type2 is TypeZ.DoubleZ -> true
        type1 is TypeZ.StringZ && type2 is TypeZ.StringZ -> true
        type1 is TypeZ.ByteArrayZ && type2 is TypeZ.ByteArrayZ -> true
        type1 is TypeZ.IntArrayZ && type2 is TypeZ.IntArrayZ -> true
        type1 is TypeZ.LongArrayZ && type2 is TypeZ.LongArrayZ -> true
        type1 is TypeZ.ListZ && type2 is TypeZ.ListZ -> unifyZ(type1.element, type2.element)
        type1 is TypeZ.CompoundZ && type2 is TypeZ.CompoundZ -> {
            type1.elements.keys == type2.elements.keys && type1.elements.all {
                unifyZ(it.value, type2.elements[it.key]!!)
            }
        }

        else -> false
    }

    fun unifyS(
        lvl: Int,
        value1: Value,
        value2: Value,
    ): Boolean {
        @Suppress("NAME_SHADOWING") val value1 = forceS(value1)
        @Suppress("NAME_SHADOWING") val value2 = forceS(value2)
        return when {
            value1 is Value.Meta -> solveS(lvl, value1.index, value2)
            value2 is Value.Meta -> solveS(lvl, value2.index, value1)
            value1 is Value.UniverseS && value2 is Value.UniverseS -> true
            value1 is Value.EndS && value2 is Value.EndS -> true
            value1 is Value.ByteS && value2 is Value.ByteS -> true
            value1 is Value.ShortS && value2 is Value.ShortS -> true
            value1 is Value.IntS && value2 is Value.IntS -> true
            value1 is Value.LongS && value2 is Value.LongS -> true
            value1 is Value.FloatS && value2 is Value.FloatS -> true
            value1 is Value.DoubleS && value2 is Value.DoubleS -> true
            value1 is Value.StringS && value2 is Value.StringS -> true
            value1 is Value.ByteArrayS && value2 is Value.ByteArrayS -> true
            value1 is Value.IntArrayS && value2 is Value.IntArrayS -> true
            value1 is Value.LongArrayS && value2 is Value.LongArrayS -> true
            value1 is Value.ListS && value2 is Value.ListS -> unifyS(lvl, value1.element.value, value2.element.value)
            value1 is Value.CompoundS && value2 is Value.CompoundS -> {
                value1.elements.keys == value2.elements.keys && value1.elements.all {
                    unifyS(lvl, it.value.value, value2.elements[it.key]!!.value)
                }
            }

            value1 is Value.IndexedElement && value2 is Value.IndexedElement -> false // ?
            value1 is Value.FunctionS && value2 is Value.FunctionS -> {
                unifyS(lvl, value1.dom.value, value2.dom.value) && lazyOf(Value.Var(null, lvl, value1.dom)).let { operand ->
                    unifyS(lvl.inc(), value1.cod(operand), value2.cod(operand))
                }
            }

            value1 is Value.TypeZ && value2 is Value.TypeZ -> true
            value1 is Value.EndTag && value2 is Value.EndTag -> true
            value1 is Value.ByteTag && value2 is Value.ByteTag -> value1.data == value2.data
            value1 is Value.ShortTag && value2 is Value.ShortTag -> value1.data == value2.data
            value1 is Value.IntTag && value2 is Value.IntTag -> value1.data == value2.data
            value1 is Value.LongTag && value2 is Value.LongTag -> value1.data == value2.data
            value1 is Value.FloatTag && value2 is Value.FloatTag -> value1.data == value2.data
            value1 is Value.DoubleTag && value2 is Value.DoubleTag -> value1.data == value2.data
            value1 is Value.StringTag && value2 is Value.StringTag -> value1.data == value2.data
            value1 is Value.ByteArrayTag && value2 is Value.ByteArrayTag -> {
                (value1.elements zip value2.elements).all { (element1, element2) -> unifyS(lvl, element1.value, element2.value) }
            }

            value1 is Value.IntArrayTag && value2 is Value.IntArrayTag -> {
                (value1.elements zip value2.elements).all { (element1, element2) -> unifyS(lvl, element1.value, element2.value) }
            }

            value1 is Value.LongArrayTag && value2 is Value.LongArrayTag -> {
                (value1.elements zip value2.elements).all { (element1, element2) -> unifyS(lvl, element1.value, element2.value) }
            }

            value1 is Value.ListTag && value2 is Value.ListTag -> {
                (value1.elements zip value2.elements).all { (element1, element2) -> unifyS(lvl, element1.value, element2.value) }
            }

            value1 is Value.CompoundTag && value2 is Value.CompoundTag -> {
                value1.elements.keys == value2.elements.keys && value1.elements.all {
                    unifyS(lvl, it.value.value, value2.elements[it.key]!!.value)
                }
            }

            value1 is Value.Abs && value2 is Value.Abs -> {
                val operand = lazyOf(Value.Var(null, lvl, value1.anno))
                unifyS(lvl.inc(), value1.body(operand), value2.body(operand))
            }

            value1 is Value.Apply && value2 is Value.Apply -> {
                unifyS(lvl, value1.operator, value2.operator) && unifyS(lvl, value1.operand.value, value2.operand.value)
            }

            value1 is Value.Quote && value2 is Value.Quote -> false // ?
            value1 is Value.Var && value2 is Value.Var -> value1.level == value2.level
            value1 is Value.Hole && value2 is Value.Hole -> false // ?
            else -> false
        }
    }

    tailrec fun forceS(
        value: Value,
    ): Value = when (value) {
        is Value.Meta -> {
            when (val meta = metas[value.index]) {
                null -> value
                else -> forceS(meta)
            }
        }

        else -> value
    }

    private fun solveS(
        lvl: Int,
        index: Int,
        candidate: Value,
    ): Boolean = when (val meta = metas[index]) {
        null -> {
            metas[index] = candidate
            true
        }

        else -> unifyS(lvl, meta, candidate)
    }
}
