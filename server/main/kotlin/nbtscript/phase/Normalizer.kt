package nbtscript.phase

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import nbtscript.ast.Core.*
import nbtscript.ast.Core.Kind.Sem
import nbtscript.ast.Core.Kind.Syn

typealias Environment = PersistentList<Lazy<TermS<Sem>>>

fun Unifier.reflectTypeZ(
    type: TypeZ<Syn>,
): TypeZ<Sem> = when (type) {
    is TypeZ.EndType -> cast(type)
    is TypeZ.ByteType -> cast(type)
    is TypeZ.ShortType -> cast(type)
    is TypeZ.IntType -> cast(type)
    is TypeZ.LongType -> cast(type)
    is TypeZ.FloatType -> cast(type)
    is TypeZ.DoubleType -> cast(type)
    is TypeZ.StringType -> cast(type)
    is TypeZ.ByteArrayType -> cast(type)
    is TypeZ.IntArrayType -> cast(type)
    is TypeZ.LongArrayType -> cast(type)
    is TypeZ.ListType -> {
        val element = reflectTypeZ(type.element)
        TypeZ.ListType(element)
    }

    is TypeZ.CollectionType -> {
        val element = reflectTypeZ(type.element)
        TypeZ.CollectionType(element)
    }

    is TypeZ.CompoundType -> {
        val elements = type.elements.mapValues { reflectTypeZ(it.value) }
        TypeZ.CompoundType(elements)
    }

    is TypeZ.Splice -> {
        when (val element = force(reflectTermS(persistentListOf(), type.element))) {
            is TermS.VQuoteType -> element.element.value
            else -> TypeZ.Splice(element)
        }
    }
    is TypeZ.Hole -> cast(type)
}

fun Unifier.reifyTypeZ(
    type: TypeZ<Sem>,
): TypeZ<Syn> = when (type) {
    is TypeZ.EndType -> cast(type)
    is TypeZ.ByteType -> cast(type)
    is TypeZ.ShortType -> cast(type)
    is TypeZ.IntType -> cast(type)
    is TypeZ.LongType -> cast(type)
    is TypeZ.FloatType -> cast(type)
    is TypeZ.DoubleType -> cast(type)
    is TypeZ.StringType -> cast(type)
    is TypeZ.ByteArrayType -> cast(type)
    is TypeZ.IntArrayType -> cast(type)
    is TypeZ.LongArrayType -> cast(type)
    is TypeZ.ListType -> {
        val element = reifyTypeZ(type.element)
        TypeZ.ListType(element)
    }

    is TypeZ.CollectionType -> {
        val element = reifyTypeZ(type.element)
        TypeZ.CollectionType(element)
    }

    is TypeZ.CompoundType -> {
        val elements = type.elements.mapValues { reifyTypeZ(it.value) }
        TypeZ.CompoundType(elements)
    }

    is TypeZ.Splice -> {
        val element = reifyTermS(persistentListOf(), type.element)
        TypeZ.Splice(element)
    }

    is TypeZ.Hole -> cast(type)
}

fun Unifier.normalize(
    term: TermS<Syn>,
): TermS<Syn> {
    val env: Environment = persistentListOf()
    return reifyTermS(env, reflectTermS(env, term))
}

fun Unifier.reflectTermS(
    env: Environment,
    term: TermS<Syn>,
): TermS<Sem> = when (term) {
    is TermS.UniverseType -> cast(term)
    is TermS.EndType -> cast(term)
    is TermS.ByteType -> cast(term)
    is TermS.ShortType -> cast(term)
    is TermS.IntType -> cast(term)
    is TermS.LongType -> cast(term)
    is TermS.FloatType -> cast(term)
    is TermS.DoubleType -> cast(term)
    is TermS.StringType -> cast(term)
    is TermS.ByteArrayType -> cast(term)
    is TermS.IntArrayType -> cast(term)
    is TermS.LongArrayType -> cast(term)
    is TermS.ListType -> {
        val element = lazy { reflectTermS(env, term.element) }
        TermS.VListType(element, term.type)
    }

    is TermS.CompoundType -> {
        val elements = term.elements.mapValues { lazy { reflectTermS(env, it.value) } }
        TermS.VCompoundType(elements, term.type)
    }

    is TermS.FunctionType -> {
        val dom = lazy { reflectTermS(env, term.dom) }
        val cod = Clos(env, lazyOf(term.cod))
        TermS.VFunctionType(term.name, dom, cod, term.type)
    }

    is TermS.CodeType -> {
        val element = lazy { reflectTypeZ(term.element) }
        TermS.VCodeType(element, term.type)
    }

    is TermS.TypeType -> cast(term)
    is TermS.EndTag -> cast(term)
    is TermS.ByteTag -> cast(term)
    is TermS.ShortTag -> cast(term)
    is TermS.IntTag -> cast(term)
    is TermS.LongTag -> cast(term)
    is TermS.FloatTag -> cast(term)
    is TermS.DoubleTag -> cast(term)
    is TermS.StringTag -> cast(term)
    is TermS.ByteArrayTag -> {
        val elements = term.elements.map { lazy { reflectTermS(env, it) } }
        TermS.VByteArrayTag(elements, term.type)
    }

    is TermS.IntArrayTag -> {
        val elements = term.elements.map { lazy { reflectTermS(env, it) } }
        TermS.VIntArrayTag(elements, term.type)
    }

    is TermS.LongArrayTag -> {
        val elements = term.elements.map { lazy { reflectTermS(env, it) } }
        TermS.VLongArrayTag(elements, term.type)
    }

    is TermS.ListTag -> {
        val elements = term.elements.map { lazy { reflectTermS(env, it) } }
        TermS.VListTag(elements, term.type)
    }

    is TermS.CompoundTag -> {
        val elements = term.elements.mapValues { lazy { reflectTermS(env, it.value) } }
        TermS.VCompoundTag(elements, term.type)
    }

    is TermS.IndexedElement -> {
        val index = lazy { reflectTermS(env, term.index) }
        TermS.VIndexedElement(term.target, index, term.type)
    }

    is TermS.Abs -> {
        val anno = lazy { reflectTermS(env, term.anno) }
        val body = Clos(env, lazyOf(term.body))
        TermS.VAbs(term.name, anno, body, term.type)
    }

    is TermS.Apply -> {
        when (val operator = force(reflectTermS(env, term.operator))) {
            is TermS.VAbs -> {
                val operand = lazy { reflectTermS(env, term.operand) }
                operator.body(this, operand)
            }

            else -> {
                val operand = lazy { reflectTermS(env, term.operand) }
                TermS.VApply(operator, operand, term.type)
            }
        }
    }

    is TermS.QuoteType -> {
        val element = lazy { reflectTypeZ(term.element) }
        TermS.VQuoteType(element, term.type)
    }

    is TermS.QuoteTerm -> cast(term)
    is TermS.Let -> {
        val init = lazy { reflectTermS(env, term.init) }
        reflectTermS(env + init, term.next)
    }

    is TermS.Var -> env[term.level].value
    is TermS.Meta -> cast(term)
    is TermS.Hole -> cast(term)
    else -> unreachable()
}

