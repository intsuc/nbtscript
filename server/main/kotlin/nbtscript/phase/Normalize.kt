package nbtscript.phase

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import nbtscript.ast.Core.*

typealias Environment = PersistentList<Lazy<Value>>

fun normalize(
    term: TermS,
): TermS {
    val env: Environment = persistentListOf()
    return reify(env, reflect(env, term))
}

fun reflect(
    env: Environment,
    term: TermS,
): Value = when (term) {
    is TermS.UniverseS -> Value.UniverseS
    is TermS.EndS -> Value.EndS
    is TermS.ByteS -> Value.ByteS
    is TermS.ShortS -> Value.ShortS
    is TermS.IntS -> Value.IntS
    is TermS.LongS -> Value.LongS
    is TermS.FloatS -> Value.FloatS
    is TermS.DoubleS -> Value.DoubleS
    is TermS.StringS -> Value.StringS
    is TermS.ByteArrayS -> Value.ByteArrayS
    is TermS.IntArrayS -> Value.IntArrayS
    is TermS.LongArrayS -> Value.LongArrayS
    is TermS.ListS -> {
        val element = lazy { reflect(env, term.element) }
        Value.ListS(element)
    }

    is TermS.CompoundS -> {
        val elements = term.elements.mapValues { lazy { reflect(env, it.value) } }
        Value.CompoundS(elements)
    }

    is TermS.ArrowS -> {
        val dom = lazy { reflect(env, term.dom) }
        val cod = Clos(env, lazyOf(term.cod))
        Value.ArrowS(term.name, dom, cod)
    }

    is TermS.CodeS -> Value.CodeS(term.element)
    is TermS.TypeZ -> Value.TypeZ
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
        when (val operator = reflect(env, term.operator)) {
            is Value.Abs -> {
                val operand = lazy { reflect(env, term.operand) }
                operator.body(operand)
            }

            else -> {
                val operand = lazy { reflect(env, term.operand) }
                Value.Apply(operator, operand)
            }
        }
    }

    is TermS.Quote -> Value.Quote(term.element)
    is TermS.Let -> {
        val init = lazy { reflect(env, term.init) }
        reflect(env + init, term.next)
    }

    is TermS.Var -> env[term.level].value
    is TermS.Hole -> Value.Hole
}

fun reify(
    env: Environment,
    value: Value,
): TermS = when (value) {
    is Value.UniverseS -> TermS.UniverseS(Value.UniverseS)
    is Value.EndS -> TermS.EndS(Value.UniverseS)
    is Value.ByteS -> TermS.ByteS(Value.UniverseS)
    is Value.ShortS -> TermS.ShortS(Value.UniverseS)
    is Value.IntS -> TermS.IntS(Value.UniverseS)
    is Value.LongS -> TermS.LongS(Value.UniverseS)
    is Value.FloatS -> TermS.FloatS(Value.UniverseS)
    is Value.DoubleS -> TermS.DoubleS(Value.UniverseS)
    is Value.StringS -> TermS.StringS(Value.UniverseS)
    is Value.ByteArrayS -> TermS.ByteArrayS(Value.UniverseS)
    is Value.IntArrayS -> TermS.IntArrayS(Value.UniverseS)
    is Value.LongArrayS -> TermS.LongArrayS(Value.UniverseS)
    is Value.ListS -> {
        val element = reify(env, value.element.value)
        TermS.ListS(element, Value.UniverseS)
    }

    is Value.CompoundS -> {
        val elements = value.elements.mapValues { reify(env, it.value.value) }
        TermS.CompoundS(elements, Value.UniverseS)
    }

    is Value.IndexedElement -> {
        val index = reify(env, value.index.value)
        TermS.IndexedElement(value.target, index, value.type)
    }

    is Value.ArrowS -> {
        val dom = reify(env, value.dom.value)
        val x = lazyOf(Value.Var(value.name, env.size, value.dom))
        val cod = reify(env + x, value.cod(x))
        TermS.ArrowS(value.name, dom, cod, Value.UniverseS)
    }

    is Value.CodeS -> TermS.CodeS(value.element, Value.UniverseS)
    is Value.TypeZ -> TermS.TypeZ(Value.UniverseS)
    is Value.EndTag -> TermS.EndTag(Value.EndS)
    is Value.ByteTag -> TermS.ByteTag(value.data, Value.ByteS)
    is Value.ShortTag -> TermS.ShortTag(value.data, Value.ShortS)
    is Value.IntTag -> TermS.IntTag(value.data, Value.IntS)
    is Value.LongTag -> TermS.LongTag(value.data, Value.LongS)
    is Value.FloatTag -> TermS.FloatTag(value.data, Value.FloatS)
    is Value.DoubleTag -> TermS.DoubleTag(value.data, Value.DoubleS)
    is Value.StringTag -> TermS.StringTag(value.data, Value.StringS)
    is Value.ByteArrayTag -> {
        val elements = value.elements.map { reify(env, it.value) }
        TermS.ByteArrayTag(elements, Value.ByteArrayS)
    }

    is Value.IntArrayTag -> {
        val elements = value.elements.map { reify(env, it.value) }
        TermS.IntArrayTag(elements, Value.IntArrayS)
    }

    is Value.LongArrayTag -> {
        val elements = value.elements.map { reify(env, it.value) }
        TermS.LongArrayTag(elements, Value.LongArrayS)
    }

    is Value.ListTag -> {
        val elements = value.elements.map { reify(env, it.value) }
        val elementType = lazyOf(elements.firstOrNull()?.type ?: Value.EndS)
        TermS.ListTag(elements, Value.ListS(elementType))
    }

    is Value.CompoundTag -> {
        val elements = value.elements.mapValues { reify(env, it.value.value) }
        val elementTypes = elements.mapValues { lazyOf(it.value.type) }
        TermS.CompoundTag(elements, Value.CompoundS(elementTypes))
    }

    is Value.Abs -> {
        val anno = reify(env, value.anno.value)
        val x = lazyOf(Value.Var(value.name, env.size, value.anno))
        val body = reify(env + x, value.body(x))
        TermS.Abs(value.name, anno, body, Value.ArrowS(null, lazyOf(anno.type), Clos(env, lazy { reify(env, body.type) })))
    }

    is Value.Apply -> {
        val operator = reify(env, value.operator)
        val operand = reify(env, value.operand.value)
        val cod = (operator.type as Value.ArrowS).cod(value.operand)
        TermS.Apply(operator, operand, cod)
    }

    is Value.Quote -> TermS.Quote(value.element, Value.CodeS(value.element.type))
    is Value.Var -> TermS.Var(value.name, value.level, value.type.value)
    is Value.Hole -> TermS.Hole(Value.EndS)
}

operator fun Clos.invoke(
    argument: Lazy<Value>,
): Value = reflect(env + argument, body.value)
