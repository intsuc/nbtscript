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
    is TypeZ.EndType -> type
    is TypeZ.ByteType -> type
    is TypeZ.ShortType -> type
    is TypeZ.IntType -> type
    is TypeZ.LongType -> type
    is TypeZ.FloatType -> type
    is TypeZ.DoubleType -> type
    is TypeZ.StringType -> type
    is TypeZ.CollectionType -> {
        val element = reflectTypeZ(env, type.element)
        TypeZ.CollectionType(element)
    }

    is TypeZ.ByteArrayType -> type
    is TypeZ.IntArrayType -> type
    is TypeZ.LongArrayType -> type
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
            is TermS.VQuoteTypeZ -> element.element.value
            else -> TypeZ.Splice(element)
        }
    }

    is TypeZ.Hole -> type
}

fun Unifier.reifyTypeZ(
    env: Environment,
    type: TypeZ<Sem>,
): TypeZ<Syn> = when (type) {
    is TypeZ.EndType -> type
    is TypeZ.ByteType -> type
    is TypeZ.ShortType -> type
    is TypeZ.IntType -> type
    is TypeZ.LongType -> type
    is TypeZ.FloatType -> type
    is TypeZ.DoubleType -> type
    is TypeZ.StringType -> type
    is TypeZ.CollectionType -> {
        val element = reifyTypeZ(env, type.element)
        TypeZ.CollectionType(element)
    }

    is TypeZ.ByteArrayType -> type
    is TypeZ.IntArrayType -> type
    is TypeZ.LongArrayType -> type
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

    is TypeZ.Hole -> type
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
    is TermS.UniverseType -> term
    is TermS.EndType -> term
    is TermS.ByteType -> term
    is TermS.ShortType -> term
    is TermS.IntType -> term
    is TermS.LongType -> term
    is TermS.FloatType -> term
    is TermS.DoubleType -> term
    is TermS.StringType -> term
    is TermS.ByteArrayType -> term
    is TermS.IntArrayType -> term
    is TermS.LongArrayType -> term
    is TermS.ListType -> {
        val element = lazy { reflectTermS(env, term.element) }
        TermS.VListType(element)
    }

    is TermS.CompoundType -> {
        val elements = term.elements.mapValues { lazy { reflectTermS(env, it.value) } }
        TermS.VCompoundType(elements)
    }

    is TermS.NodeType -> term
    is TermS.FunType -> {
        val dom = lazy { reflectTermS(env, term.dom) }
        val cod = Clos(env, lazyOf(term.cod))
        TermS.VFunType(term.name, dom, cod)
    }

    is TermS.CodeZType -> {
        val element = lazy { reflectTypeZ(env, term.element) }
        TermS.VCodeZType(element)
    }

    is TermS.CodeSType -> {
        val element = lazy { reflectTermS(env, term.element) }
        TermS.VCodeSType(element)
    }

    is TermS.TypeType -> term
    is TermS.EndTag -> term
    is TermS.ByteTag -> term
    is TermS.ShortTag -> term
    is TermS.IntTag -> term
    is TermS.LongTag -> term
    is TermS.FloatTag -> term
    is TermS.DoubleTag -> term
    is TermS.StringTag -> term
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

    is TermS.AllElementsNode -> term
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

    is TermS.QuoteTypeZ -> {
        val element = lazy { reflectTypeZ(env, term.element) }
        TermS.VQuoteTypeZ(element)
    }

    is TermS.QuoteTermZ -> term
    is TermS.QuoteTermS -> {
        val element = lazy { reflectTermS(env, term.element) }
        TermS.VQuoteTermS(element, term.type)
    }

    is TermS.Splice -> {
        val element = lazy { reflectTermS(env, term.element) }
        TermS.VSplice(element, term.type)
    }

    is TermS.Let -> {
        val init = lazy { reflectTermS(env, term.init) }
        reflectTermS(env + init, term.next)
    }

    is TermS.Var -> env[term.level].value
    is TermS.Meta -> term
    is TermS.Hole -> term
    else -> unreachable()
}

