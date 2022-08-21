package nbtscript.phase

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import nbtscript.ast.Core.*
import nbtscript.ast.Core.Kind.Sem
import nbtscript.ast.Core.Kind.Syn
import nbtscript.unreachable

typealias Environment = PersistentList<Lazy<TermS<Sem>>>

fun Unifier.reflectTypeZ(
    env: Environment,
    type: TypeZ<Syn>,
): TypeZ<Sem> = when (type) {
    is TypeZ.EndType -> TypeZ.reflect(type)
    is TypeZ.ByteType -> TypeZ.reflect(type)
    is TypeZ.ShortType -> TypeZ.reflect(type)
    is TypeZ.IntType -> TypeZ.reflect(type)
    is TypeZ.LongType -> TypeZ.reflect(type)
    is TypeZ.FloatType -> TypeZ.reflect(type)
    is TypeZ.DoubleType -> TypeZ.reflect(type)
    is TypeZ.StringType -> TypeZ.reflect(type)
    is TypeZ.CollectionType -> {
        val element = reflectTypeZ(env, type.element)
        TypeZ.CollectionType(element)
    }

    is TypeZ.ByteArrayType -> TypeZ.reflect(type)
    is TypeZ.IntArrayType -> TypeZ.reflect(type)
    is TypeZ.LongArrayType -> TypeZ.reflect(type)
    is TypeZ.ListType -> {
        val element = reflectTypeZ(env, type.element)
        TypeZ.ListType(element)
    }

    is TypeZ.CompoundType -> {
        val elements = type.elements.mapValues { reflectTypeZ(env, it.value) }
        TypeZ.CompoundType(elements)
    }

    is TypeZ.Splice -> {
        when (val element = force(reflectTermS(env, type.element))) {
            is TermS.VQuoteType -> element.element.value
            else -> TypeZ.Splice(element)
        }
    }

    is TypeZ.Hole -> TypeZ.reflect(type)
}

