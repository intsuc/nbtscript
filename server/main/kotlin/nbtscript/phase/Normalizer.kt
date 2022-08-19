package nbtscript.phase

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import nbtscript.ast.Core.*

typealias Environment = PersistentList<Lazy<VTermS>>

fun Unifier.normalize(
    term: TermS,
): TermS {
    val env: Environment = persistentListOf()
    return reify(env, reflect(env, term))
}

fun Unifier.reflect(
    env: Environment,
    term: TermS,
): VTermS = when (term) {
    is TermS.UniverseType -> VTermS.UniverseType
    is TermS.EndType -> VTermS.EndType
    is TermS.ByteType -> VTermS.ByteType
    is TermS.ShortType -> VTermS.ShortType
    is TermS.IntType -> VTermS.IntType
    is TermS.LongType -> VTermS.LongType
    is TermS.FloatType -> VTermS.FloatType
    is TermS.DoubleType -> VTermS.DoubleType
    is TermS.StringType -> VTermS.StringType
    is TermS.ByteArrayType -> VTermS.ByteArrayType
    is TermS.IntArrayType -> VTermS.IntArrayType
    is TermS.LongArrayType -> VTermS.LongArrayType
    is TermS.ListType -> {
        val element = lazy { reflect(env, term.element) }
        VTermS.ListType(element)
    }

    is TermS.CompoundType -> {
        val elements = term.elements.mapValues { lazy { reflect(env, it.value) } }
        VTermS.CompoundType(elements)
    }

    is TermS.FunctionType -> {
        val dom = lazy { reflect(env, term.dom) }
        val cod = Clos(env, lazyOf(term.cod))
        VTermS.FunctionType(term.name, dom, cod)
    }

    is TermS.CodeType -> VTermS.CodeType(term.element)
    is TermS.TypeType -> VTermS.TypeType
    is TermS.EndTag -> VTermS.EndTag
    is TermS.ByteTag -> VTermS.ByteTag(term.data)
    is TermS.ShortTag -> VTermS.ShortTag(term.data)
    is TermS.IntTag -> VTermS.IntTag(term.data)
    is TermS.LongTag -> VTermS.LongTag(term.data)
    is TermS.FloatTag -> VTermS.FloatTag(term.data)
    is TermS.DoubleTag -> VTermS.DoubleTag(term.data)
    is TermS.StringTag -> VTermS.StringTag(term.data)
    is TermS.ByteArrayTag -> {
        val elements = term.elements.map { lazy { reflect(env, it) } }
        VTermS.ByteArrayTag(elements)
    }

    is TermS.IntArrayTag -> {
        val elements = term.elements.map { lazy { reflect(env, it) } }
        VTermS.IntArrayTag(elements)
    }

    is TermS.LongArrayTag -> {
        val elements = term.elements.map { lazy { reflect(env, it) } }
        VTermS.LongArrayTag(elements)
    }

    is TermS.ListTag -> {
        val elements = term.elements.map { lazy { reflect(env, it) } }
        VTermS.ListTag(elements)
    }

    is TermS.CompoundTag -> {
        val elements = term.elements.mapValues { lazy { reflect(env, it.value) } }
        VTermS.CompoundTag(elements)
    }

    is TermS.IndexedElement -> {
        val index = lazy { reflect(env, term.index) }
        VTermS.IndexedElement(term.target, index, term.type)
    }

    is TermS.Abs -> {
        val anno = lazy { reflect(env, term.anno) }
        val body = Clos(env, lazyOf(term.body))
        VTermS.Abs(term.name, anno, body)
    }

    is TermS.Apply -> {
        when (val operator = force(reflect(env, term.operator))) {
            is VTermS.Abs -> {
                val operand = lazy { reflect(env, term.operand) }
                operator.body(this, operand)
            }

            else -> {
                val operand = lazy { reflect(env, term.operand) }
                VTermS.Apply(operator, operand)
            }
        }
    }

    is TermS.QuoteType -> VTermS.QuoteType(term.element)
    is TermS.QuoteTerm -> VTermS.QuoteTerm(term.element)
    is TermS.Let -> {
        val init = lazy { reflect(env, term.init) }
        reflect(env + init, term.next)
    }

    is TermS.Var -> env[term.level].value
    is TermS.Meta -> VTermS.Meta(term.index, term.type)
    is TermS.Hole -> VTermS.Hole
}

