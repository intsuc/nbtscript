package nbts.phase

import kotlinx.collections.immutable.persistentListOf
import nbts.ast.Core.*
import nbts.ast.Core.Kind.Syn
import nbts.unreachable

class Zonk private constructor(
    private val context: Phase.Context = Phase.Context(),
) {
    private fun zonkRoot(
        root: Root,
    ): Root {
        val body = zonkTermZ(root.body)
        return Root(body)
    }

    private fun zonkTypeZ(
        type: TypeZ<Syn>,
    ): TypeZ<Syn> = type

    private fun zonkTermZ(
        term: TermZ,
    ): TermZ = when (term) {
        is TermZ.ByteTag -> term
        is TermZ.ShortTag -> term
        is TermZ.IntTag -> term
        is TermZ.LongTag -> term
        is TermZ.FloatTag -> term
        is TermZ.DoubleTag -> term
        is TermZ.StringTag -> term
        is TermZ.ByteArrayTag -> {
            val elements = term.elements.map { zonkTermZ(it) }
            TermZ.ByteArrayTag(elements)
        }

        is TermZ.IntArrayTag -> {
            val elements = term.elements.map { zonkTermZ(it) }
            TermZ.IntArrayTag(elements)
        }

        is TermZ.LongArrayTag -> {
            val elements = term.elements.map { zonkTermZ(it) }
            TermZ.LongArrayTag(elements)
        }

        is TermZ.ListTag -> {
            val elements = term.elements.map { zonkTermZ(it) }
            TermZ.ListTag(elements, term.type)
        }

        is TermZ.CompoundTag -> {
            val elements = term.elements.mapValues { zonkTermZ(it.value) }
            TermZ.CompoundTag(elements, term.type)
        }

        is TermZ.Fun -> {
            val body = zonkTermZ(term.body)
            val next = zonkTermZ(term.next)
            TermZ.Fun(term.name, body, next, term.type)
        }

        is TermZ.Run -> term
        is TermZ.Splice -> {
            val element = zonkTermS(term.element)
            TermZ.Splice(element, term.type)
        }

        is TermZ.Hole -> term
    }

    // TODO: force types?
    private fun zonkTermS(
        term: TermS<Syn>,
    ): TermS<Syn> = when (term) {
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
            val element = zonkTermS(term.element)
            TermS.ListType(element)
        }

        is TermS.CompoundType -> {
            val elements = term.elements.mapValues { zonkTermS(it.value) }
            TermS.CompoundType(elements)
        }

        is TermS.NodeType -> term
        is TermS.FunType -> {
            val dom = zonkTermS(term.dom)
            val cod = zonkTermS(term.cod)
            TermS.FunType(term.name, dom, cod)
        }

        is TermS.CodeZType -> {
            val element = zonkTypeZ(term.element)
            TermS.CodeZType(element)
        }

        is TermS.CodeSType -> {
            val element = zonkTermS(term.element)
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
        is TermS.ByteArrayTag -> {
            val elements = term.elements.map { zonkTermS(it) }
            TermS.ByteArrayTag(elements)
        }

        is TermS.IntArrayTag -> {
            val elements = term.elements.map { zonkTermS(it) }
            TermS.IntArrayTag(elements)
        }

        is TermS.LongArrayTag -> {
            val elements = term.elements.map { zonkTermS(it) }
            TermS.LongArrayTag(elements)
        }

        is TermS.ListTag -> {
            val elements = term.elements.map { zonkTermS(it) }
            TermS.ListTag(elements, term.type)
        }

        is TermS.CompoundTag -> {
            val elements = term.elements.mapValues { zonkTermS(it.value) }
            TermS.CompoundTag(elements, term.type)
        }

        is TermS.MatchObjectNode -> {
            val pattern = zonkTermS(term.pattern)
            TermS.MatchObjectNode(pattern)
        }

        is TermS.MatchElementNode -> {
            val pattern = zonkTermS(term.pattern)
            TermS.MatchElementNode(pattern)
        }

        is TermS.AllElementsNode -> term
        is TermS.IndexedElementNode -> {
            val index = zonkTermS(term.index)
            TermS.IndexedElementNode(index)
        }

        is TermS.CompoundChildNode -> {
            val name = zonkTermS(term.name)
            TermS.CompoundChildNode(name)
        }

        is TermS.Get -> {
            val target = zonkTermS(term.target)
            val path = zonkTermS(term.path)
            TermS.Get(target, path, term.type)
        }

        is TermS.Abs -> {
            val anno = zonkTermS(term.anno)
            val body = zonkTermS(term.body)
            TermS.Abs(term.name, anno, body, term.type)
        }

        is TermS.Apply -> {
            val operator = zonkTermS(term.operator)
            val operand = zonkTermS(term.operand)
            TermS.Apply(operator, operand, term.type)
        }

        is TermS.QuoteTypeZ -> {
            val element = zonkTypeZ(term.element)
            TermS.QuoteTypeZ(element)
        }

        is TermS.QuoteTermZ -> {
            val element = zonkTermZ(term.element)
            TermS.QuoteTermZ(element, term.type)
        }

        is TermS.QuoteTermS -> {
            val element = zonkTermS(term.element)
            TermS.QuoteTermS(element, term.type)
        }

        is TermS.Splice -> {
            val element = zonkTermS(term.element)
            TermS.Splice(element, term.type)
        }

        is TermS.Let -> {
            val init = zonkTermS(term.init)
            val next = zonkTermS(term.next)
            TermS.Let(term.name, init, next, term.type)
        }

        is TermS.Var -> term
        is TermS.Meta -> {
            context.unifier[term.index]
                ?.let { context.unifier.reifyTermS(persistentListOf(), it) }
                ?: run {
                    context.addDiagnostic(unsolvedMeta(term.index))
                    term
                }
        }

        is TermS.Hole -> term
        else -> unreachable()
    }

    companion object : Phase<Root, Root> {
        override operator fun invoke(
            context: Phase.Context,
            input: Root,
        ): Root = Zonk(context).zonkRoot(input)
    }
}
