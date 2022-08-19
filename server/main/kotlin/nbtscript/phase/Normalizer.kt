package nbtscript.phase

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import nbtscript.ast.Core.*

typealias Environment = PersistentList<Lazy<VTermS>>

fun Unifier.reflectTypeZ(
    type: TypeZ<Kind.Syn>,
): TypeZ<Kind.Sem> = when (type) {
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
            is VTermS.QuoteType -> element.element.value
            else -> TypeZ.VSplice(element)
        }
    }

    is TypeZ.Hole -> cast(type)
    else -> error("unreachable")
}

fun Unifier.reifyTypeZ(
    type: TypeZ<Kind.Sem>,
): TypeZ<Kind.Syn> = when (type) {
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

    is TypeZ.VSplice -> {
        val element = reifyTermS(persistentListOf(), type.element)
        TypeZ.Splice(element)
    }

    is TypeZ.Hole -> cast(type)
    else -> error("unreachable")
}

fun Unifier.normalize(
    term: TermS,
): TermS {
    val env: Environment = persistentListOf()
    return reifyTermS(env, reflectTermS(env, term))
}

fun Unifier.reflectTermS(
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
        val element = lazy { reflectTermS(env, term.element) }
        VTermS.ListType(element)
    }

    is TermS.CompoundType -> {
        val elements = term.elements.mapValues { lazy { reflectTermS(env, it.value) } }
        VTermS.CompoundType(elements)
    }

    is TermS.FunctionType -> {
        val dom = lazy { reflectTermS(env, term.dom) }
        val cod = Clos(env, lazyOf(term.cod))
        VTermS.FunctionType(term.name, dom, cod)
    }

    is TermS.CodeType -> {
        val element = lazy { reflectTypeZ(term.element) }
        VTermS.CodeType(element)
    }

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
        val elements = term.elements.map { lazy { reflectTermS(env, it) } }
        VTermS.ByteArrayTag(elements)
    }

    is TermS.IntArrayTag -> {
        val elements = term.elements.map { lazy { reflectTermS(env, it) } }
        VTermS.IntArrayTag(elements)
    }

    is TermS.LongArrayTag -> {
        val elements = term.elements.map { lazy { reflectTermS(env, it) } }
        VTermS.LongArrayTag(elements)
    }

    is TermS.ListTag -> {
        val elements = term.elements.map { lazy { reflectTermS(env, it) } }
        VTermS.ListTag(elements)
    }

    is TermS.CompoundTag -> {
        val elements = term.elements.mapValues { lazy { reflectTermS(env, it.value) } }
        VTermS.CompoundTag(elements)
    }

    is TermS.IndexedElement -> {
        val index = lazy { reflectTermS(env, term.index) }
        VTermS.IndexedElement(term.target, index, term.type)
    }

    is TermS.Abs -> {
        val anno = lazy { reflectTermS(env, term.anno) }
        val body = Clos(env, lazyOf(term.body))
        VTermS.Abs(term.name, anno, body)
    }

    is TermS.Apply -> {
        when (val operator = force(reflectTermS(env, term.operator))) {
            is VTermS.Abs -> {
                val operand = lazy { reflectTermS(env, term.operand) }
                operator.body(this, operand)
            }

            else -> {
                val operand = lazy { reflectTermS(env, term.operand) }
                VTermS.Apply(operator, operand)
            }
        }
    }

    is TermS.QuoteType -> {
        val element = lazy { reflectTypeZ(term.element) }
        VTermS.QuoteType(element)
    }

    is TermS.QuoteTerm -> VTermS.QuoteTerm(term.element)
    is TermS.Let -> {
        val init = lazy { reflectTermS(env, term.init) }
        reflectTermS(env + init, term.next)
    }

    is TermS.Var -> env[term.level].value
    is TermS.Meta -> VTermS.Meta(term.index, term.type)
    is TermS.Hole -> VTermS.Hole
}

