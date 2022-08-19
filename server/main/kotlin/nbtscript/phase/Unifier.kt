package nbtscript.phase

import nbtscript.ast.Core.*

class Unifier {
    private val metas: MutableList<Value?> = mutableListOf()

    operator fun get(index: Int): Value? = metas.getOrNull(index)

    fun fresh(
        type: Value,
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

        else -> false
    }

    fun unifyValue(
        lvl: Int,
        value1: Value,
        value2: Value,
    ): Boolean {
        @Suppress("NAME_SHADOWING") val value1 = force(value1)
        @Suppress("NAME_SHADOWING") val value2 = force(value2)
        return when {
            value1 is Value.Meta -> solve(lvl, value1.index, value2)
            value2 is Value.Meta -> solve(lvl, value2.index, value1)

            value1 is Value.UniverseType && value2 is Value.UniverseType -> true
            value1 is Value.EndType && value2 is Value.EndType -> true
            value1 is Value.ByteType && value2 is Value.ByteType -> true
            value1 is Value.ShortType && value2 is Value.ShortType -> true
            value1 is Value.IntType && value2 is Value.IntType -> true
            value1 is Value.LongType && value2 is Value.LongType -> true
            value1 is Value.FloatType && value2 is Value.FloatType -> true
            value1 is Value.DoubleType && value2 is Value.DoubleType -> true
            value1 is Value.StringType && value2 is Value.StringType -> true
            value1 is Value.ByteArrayType && value2 is Value.ByteArrayType -> true
            value1 is Value.IntArrayType && value2 is Value.IntArrayType -> true
            value1 is Value.LongArrayType && value2 is Value.LongArrayType -> true
            value1 is Value.ListType && value2 is Value.ListType -> unifyValue(lvl, value1.element.value, value2.element.value)
            value1 is Value.CompoundType && value2 is Value.CompoundType -> {
                value1.elements.keys == value2.elements.keys && value1.elements.all {
                    unifyValue(lvl, it.value.value, value2.elements[it.key]!!.value)
                }
            }

            value1 is Value.IndexedElement && value2 is Value.IndexedElement -> false // ?
            value1 is Value.FunctionType && value2 is Value.FunctionType -> {
                unifyValue(lvl, value1.dom.value, value2.dom.value) && lazyOf(Value.Var(null, lvl, value1.dom)).let { operand ->
                    unifyValue(lvl.inc(), value1.cod(this, operand), value2.cod(this, operand))
                }
            }

            value1 is Value.TypeType && value2 is Value.TypeType -> true
            value1 is Value.EndTag && value2 is Value.EndTag -> true
            value1 is Value.ByteTag && value2 is Value.ByteTag -> value1.data == value2.data
            value1 is Value.ShortTag && value2 is Value.ShortTag -> value1.data == value2.data
            value1 is Value.IntTag && value2 is Value.IntTag -> value1.data == value2.data
            value1 is Value.LongTag && value2 is Value.LongTag -> value1.data == value2.data
            value1 is Value.FloatTag && value2 is Value.FloatTag -> value1.data == value2.data
            value1 is Value.DoubleTag && value2 is Value.DoubleTag -> value1.data == value2.data
            value1 is Value.StringTag && value2 is Value.StringTag -> value1.data == value2.data
            value1 is Value.ByteArrayTag && value2 is Value.ByteArrayTag -> {
                (value1.elements zip value2.elements).all { (element1, element2) -> unifyValue(lvl, element1.value, element2.value) }
            }

            value1 is Value.IntArrayTag && value2 is Value.IntArrayTag -> {
                (value1.elements zip value2.elements).all { (element1, element2) -> unifyValue(lvl, element1.value, element2.value) }
            }

            value1 is Value.LongArrayTag && value2 is Value.LongArrayTag -> {
                (value1.elements zip value2.elements).all { (element1, element2) -> unifyValue(lvl, element1.value, element2.value) }
            }

            value1 is Value.ListTag && value2 is Value.ListTag -> {
                (value1.elements zip value2.elements).all { (element1, element2) -> unifyValue(lvl, element1.value, element2.value) }
            }

            value1 is Value.CompoundTag && value2 is Value.CompoundTag -> {
                value1.elements.keys == value2.elements.keys && value1.elements.all {
                    unifyValue(lvl, it.value.value, value2.elements[it.key]!!.value)
                }
            }

            value1 is Value.Abs && value2 is Value.Abs -> {
                val operand = lazyOf(Value.Var(null, lvl, value1.anno))
                unifyValue(lvl.inc(), value1.body(this, operand), value2.body(this, operand))
            }

            value1 is Value.Apply && value2 is Value.Apply -> {
                unifyValue(lvl, value1.operator, value2.operator) && unifyValue(lvl, value1.operand.value, value2.operand.value)
            }

            value1 is Value.Quote && value2 is Value.Quote -> false // ?
            value1 is Value.Var && value2 is Value.Var -> value1.level == value2.level
            value1 is Value.Hole && value2 is Value.Hole -> false // ?
            else -> false
        }
    }

    tailrec fun force(
        value: Value,
    ): Value = when (value) {
        is Value.Meta -> {
            when (val meta = metas.getOrNull(value.index)) {
                null -> value
                else -> force(meta)
            }
        }

        else -> value
    }

    private fun solve(
        lvl: Int,
        index: Int,
        candidate: Value,
    ): Boolean = when (val meta = metas.getOrNull(index)) {
        null -> {
            metas[index] = candidate
            true
        }

        else -> unifyValue(lvl, meta, candidate)
    }
}