fun Unifier.reifyTypeZ(
    env: Environment,
    type: TypeZ<Sem>,
): TypeZ<Syn> = when (type) {
    is TypeZ.EndType -> TypeZ.reify(type)
    is TypeZ.ByteType -> TypeZ.reify(type)
    is TypeZ.ShortType -> TypeZ.reify(type)
    is TypeZ.IntType -> TypeZ.reify(type)
    is TypeZ.LongType -> TypeZ.reify(type)
    is TypeZ.FloatType -> TypeZ.reify(type)
    is TypeZ.DoubleType -> TypeZ.reify(type)
    is TypeZ.StringType -> TypeZ.reify(type)
    is TypeZ.CollectionType -> {
        val element = reifyTypeZ(env, type.element)
        TypeZ.CollectionType(element)
    }

    is TypeZ.ByteArrayType -> TypeZ.reify(type)
    is TypeZ.IntArrayType -> TypeZ.reify(type)
    is TypeZ.LongArrayType -> TypeZ.reify(type)
    is TypeZ.ListType -> {
        val element = reifyTypeZ(env, type.element)
        TypeZ.ListType(element)
    }

    is TypeZ.CompoundType -> {
        val elements = type.elements.mapValues { reifyTypeZ(env, it.value) }
        TypeZ.CompoundType(elements)
    }

    is TypeZ.Splice -> {
        val element = reifyTermS(env, type.element)
        TypeZ.Splice(element)
    }

    is TypeZ.Hole -> TypeZ.reify(type)
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
    is TermS.UniverseType -> TermS.reflect(term)
    is TermS.EndType -> TermS.reflect(term)
    is TermS.ByteType -> TermS.reflect(term)
    is TermS.ShortType -> TermS.reflect(term)
    is TermS.IntType -> TermS.reflect(term)
    is TermS.LongType -> TermS.reflect(term)
    is TermS.FloatType -> TermS.reflect(term)
    is TermS.DoubleType -> TermS.reflect(term)
    is TermS.StringType -> TermS.reflect(term)
    is TermS.ByteArrayType -> TermS.reflect(term)
    is TermS.IntArrayType -> TermS.reflect(term)
    is TermS.LongArrayType -> TermS.reflect(term)
    is TermS.ListType -> {
        val element = lazy { reflectTermS(env, term.element) }
        TermS.VListType(element)
    }

    is TermS.CompoundType -> {
        val elements = term.elements.mapValues { lazy { reflectTermS(env, it.value) } }
        TermS.VCompoundType(elements)
    }

    is TermS.NodeType -> TermS.reflect(term)
    is TermS.FunType -> {
        val dom = lazy { reflectTermS(env, term.dom) }
        val cod = Clos(env, lazyOf(term.cod))
        TermS.VFunType(term.name, dom, cod)
    }

    is TermS.CodeType -> {
        val element = lazy { reflectTypeZ(env, term.element) }
        TermS.VCodeType(element)
    }

    is TermS.TypeType -> TermS.reflect(term)
    is TermS.EndTag -> TermS.reflect(term)
    is TermS.ByteTag -> TermS.reflect(term)
    is TermS.ShortTag -> TermS.reflect(term)
    is TermS.IntTag -> TermS.reflect(term)
    is TermS.LongTag -> TermS.reflect(term)
    is TermS.FloatTag -> TermS.reflect(term)
    is TermS.DoubleTag -> TermS.reflect(term)
    is TermS.StringTag -> TermS.reflect(term)
    is TermS.ByteArrayTag -> {
        val elements = term.elements.map { lazy { reflectTermS(env, it) } }
        TermS.VByteArrayTag(elements)
    }

    is TermS.IntArrayTag -> {
        val elements = term.elements.map { lazy { reflectTermS(env, it) } }
        TermS.VIntArrayTag(elements)
    }

    is TermS.LongArrayTag -> {
        val elements = term.elements.map { lazy { reflectTermS(env, it) } }
        TermS.VLongArrayTag(elements)
    }

    is TermS.ListTag -> {
        val elements = term.elements.map { lazy { reflectTermS(env, it) } }
        TermS.VListTag(elements, term.type)
    }

    is TermS.CompoundTag -> {
        val elements = term.elements.mapValues { lazy { reflectTermS(env, it.value) } }
        TermS.VCompoundTag(elements, term.type)
    }

    is TermS.MatchObjectNode -> {
        val pattern = lazy { reflectTermS(env, term.pattern) }
        TermS.VMatchObjectNode(pattern)
    }

    is TermS.MatchElementNode -> {
        val pattern = lazy { reflectTermS(env, term.pattern) }
        TermS.VMatchElementNode(pattern)
    }

    is TermS.AllElementsNode -> TermS.reflect(term)
    is TermS.IndexedElementNode -> {
        val pattern = lazy { reflectTermS(env, term.index) }
        TermS.VIndexedElementNode(pattern)
    }

    is TermS.CompoundChildNode -> {
        val pattern = lazy { reflectTermS(env, term.name) }
        TermS.VCompoundChildNode(pattern)
    }

    is TermS.Get -> {
        val target = lazy { reflectTermS(env, term.target) }
        val path = lazy { reflectTermS(env, term.path) }
        TermS.VGet(target, path, term.type)
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
        val element = lazy { reflectTypeZ(env, term.element) }
        TermS.VQuoteType(element)
    }

    is TermS.QuoteTerm -> TermS.reflect(term)
    is TermS.Let -> {
        val init = lazy { reflectTermS(env, term.init) }
        reflectTermS(env + init, term.next)
    }

    is TermS.Var -> env[term.level].value
    is TermS.Meta -> TermS.reflect(term)
    is TermS.Hole -> TermS.reflect(term)
    else -> unreachable()
}