fun Unifier.reify(
    env: Environment,
    term: VTermS,
): TermS = when (@Suppress("NAME_SHADOWING") val value = force(term)) {
    is VTermS.UniverseType -> TermS.UniverseType(VTermS.UniverseType)
    is VTermS.EndType -> TermS.EndType(VTermS.UniverseType)
    is VTermS.ByteType -> TermS.ByteType(VTermS.UniverseType)
    is VTermS.ShortType -> TermS.ShortType(VTermS.UniverseType)
    is VTermS.IntType -> TermS.IntType(VTermS.UniverseType)
    is VTermS.LongType -> TermS.LongType(VTermS.UniverseType)
    is VTermS.FloatType -> TermS.FloatType(VTermS.UniverseType)
    is VTermS.DoubleType -> TermS.DoubleType(VTermS.UniverseType)
    is VTermS.StringType -> TermS.StringType(VTermS.UniverseType)
    is VTermS.ByteArrayType -> TermS.ByteArrayType(VTermS.UniverseType)
    is VTermS.IntArrayType -> TermS.IntArrayType(VTermS.UniverseType)
    is VTermS.LongArrayType -> TermS.LongArrayType(VTermS.UniverseType)
    is VTermS.ListType -> {
        val element = reify(env, value.element.value)
        TermS.ListType(element, VTermS.UniverseType)
    }

    is VTermS.CompoundType -> {
        val elements = value.elements.mapValues { reify(env, it.value.value) }
        TermS.CompoundType(elements, VTermS.UniverseType)
    }

    is VTermS.IndexedElement -> {
        val index = reify(env, value.index.value)
        TermS.IndexedElement(value.target, index, value.type)
    }

    is VTermS.FunctionType -> {
        val dom = reify(env, value.dom.value)
        val x = lazyOf(VTermS.Var(value.name, env.size, value.dom))
        val cod = reify(env + x, value.cod(this, x))
        TermS.FunctionType(value.name, dom, cod, VTermS.UniverseType)
    }

    is VTermS.CodeType -> TermS.CodeType(value.element, VTermS.UniverseType)
    is VTermS.TypeType -> TermS.TypeType(VTermS.UniverseType)
    is VTermS.EndTag -> TermS.EndTag(VTermS.EndType)
    is VTermS.ByteTag -> TermS.ByteTag(value.data, VTermS.ByteType)
    is VTermS.ShortTag -> TermS.ShortTag(value.data, VTermS.ShortType)
    is VTermS.IntTag -> TermS.IntTag(value.data, VTermS.IntType)
    is VTermS.LongTag -> TermS.LongTag(value.data, VTermS.LongType)
    is VTermS.FloatTag -> TermS.FloatTag(value.data, VTermS.FloatType)
    is VTermS.DoubleTag -> TermS.DoubleTag(value.data, VTermS.DoubleType)
    is VTermS.StringTag -> TermS.StringTag(value.data, VTermS.StringType)
    is VTermS.ByteArrayTag -> {
        val elements = value.elements.map { reify(env, it.value) }
        TermS.ByteArrayTag(elements, VTermS.ByteArrayType)
    }

    is VTermS.IntArrayTag -> {
        val elements = value.elements.map { reify(env, it.value) }
        TermS.IntArrayTag(elements, VTermS.IntArrayType)
    }

    is VTermS.LongArrayTag -> {
        val elements = value.elements.map { reify(env, it.value) }
        TermS.LongArrayTag(elements, VTermS.LongArrayType)
    }

    is VTermS.ListTag -> {
        val elements = value.elements.map { reify(env, it.value) }
        val elementType = lazyOf(elements.firstOrNull()?.type ?: VTermS.EndType)
        TermS.ListTag(elements, VTermS.ListType(elementType))
    }

    is VTermS.CompoundTag -> {
        val elements = value.elements.mapValues { reify(env, it.value.value) }
        val elementTypes = elements.mapValues { lazyOf(it.value.type) }
        TermS.CompoundTag(elements, VTermS.CompoundType(elementTypes))
    }

    is VTermS.Abs -> {
        val anno = reify(env, value.anno.value)
        val x = lazyOf(VTermS.Var(value.name, env.size, value.anno))
        val body = reify(env + x, value.body(this, x))
        TermS.Abs(value.name, anno, body, VTermS.FunctionType(null, lazyOf(anno.type), Clos(env, lazy { reify(env, body.type) })))
    }

    is VTermS.Apply -> {
        val operator = reify(env, value.operator)
        val operand = reify(env, value.operand.value)
        val cod = when (val operatorType = operator.type) {
            is VTermS.FunctionType -> operatorType.cod(this, value.operand)
            else -> operatorType
        }
        TermS.Apply(operator, operand, cod)
    }

    is VTermS.QuoteType -> TermS.QuoteType(value.element, VTermS.TypeType)
    is VTermS.QuoteTerm -> TermS.QuoteTerm(value.element, VTermS.CodeType(value.element.type))
    is VTermS.Var -> TermS.Var(value.name, value.level, value.type.value)
    is VTermS.Meta -> TermS.Meta(value.index, value.type)
    is VTermS.Hole -> TermS.Hole(VTermS.EndType)
}

operator fun Clos.invoke(
    unifier: Unifier,
    argument: Lazy<VTermS>,
): VTermS = unifier.reflect(env + argument, body.value)