fun Unifier.reifyTermS(
    env: Environment,
    term: VTermS,
): TermS = when (@Suppress("NAME_SHADOWING") val term = force(term)) {
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
        val element = reifyTermS(env, term.element.value)
        TermS.ListType(element, VTermS.UniverseType)
    }

    is VTermS.CompoundType -> {
        val elements = term.elements.mapValues { reifyTermS(env, it.value.value) }
        TermS.CompoundType(elements, VTermS.UniverseType)
    }

    is VTermS.IndexedElement -> {
        val index = reifyTermS(env, term.index.value)
        TermS.IndexedElement(term.target, index, term.type)
    }

    is VTermS.FunctionType -> {
        val dom = reifyTermS(env, term.dom.value)
        val x = lazyOf(VTermS.Var(term.name, env.size, term.dom))
        val cod = reifyTermS(env + x, term.cod(this, x))
        TermS.FunctionType(term.name, dom, cod, VTermS.UniverseType)
    }

    is VTermS.CodeType -> {
        val element = reifyTypeZ(term.element.value)
        TermS.CodeType(element, VTermS.UniverseType)
    }

    is VTermS.TypeType -> TermS.TypeType(VTermS.UniverseType)
    is VTermS.EndTag -> TermS.EndTag(VTermS.EndType)
    is VTermS.ByteTag -> TermS.ByteTag(term.data, VTermS.ByteType)
    is VTermS.ShortTag -> TermS.ShortTag(term.data, VTermS.ShortType)
    is VTermS.IntTag -> TermS.IntTag(term.data, VTermS.IntType)
    is VTermS.LongTag -> TermS.LongTag(term.data, VTermS.LongType)
    is VTermS.FloatTag -> TermS.FloatTag(term.data, VTermS.FloatType)
    is VTermS.DoubleTag -> TermS.DoubleTag(term.data, VTermS.DoubleType)
    is VTermS.StringTag -> TermS.StringTag(term.data, VTermS.StringType)
    is VTermS.ByteArrayTag -> {
        val elements = term.elements.map { reifyTermS(env, it.value) }
        TermS.ByteArrayTag(elements, VTermS.ByteArrayType)
    }

    is VTermS.IntArrayTag -> {
        val elements = term.elements.map { reifyTermS(env, it.value) }
        TermS.IntArrayTag(elements, VTermS.IntArrayType)
    }

    is VTermS.LongArrayTag -> {
        val elements = term.elements.map { reifyTermS(env, it.value) }
        TermS.LongArrayTag(elements, VTermS.LongArrayType)
    }

    is VTermS.ListTag -> {
        val elements = term.elements.map { reifyTermS(env, it.value) }
        val elementType = lazyOf(elements.firstOrNull()?.type ?: VTermS.EndType)
        TermS.ListTag(elements, VTermS.ListType(elementType))
    }

    is VTermS.CompoundTag -> {
        val elements = term.elements.mapValues { reifyTermS(env, it.value.value) }
        val elementTypes = elements.mapValues { lazyOf(it.value.type) }
        TermS.CompoundTag(elements, VTermS.CompoundType(elementTypes))
    }

    is VTermS.Abs -> {
        val anno = reifyTermS(env, term.anno.value)
        val x = lazyOf(VTermS.Var(term.name, env.size, term.anno))
        val body = reifyTermS(env + x, term.body(this, x))
        TermS.Abs(term.name, anno, body, VTermS.FunctionType(null, lazyOf(anno.type), Clos(env, lazy { reifyTermS(env, body.type) })))
    }

    is VTermS.Apply -> {
        val operator = reifyTermS(env, term.operator)
        val operand = reifyTermS(env, term.operand.value)
        val cod = when (val operatorType = operator.type) {
            is VTermS.FunctionType -> operatorType.cod(this, term.operand)
            else -> operatorType
        }
        TermS.Apply(operator, operand, cod)
    }

    is VTermS.QuoteType -> {
        val element = reifyTypeZ(term.element.value)
        TermS.QuoteType(element, VTermS.TypeType)
    }

    is VTermS.QuoteTerm -> TermS.QuoteTerm(term.element, VTermS.CodeType(lazyOf(term.element.type)))
    is VTermS.Var -> TermS.Var(term.name, term.level, term.type.value)
    is VTermS.Meta -> TermS.Meta(term.index, term.type)
    is VTermS.Hole -> TermS.Hole(VTermS.EndType)
}

operator fun Clos.invoke(
    unifier: Unifier,
    argument: Lazy<VTermS>,
): VTermS = unifier.reflectTermS(env + argument, body.value)

private inline fun <reified A, reified B> cast(a: A): B = a as B