fun Unifier.reifyTermS(
    env: Environment,
    term: TermS<Sem>,
): TermS<Syn> = when (@Suppress("NAME_SHADOWING") val term = force(term)) {
    is TermS.UniverseType -> TermS.reify(term)
    is TermS.EndType -> TermS.reify(term)
    is TermS.ByteType -> TermS.reify(term)
    is TermS.ShortType -> TermS.reify(term)
    is TermS.IntType -> TermS.reify(term)
    is TermS.LongType -> TermS.reify(term)
    is TermS.FloatType -> TermS.reify(term)
    is TermS.DoubleType -> TermS.reify(term)
    is TermS.StringType -> TermS.reify(term)
    is TermS.ByteArrayType -> TermS.reify(term)
    is TermS.IntArrayType -> TermS.reify(term)
    is TermS.LongArrayType -> TermS.reify(term)
    is TermS.VListType -> {
        val element = reifyTermS(env, term.element.value)
        TermS.ListType(element)
    }

    is TermS.VCompoundType -> {
        val elements = term.elements.mapValues { reifyTermS(env, it.value.value) }
        TermS.CompoundType(elements)
    }

    is TermS.NodeType -> TermS.reify(term)
    is TermS.VMatchObjectNode -> {
        val pattern = reifyTermS(env, term.pattern.value)
        TermS.MatchObjectNode(pattern)
    }

    is TermS.VMatchElementNode -> {
        val pattern = reifyTermS(env, term.pattern.value)
        TermS.MatchElementNode(pattern)
    }

    is TermS.AllElementsNode -> TermS.reify(term)
    is TermS.VIndexedElementNode -> {
        val pattern = reifyTermS(env, term.index.value)
        TermS.IndexedElementNode(pattern)
    }

    is TermS.VCompoundChildNode -> {
        val pattern = reifyTermS(env, term.name.value)
        TermS.CompoundChildNode(pattern)
    }

    is TermS.VFunType -> {
        val dom = reifyTermS(env, term.dom.value)
        val x = lazyOf(TermS.Var<Sem>(term.name, env.size, term.dom.value))
        val cod = reifyTermS(env + x, term.cod(this, x))
        TermS.FunType(term.name, dom, cod)
    }

    is TermS.VCodeType -> {
        val element = reifyTypeZ(env, term.element.value)
        TermS.CodeType(element)
    }

    is TermS.TypeType -> TermS.reify(term)
    is TermS.EndTag -> TermS.reify(term)
    is TermS.ByteTag -> TermS.reify(term)
    is TermS.ShortTag -> TermS.reify(term)
    is TermS.IntTag -> TermS.reify(term)
    is TermS.LongTag -> TermS.reify(term)
    is TermS.FloatTag -> TermS.reify(term)
    is TermS.DoubleTag -> TermS.reify(term)
    is TermS.StringTag -> TermS.reify(term)
    is TermS.VByteArrayTag -> {
        val elements = term.elements.map { reifyTermS(env, it.value) }
        TermS.ByteArrayTag(elements)
    }

    is TermS.VIntArrayTag -> {
        val elements = term.elements.map { reifyTermS(env, it.value) }
        TermS.IntArrayTag(elements)
    }

    is TermS.VLongArrayTag -> {
        val elements = term.elements.map { reifyTermS(env, it.value) }
        TermS.LongArrayTag(elements)
    }

    is TermS.VListTag -> {
        val elements = term.elements.map { reifyTermS(env, it.value) }
        TermS.ListTag(elements, term.type)
    }

    is TermS.VCompoundTag -> {
        val elements = term.elements.mapValues { reifyTermS(env, it.value.value) }
        TermS.CompoundTag(elements, term.type)
    }

    is TermS.VGet -> {
        val target = reifyTermS(env, term.target.value)
        val path = reifyTermS(env, term.path.value)
        TermS.Get(target, path, term.type)
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
            is TermS.VFunType -> operatorType.cod(this, term.operand)
            else -> operatorType
        }
        TermS.Apply(operator, operand, cod)
    }

    is TermS.VQuoteType -> {
        val element = reifyTypeZ(env, term.element.value)
        TermS.QuoteType(element)
    }

    is TermS.QuoteTerm -> TermS.reify(term)
    is TermS.Var -> TermS.reify(term)
    is TermS.Meta -> TermS.reify(term)
    is TermS.Hole -> TermS.reify(term)
    else -> unreachable()
}

operator fun Clos.invoke(
    unifier: Unifier,
    argument: Lazy<TermS<Sem>>,
): TermS<Sem> = unifier.reflectTermS(env + argument, body.value)