fun Unifier.reifyTermS(
    env: Environment,
    term: TermS<Sem>,
): TermS<Syn> = when (@Suppress("NAME_SHADOWING") val term = force(term)) {
    is TermS.UniverseType -> term
    is TermS.EndType -> term
    is TermS.ByteType -> term
    is TermS.ShortType -> term
    is TermS.IntType -> term
    is TermS.LongType -> term
    is TermS.FloatType -> term
    is TermS.DoubleType -> term
    is TermS.StringType -> term
    is TermS.ByteArrayType -> term
    is TermS.IntArrayType -> term
    is TermS.LongArrayType -> term
    is TermS.VListType -> {
        val element = reifyTermS(env, term.element.value)
        TermS.ListType(element)
    }

    is TermS.VCompoundType -> {
        val elements = term.elements.mapValues { reifyTermS(env, it.value.value) }
        TermS.CompoundType(elements)
    }

    is TermS.NodeType -> term
    is TermS.VFunType -> {
        val dom = reifyTermS(env, term.dom.value)
        val x = lazyOf(TermS.Var(term.name, env.size, term.dom.value))
        val cod = reifyTermS(env + x, term.cod(this, x))
        TermS.FunType(term.name, dom, cod)
    }

    is TermS.VCodeZType -> {
        val element = reifyTypeZ(env, term.element.value)
        TermS.CodeZType(element)
    }

    is TermS.VCodeSType -> {
        val element = reifyTermS(env, term.element.value)
        TermS.CodeSType(element)
    }

    is TermS.TypeType -> term
    is TermS.EndTag -> term
    is TermS.ByteTag -> term
    is TermS.ShortTag -> term
    is TermS.IntTag -> term
    is TermS.LongTag -> term
    is TermS.FloatTag -> term
    is TermS.DoubleTag -> term
    is TermS.StringTag -> term
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

    is TermS.VMatchObjectNode -> {
        val pattern = reifyTermS(env, term.pattern.value)
        TermS.MatchObjectNode(pattern)
    }

    is TermS.VMatchElementNode -> {
        val pattern = reifyTermS(env, term.pattern.value)
        TermS.MatchElementNode(pattern)
    }

    is TermS.AllElementsNode -> term
    is TermS.VIndexedElementNode -> {
        val pattern = reifyTermS(env, term.index.value)
        TermS.IndexedElementNode(pattern)
    }

    is TermS.VCompoundChildNode -> {
        val pattern = reifyTermS(env, term.name.value)
        TermS.CompoundChildNode(pattern)
    }

    is TermS.VGet -> {
        val target = reifyTermS(env, term.target.value)
        val path = reifyTermS(env, term.path.value)
        TermS.Get(target, path, term.type)
    }

    is TermS.VAbs -> {
        val anno = reifyTermS(env, term.anno.value)
        val x = lazyOf(TermS.Var(term.name, env.size, term.anno.value))
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

    is TermS.VQuoteTypeZ -> {
        val element = reifyTypeZ(env, term.element.value)
        TermS.QuoteTypeZ(element)
    }

    is TermS.QuoteTermZ -> term
    is TermS.VQuoteTermS -> {
        val element = reifyTermS(env, term.element.value)
        TermS.QuoteTermS(element, term.type)
    }

    is TermS.VSplice -> {
        val element = reifyTermS(env, term.element.value)
        TermS.Splice(element, term.type)
    }

    is TermS.Var -> term
    is TermS.Meta -> term
    is TermS.Hole -> term
    else -> unreachable()
}

operator fun Clos.invoke(
    unifier: Unifier,
    argument: Lazy<TermS<Sem>>,
): TermS<Sem> = unifier.reflectTermS(env + argument, body.value)
