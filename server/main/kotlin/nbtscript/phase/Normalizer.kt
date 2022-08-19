package nbtscript.phase

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import nbtscript.ast.Core.*

typealias Environment = PersistentList<Lazy<Value>>

fun Unifier.normalize(
    term: TermS,
): TermS {
    val env: Environment = persistentListOf()
    return reify(env, reflect(env, term))
}

fun Unifier.reflect(
    env: Environment,
    term: TermS,
): Value = when (term) {
    is TermS.UniverseType -> Value.UniverseType
    is TermS.EndType -> Value.EndType
    is TermS.ByteType -> Value.ByteType
    is TermS.ShortType -> Value.ShortType
    is TermS.IntType -> Value.IntType
    is TermS.LongType -> Value.LongType
    is TermS.FloatType -> Value.FloatType
    is TermS.DoubleType -> Value.DoubleType
    is TermS.StringType -> Value.StringType
    is TermS.ByteArrayType -> Value.ByteArrayType
    is TermS.IntArrayType -> Value.IntArrayType
    is TermS.LongArrayType -> Value.LongArrayType
    is TermS.ListType -> {
        val element = lazy { reflect(env, term.element) }
        Value.ListType(element)
    }

    is TermS.CompoundType -> {
        val elements = term.elements.mapValues { lazy { reflect(env, it.value) } }
        Value.CompoundType(elements)
    }

    is TermS.FunctionType -> {
        val dom = lazy { reflect(env, term.dom) }
        val cod = Clos(env, lazyOf(term.cod))
        Value.FunctionType(term.name, dom, cod)
    }

    is TermS.CodeType -> Value.CodeType(term.element)
    is TermS.TypeType -> Value.TypeType
    is TermS.EndTag -> Value.EndTag
    is TermS.ByteTag -> Value.ByteTag(term.data)
    is TermS.ShortTag -> Value.ShortTag(term.data)
    is TermS.IntTag -> Value.IntTag(term.data)
    is TermS.LongTag -> Value.LongTag(term.data)
    is TermS.FloatTag -> Value.FloatTag(term.data)
    is TermS.DoubleTag -> Value.DoubleTag(term.data)
    is TermS.StringTag -> Value.StringTag(term.data)
    is TermS.ByteArrayTag -> {
        val elements = term.elements.map { lazy { reflect(env, it) } }
        Value.ByteArrayTag(elements)
    }

    is TermS.IntArrayTag -> {
        val elements = term.elements.map { lazy { reflect(env, it) } }
        Value.IntArrayTag(elements)
    }

    is TermS.LongArrayTag -> {
        val elements = term.elements.map { lazy { reflect(env, it) } }
        Value.LongArrayTag(elements)
    }

    is TermS.ListTag -> {
        val elements = term.elements.map { lazy { reflect(env, it) } }
        Value.ListTag(elements)
    }

    is TermS.CompoundTag -> {
        val elements = term.elements.mapValues { lazy { reflect(env, it.value) } }
        Value.CompoundTag(elements)
    }

    is TermS.IndexedElement -> {
        val index = lazy { reflect(env, term.index) }
        Value.IndexedElement(term.target, index, term.type)
    }

    is TermS.Abs -> {
        val anno = lazy { reflect(env, term.anno) }
        val body = Clos(env, lazyOf(term.body))
        Value.Abs(term.name, anno, body)
    }

    is TermS.Apply -> {
        when (val operator = force(reflect(env, term.operator))) {
            is Value.Abs -> {
                val operand = lazy { reflect(env, term.operand) }
                operator.body(this, operand)
            }

            else -> {
                val operand = lazy { reflect(env, term.operand) }
                Value.Apply(operator, operand)
            }
        }
    }

    is TermS.QuoteType -> Value.QuoteType(term.element)
    is TermS.QuoteTerm -> Value.QuoteTerm(term.element)
    is TermS.Let -> {
        val init = lazy { reflect(env, term.init) }
        reflect(env + init, term.next)
    }

    is TermS.Var -> env[term.level].value
    is TermS.Meta -> Value.Meta(term.index, term.type)
    is TermS.Hole -> Value.Hole
}