fun Unifier.reifyTermS(
    env: Environment,
    term: TermS<Sem>,
): TermS<Syn> = when (@Suppress("NAME_SHADOWING") val term = force(term)) {
    is TermS.UniverseType -> cast(term)
    is TermS.EndType -> cast(term)
    is TermS.ByteType -> cast(term)
    is TermS.ShortType -> cast(term)
    is TermS.IntType -> cast(term)
    is TermS.LongType -> cast(term)
    is TermS.FloatType -> cast(term)
    is TermS.DoubleType -> cast(term)
    is TermS.StringType -> cast(term)
    is TermS.ByteArrayType -> cast(term)
    is TermS.IntArrayType -> cast(term)
    is TermS.LongArrayType -> cast(term)
    is TermS.VListType -> {
        val element = reifyTermS(env, term.element.value)
        TermS.ListType(element, term.type)
    }

    is TermS.VCompoundType -> {
        val elements = term.elements.mapValues { reifyTermS(env, it.value.value) }
        TermS.CompoundType(elements, term.type)
    }

    is TermS.VIndexedElement -> {
        val index = reifyTermS(env, term.index.value)
        TermS.IndexedElement(term.target, index, term.type)
    }

    is TermS.VFunctionType -> {
        val dom = reifyTermS(env, term.dom.value)
        val x = lazyOf(TermS.Var<Sem>(term.name, env.size, term.dom.value))
        val cod = reifyTermS(env + x, term.cod(this, x))
        TermS.FunctionType(term.name, dom, cod, term.type)
    }

    is TermS.VCodeType -> {
        val element = reifyTypeZ(term.element.value)
        TermS.CodeType(element, term.type)
    }

    is TermS.TypeType -> cast(term)
    is TermS.EndTag -> cast(term)
    is TermS.ByteTag -> cast(term)
    is TermS.ShortTag -> cast(term)
    is TermS.IntTag -> cast(term)
    is TermS.LongTag -> cast(term)
    is TermS.FloatTag -> cast(term)
    is TermS.DoubleTag -> cast(term)
    is TermS.StringTag -> cast(term)
    is TermS.VByteArrayTag -> {
        val elements = term.elements.map { reifyTermS(env, it.value) }
        TermS.ByteArrayTag(elements, term.type)
    }

    is TermS.VIntArrayTag -> {
        val elements = term.elements.map { reifyTermS(env, it.value) }
        TermS.IntArrayTag(elements, term.type)
    }

    is TermS.VLongArrayTag -> {
        val elements = term.elements.map { reifyTermS(env, it.value) }
        TermS.LongArrayTag(elements, term.type)
    }

    is TermS.VListTag -> {
        val elements = term.elements.map { reifyTermS(env, it.value) }
        TermS.ListTag(elements, term.type)
    }

    is TermS.VCompoundTag -> {
        val elements = term.elements.mapValues { reifyTermS(env, it.value.value) }
        TermS.CompoundTag(elements, term.type)
    }

    is TermS.VAbs -> {
        val anno = reifyTermS(env, term.anno.value)
        val x = lazyOf(TermS.Var<Sem>(term.name, env.size, term.anno.value))
        val body = reifyTermS(env + x, term.body(this, x))
        TermS.Abs(term.name, anno, body, term.type)
    }

    is TermS.VApply -> {
        val operator = reifyTermS(env, term.operator)
        val operand = reifyTermS(env, term.operand.value)
        val cod = when (val operatorType = operator.type) {
            is TermS.VFunctionType -> operatorType.cod(this, term.operand)
            else -> operatorType
        }
        TermS.Apply(operator, operand, cod)
    }

    is TermS.VQuoteType -> {
        val element = reifyTypeZ(term.element.value)
        TermS.QuoteType(element, term.type)
    }

    is TermS.QuoteTerm -> TermS.QuoteTerm(term.element, term.type)
    is TermS.Var -> cast(term)
    is TermS.Meta -> cast(term)
    is TermS.Hole -> cast(term)
    else -> unreachable()
}

operator fun Clos.invoke(
    unifier: Unifier,
    argument: Lazy<TermS<Sem>>,
): TermS<Sem> = unifier.reflectTermS(env + argument, body.value)