fun Unifier.reify(
    env: Environment,
    value: Value,
): TermS = when (@Suppress("NAME_SHADOWING") val value = force(value)) {
    is Value.UniverseType -> TermS.UniverseType(Value.UniverseType)
    is Value.EndType -> TermS.EndType(Value.UniverseType)
    is Value.ByteType -> TermS.ByteType(Value.UniverseType)
    is Value.ShortType -> TermS.ShortType(Value.UniverseType)
    is Value.IntType -> TermS.IntType(Value.UniverseType)
    is Value.LongType -> TermS.LongType(Value.UniverseType)
    is Value.FloatType -> TermS.FloatType(Value.UniverseType)
    is Value.DoubleType -> TermS.DoubleType(Value.UniverseType)
    is Value.StringType -> TermS.StringType(Value.UniverseType)
    is Value.ByteArrayType -> TermS.ByteArrayType(Value.UniverseType)
    is Value.IntArrayType -> TermS.IntArrayType(Value.UniverseType)
    is Value.LongArrayType -> TermS.LongArrayType(Value.UniverseType)
    is Value.ListType -> {
        val element = reify(env, value.element.value)
        TermS.ListType(element, Value.UniverseType)
    }

    is Value.CompoundType -> {
        val elements = value.elements.mapValues { reify(env, it.value.value) }
        TermS.CompoundType(elements, Value.UniverseType)
    }

    is Value.IndexedElement -> {
        val index = reify(env, value.index.value)
        TermS.IndexedElement(value.target, index, value.type)
    }

    is Value.FunctionType -> {
        val dom = reify(env, value.dom.value)
        val x = lazyOf(Value.Var(value.name, env.size, value.dom))
        val cod = reify(env + x, value.cod(this, x))
        TermS.FunctionType(value.name, dom, cod, Value.UniverseType)
    }

    is Value.CodeType -> TermS.CodeType(value.element, Value.UniverseType)
    is Value.TypeType -> TermS.TypeType(Value.UniverseType)
    is Value.EndTag -> TermS.EndTag(Value.EndType)
    is Value.ByteTag -> TermS.ByteTag(value.data, Value.ByteType)
    is Value.ShortTag -> TermS.ShortTag(value.data, Value.ShortType)
    is Value.IntTag -> TermS.IntTag(value.data, Value.IntType)
    is Value.LongTag -> TermS.LongTag(value.data, Value.LongType)
    is Value.FloatTag -> TermS.FloatTag(value.data, Value.FloatType)
    is Value.DoubleTag -> TermS.DoubleTag(value.data, Value.DoubleType)
    is Value.StringTag -> TermS.StringTag(value.data, Value.StringType)
    is Value.ByteArrayTag -> {
        val elements = value.elements.map { reify(env, it.value) }
        TermS.ByteArrayTag(elements, Value.ByteArrayType)
    }

    is Value.IntArrayTag -> {
        val elements = value.elements.map { reify(env, it.value) }
        TermS.IntArrayTag(elements, Value.IntArrayType)
    }

    is Value.LongArrayTag -> {
        val elements = value.elements.map { reify(env, it.value) }
        TermS.LongArrayTag(elements, Value.LongArrayType)
    }

    is Value.ListTag -> {
        val elements = value.elements.map { reify(env, it.value) }
        val elementType = lazyOf(elements.firstOrNull()?.type ?: Value.EndType)
        TermS.ListTag(elements, Value.ListType(elementType))
    }

    is Value.CompoundTag -> {
        val elements = value.elements.mapValues { reify(env, it.value.value) }
        val elementTypes = elements.mapValues { lazyOf(it.value.type) }
        TermS.CompoundTag(elements, Value.CompoundType(elementTypes))
    }

    is Value.Abs -> {
        val anno = reify(env, value.anno.value)
        val x = lazyOf(Value.Var(value.name, env.size, value.anno))
        val body = reify(env + x, value.body(this, x))
        TermS.Abs(value.name, anno, body, Value.FunctionType(null, lazyOf(anno.type), Clos(env, lazy { reify(env, body.type) })))
    }

    is Value.Apply -> {
        val operator = reify(env, value.operator)
        val operand = reify(env, value.operand.value)
        val cod = when (val operatorType = operator.type) {
            is Value.FunctionType -> operatorType.cod(this, value.operand)
            else -> operatorType
        }
        TermS.Apply(operator, operand, cod)
    }

    is Value.QuoteType -> TermS.QuoteType(value.element, Value.TypeType)
    is Value.QuoteTerm -> TermS.QuoteTerm(value.element, Value.CodeType(value.element.type))
    is Value.Var -> TermS.Var(value.name, value.level, value.type.value)
    is Value.Meta -> TermS.Meta(value.index, value.type)
    is Value.Hole -> TermS.Hole(Value.EndType)
}

operator fun Clos.invoke(
    unifier: Unifier,
    argument: Lazy<Value>,
): Value = unifier.reflect(env + argument, body.value)
