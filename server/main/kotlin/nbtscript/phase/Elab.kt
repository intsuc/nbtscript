package nbtscript.phase

import kotlinx.collections.immutable.*
import nbtscript.ast.Core.Kind.*
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either.forRight
import nbtscript.ast.Core as C
import nbtscript.ast.Surface as S

// TODO: create report messages lazily
@Suppress("NOTHING_TO_INLINE")
class Elab private constructor(
    private val context: Phase.Context = Phase.Context(),
) {
    private fun elabRoot(
        root: S.Root,
    ): C.Root {
        val body = elabTermZ(Context(), root.body)
        return C.Root(body)
    }

    private fun elabTypeZ(
        ctx: Context,
        type: S.Term,
    ): C.TypeZ<Syn> = when (type) {
        !is S.TypeZ -> errorTypeZ(typeZExpected(type.range))
        is S.Term.EndType -> elabTypeZEndType()
        is S.Term.ByteType -> elabTypeZByteType()
        is S.Term.ShortType -> elabTypeZShortType()
        is S.Term.IntType -> elabTypeZIntType()
        is S.Term.LongType -> elabTypeZLongType()
        is S.Term.FloatType -> elabTypeZFloatType()
        is S.Term.DoubleType -> elabTypeZDoubleType()
        is S.Term.StringType -> elabTypeZStringType()
        is S.Term.CollectionType -> elabTypeZCollectionType(ctx, type)
        is S.Term.ByteArrayType -> elabTypeZByteArrayType()
        is S.Term.IntArrayType -> elabTypeZIntArrayType()
        is S.Term.LongArrayType -> elabTypeZLongArrayType()
        is S.Term.ListType -> elabTypeZListType(ctx, type)
        is S.Term.CompoundType -> elabTypeZCompoundType(ctx, type)
        is S.Term.Splice -> elabTypeZSplice(ctx, type)
        is S.Term.Unlift -> TODO()
    }.also {
        context.setHover(type.range, lazy {
            Hover(markup("type₀"))
        })
    }

    private inline fun elabTypeZEndType(): C.TypeZ<Syn> = C.TypeZ.EndType
    private inline fun elabTypeZByteType(): C.TypeZ<Syn> = C.TypeZ.ByteType
    private inline fun elabTypeZShortType(): C.TypeZ<Syn> = C.TypeZ.ShortType
    private inline fun elabTypeZIntType(): C.TypeZ<Syn> = C.TypeZ.IntType
    private inline fun elabTypeZLongType(): C.TypeZ<Syn> = C.TypeZ.LongType
    private inline fun elabTypeZFloatType(): C.TypeZ<Syn> = C.TypeZ.FloatType
    private inline fun elabTypeZDoubleType(): C.TypeZ<Syn> = C.TypeZ.DoubleType
    private inline fun elabTypeZStringType(): C.TypeZ<Syn> = C.TypeZ.StringType
    private inline fun elabTypeZCollectionType(ctx: Context, type: S.Term.CollectionType): C.TypeZ<Syn> {
        val element = elabTypeZ(ctx, type.element)
        return C.TypeZ.CollectionType(element)
    }

    private inline fun elabTypeZByteArrayType(): C.TypeZ<Syn> = C.TypeZ.ByteArrayType
    private inline fun elabTypeZIntArrayType(): C.TypeZ<Syn> = C.TypeZ.IntArrayType
    private inline fun elabTypeZLongArrayType(): C.TypeZ<Syn> = C.TypeZ.LongArrayType
    private inline fun elabTypeZListType(ctx: Context, type: S.Term.ListType): C.TypeZ<Syn> {
        val element = elabTypeZ(ctx, type.element)
        return C.TypeZ.ListType(element)
    }

    private inline fun elabTypeZCompoundType(ctx: Context, type: S.Term.CompoundType): C.TypeZ<Syn> {
        val elements = type.elements.map {
            val value = elabTypeZ(ctx, it.value)
            context.setHover(it.key.range, lazy {
                Hover(markup(context.unifier.stringifyTypeZ(value)))
            })
            it.key.text to value
        }.toMap()
        return C.TypeZ.CompoundType(elements)
    }

    private inline fun elabTypeZSplice(ctx: Context, type: S.Term.Splice): C.TypeZ<Syn> {
        val element = elabTermS(ctx, type.element, C.TermS.TypeType)
        return C.TypeZ.Splice(element)
    }

    private fun elabTermZ(
        ctx: Context,
        term: S.Term,
        type: C.TypeZ<Sem>? = null,
    ): C.TermZ = when {
        term !is S.TermZ -> errorTermZ(termZExpected(term.range))
        term is S.Term.ByteTag && type is C.TypeZ.ByteType? -> elabTermZByteTag(term)
        term is S.Term.ShortTag && type is C.TypeZ.ShortType? -> elabTermZShortTag(term)
        term is S.Term.IntTag && type is C.TypeZ.IntType? -> elabTermZIntTag(term)
        term is S.Term.LongTag && type is C.TypeZ.LongType? -> elabTermZLongTag(term)
        term is S.Term.FloatTag && type is C.TypeZ.FloatType? -> elabTermZFloatTag(term)
        term is S.Term.DoubleTag && type is C.TypeZ.DoubleType? -> elabTermZDoubleTag(term)
        term is S.Term.StringTag && type is C.TypeZ.StringType? -> elabTermZStringTag(term)
        term is S.Term.ByteArrayTag && type is C.TypeZ.ByteArrayType? -> elabTermZByteArrayTag(ctx, term)
        term is S.Term.IntArrayTag && type is C.TypeZ.IntArrayType? -> elabTermZIntArrayTag(ctx, term)
        term is S.Term.LongArrayTag && type is C.TypeZ.LongArrayType? -> elabTermZLongArrayTag(ctx, term)
        term is S.Term.ListTag && type is C.TypeZ.ListType? -> elabTermZListTag(ctx, term, type)
        term is S.Term.CompoundTag && type is C.TypeZ.CompoundType? -> elabTermZCompoundTag(ctx, term, type)
        term is S.Term.Splice -> elabTermZSplice(ctx, term, type)
        term is S.Term.Unlift -> TODO()
        term is S.Term.Fun -> elabTermZFun(ctx, term, type)
        term is S.Term.Var && type == null -> synthTermZVar(ctx, term)
        type == null -> error("failed: synthesis")
        else -> elabTermZSub(ctx, term, type)
    }.also {
        context.setHover(term.range, lazy {
            Hover(markup(context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(ctx.values, it.type))))
        })
        context.setCompletionItems(term.range, lazy {
            ctx.typesZ.entries.map { (name, type) ->
                CompletionItem(name).apply {
                    kind = CompletionItemKind.Function
                    labelDetails = CompletionItemLabelDetails().apply {
                        detail = " : ${context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(ctx.values, type))}"
                    }
                }
            }
        })
    }

    private inline fun elabTermZByteTag(term: S.Term.ByteTag): C.TermZ = C.TermZ.ByteTag(term.data)
    private inline fun elabTermZShortTag(term: S.Term.ShortTag): C.TermZ = C.TermZ.ShortTag(term.data)
    private inline fun elabTermZIntTag(term: S.Term.IntTag): C.TermZ = C.TermZ.IntTag(term.data)
    private inline fun elabTermZLongTag(term: S.Term.LongTag): C.TermZ = C.TermZ.LongTag(term.data)
    private inline fun elabTermZFloatTag(term: S.Term.FloatTag): C.TermZ = C.TermZ.FloatTag(term.data)
    private inline fun elabTermZDoubleTag(term: S.Term.DoubleTag): C.TermZ = C.TermZ.DoubleTag(term.data)
    private inline fun elabTermZStringTag(term: S.Term.StringTag): C.TermZ = C.TermZ.StringTag(term.data)
    private inline fun elabTermZByteArrayTag(ctx: Context, term: S.Term.ByteArrayTag): C.TermZ {
        val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.ByteType) }
        return C.TermZ.ByteArrayTag(elements)
    }

    private inline fun elabTermZIntArrayTag(ctx: Context, term: S.Term.IntArrayTag): C.TermZ {
        val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.IntType) }
        return C.TermZ.IntArrayTag(elements)
    }

    private inline fun elabTermZLongArrayTag(ctx: Context, term: S.Term.LongArrayTag): C.TermZ {
        val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.LongType) }
        return C.TermZ.LongArrayTag(elements)
    }

    private inline fun elabTermZListTag(ctx: Context, term: S.Term.ListTag, type: C.TypeZ.ListType<Sem>? = null): C.TermZ {
        return if (term.elements.isEmpty()) {
            C.TermZ.ListTag(emptyList(), C.TypeZ.ListType(C.TypeZ.EndType))
        } else {
            val elements = mutableListOf<C.TermZ>()
            val head = elabTermZ(ctx, term.elements.first(), type?.element)
            elements += head
            term.elements.subList(1, term.elements.size).mapTo(elements) { elabTermZ(ctx, it, type?.element ?: head.type) }
            C.TermZ.ListTag(elements, type ?: C.TypeZ.ListType(head.type))
        }
    }

    private inline fun elabTermZCompoundTag(ctx: Context, term: S.Term.CompoundTag, type: C.TypeZ.CompoundType<Sem>? = null): C.TermZ {
        val elements = term.elements.map {
            val value = elabTermZ(ctx, it.value, type?.elements?.get(it.key.text))
            context.setHover(it.key.range, lazy {
                Hover(markup(context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(ctx.values, value.type))))
            })
            it.key.text to value
        }.toMap()
        return C.TermZ.CompoundTag(elements, type ?: C.TypeZ.CompoundType(elements.mapValues { it.value.type }))
    }

    private inline fun elabTermZSplice(ctx: Context, term: S.Term.Splice, type: C.TypeZ<Sem>? = null): C.TermZ {
        val element = elabTermS(ctx, term.element, type?.let { C.TermS.VCodeZType(lazyOf(it)) })
        return when (val elementType = context.unifier.force(element.type)) {
            is C.TermS.VCodeZType -> C.TermZ.Splice(element, elementType.element.value)
            else -> errorTermZ(codeTypeExpected(context.unifier, context.unifier.reifyTermS(ctx.values, elementType), term.range))
        }
    }

    private inline fun elabTermZFun(ctx: Context, term: S.Term.Fun, type: C.TypeZ<Sem>? = null): C.TermZ {
        val anno = term.anno?.let { context.unifier.reflectTypeZ(ctx.values, elabTypeZ(ctx, it)) }
        val body = elabTermZ(ctx, term.body, anno)
        context.setHover(term.name.range, lazy {
            Hover(markup(context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(ctx.values, anno ?: body.type))))
        })
        if (term.anno == null) {
            context.addInlayHint(lazy {
                val part = InlayHintLabelPart(": ${context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(ctx.values, body.type))}")
                InlayHint(term.name.range.end, forRight(listOf(part)))
            })
        }
        val next = elabTermZ(ctx.bindZ(term.name.text, anno ?: body.type), term.next, type)
        return C.TermZ.Fun(term.name.text, body, next, next.type)
    }

    private inline fun synthTermZVar(ctx: Context, term: S.Term.Var): C.TermZ {
        return if (ctx.typesZ.contains(term.name)) {
            C.TermZ.Run(term.name, ctx.typesZ[term.name]!!)
        } else {
            errorTermZ(notFound(term.name, term.range))
        }
    }

    private inline fun elabTermZSub(ctx: Context, term: S.Term, type: C.TypeZ<Sem>): C.TermZ {
        val synth = elabTermZ(ctx, term)
        return if (context.unifier.subTypeZ(synth.type, type)) {
            synth
        } else {
            errorTermZ(typeZMismatched(ctx.values, context.unifier, type, synth.type, term.range))
        }
    }

    private fun elabTermS(
        ctx: Context,
        term: S.Term,
        type: C.TermS<Sem>? = null,
    ): C.TermS<Syn> {
        @Suppress("NAME_SHADOWING") val type = type?.let { context.unifier.force(it) }
        return when {
            term !is S.TermS -> errorTermS(termSExpected(term.range))
            term is S.Term.UniverseType && type is C.TermS.UniverseType? -> elabTermSUniverseType()
            term is S.Term.EndType && type is C.TermS.UniverseType? -> elabTermSEndType()
            term is S.Term.ByteType && type is C.TermS.UniverseType? -> elabTermSByteType()
            term is S.Term.ShortType && type is C.TermS.UniverseType? -> elabTermSShortType()
            term is S.Term.IntType && type is C.TermS.UniverseType? -> elabTermSIntType()
            term is S.Term.LongType && type is C.TermS.UniverseType? -> elabTermSLongType()
            term is S.Term.FloatType && type is C.TermS.UniverseType? -> elabTermSFloatType()
            term is S.Term.DoubleType && type is C.TermS.UniverseType? -> elabTermSDoubleType()
            term is S.Term.StringType && type is C.TermS.UniverseType? -> elabTermSStringType()
            term is S.Term.ByteArrayType && type is C.TermS.UniverseType? -> elabTermSByteArrayType()
            term is S.Term.IntArrayType && type is C.TermS.UniverseType? -> elabTermSIntArrayType()
            term is S.Term.LongArrayType && type is C.TermS.UniverseType? -> elabTermSLongArrayType()
            term is S.Term.ListType && type is C.TermS.UniverseType? -> elabTermSListType(ctx, term)
            term is S.Term.CompoundType && type is C.TermS.UniverseType? -> elabTermSCompoundType(ctx, term)
            term is S.Term.NodeType && type is C.TermS.UniverseType? -> elabTermSNodeType()
            term is S.Term.FunType && type is C.TermS.UniverseType? -> elabTermSFunType(ctx, term)
            term is S.Term.CodeZType && type is C.TermS.UniverseType? -> elabTermSCodeTypeZ(ctx, term)
            term is S.Term.CodeSType && type is C.TermS.UniverseType? -> elabTermSCodeTypeS(ctx, term)
            term is S.Term.TypeType && type is C.TermS.UniverseType? -> elabTermSTypeType()
            term is S.Term.ByteTag && type is C.TermS.ByteType? -> elabTermSByteTag(term)
            term is S.Term.ShortTag && type is C.TermS.ShortType? -> elabTermSShortTag(term)
            term is S.Term.IntTag && type is C.TermS.IntType? -> elabTermSIntTag(term)
            term is S.Term.LongTag && type is C.TermS.LongType? -> elabTermSLongTag(term)
            term is S.Term.FloatTag && type is C.TermS.FloatType? -> elabTermSFloatTag(term)
            term is S.Term.DoubleTag && type is C.TermS.DoubleType? -> elabTermSDoubleTag(term)
            term is S.Term.StringTag && type is C.TermS.StringType? -> elabTermSStringTag(term)
            term is S.Term.ByteArrayTag && type is C.TermS.ByteArrayType? -> elabTermSByteArrayTag(ctx, term)
            term is S.Term.IntArrayTag && type is C.TermS.IntArrayType? -> elabTermSIntArrayTag(ctx, term)
            term is S.Term.LongArrayTag && type is C.TermS.LongArrayType? -> elabTermSLongArrayTag(ctx, term)
            term is S.Term.ListTag && type is C.TermS.VListType? -> elabTermSListTag(ctx, term, type)
            term is S.Term.CompoundTag && type is C.TermS.VCompoundType? -> elabTermSCompoundTag(ctx, term, type)
            type is C.TermS.NodeType -> checkTermSNode(ctx, term)
            term is S.Term.Get -> elabTermSGet(ctx, term)
            term is S.Term.Abs && type == null -> synthTermSAbs(ctx, term)
            term is S.Term.Abs && term.anno == null && type is C.TermS.VFunType -> checkTermSAbs(ctx, term, type)
            term is S.Term.Apply && type != null -> checkTermSApply(ctx, term, type)
            term is S.Term.Apply -> synthTermSApply(ctx, term)
            term is S.Term.Quote && type is C.TermS.TypeType -> checkTermSQuoteTypeType(ctx, term)
            term is S.Term.Quote && type is C.TermS.VCodeZType -> checkTermSQuoteVCodeTypeZ(ctx, term, type)
            term is S.Term.Quote && type is C.TermS.VCodeSType -> checkTermSQuoteVCodeTypeS(ctx, term, type)
            term is S.Term.Quote && type == null -> synthTermSQuoteS(ctx, term) // TODO: disambiguate?
            term is S.Term.Splice -> synthTermSSplice(ctx, term, type)
            term is S.Term.Unlift -> TODO()
            term is S.Term.Let -> elabTermSLet(ctx, term, type)
            term is S.Term.Var && type == null -> synthTermSVar(ctx, term)
            type == null -> error("failed: synthesis")
            else -> elabTermSSub(ctx, term, type)
        }.also {
            context.setHover(term.range, lazy {
                Hover(markup(context.unifier.stringifyTermS(context.unifier.reifyTermS(ctx.values, it.type))))
            })
            context.setCompletionItems(term.range, lazy {
                ctx.levels.map { (name, level) ->
                    CompletionItem(name).apply {
                        kind = CompletionItemKind.Variable
                        labelDetails = CompletionItemLabelDetails().apply {
                            detail = " : ${context.unifier.stringifyTermS(context.unifier.reifyTermS(ctx.values, ctx.typesS[level]))}"
                        }
                        detail = context.unifier.stringifyTermS(context.unifier.reifyTermS(ctx.values, ctx.values[level].value))
                    }
                }
            })
        }
    }

    private inline fun elabTermSUniverseType(): C.TermS<Syn> = C.TermS.UniverseType
    private inline fun elabTermSEndType(): C.TermS<Syn> = C.TermS.EndType
    private inline fun elabTermSByteType(): C.TermS<Syn> = C.TermS.ByteType
    private inline fun elabTermSShortType(): C.TermS<Syn> = C.TermS.ShortType
    private inline fun elabTermSIntType(): C.TermS<Syn> = C.TermS.IntType
    private inline fun elabTermSLongType(): C.TermS<Syn> = C.TermS.LongType
    private inline fun elabTermSFloatType(): C.TermS<Syn> = C.TermS.FloatType
    private inline fun elabTermSDoubleType(): C.TermS<Syn> = C.TermS.DoubleType
    private inline fun elabTermSStringType(): C.TermS<Syn> = C.TermS.StringType
    private inline fun elabTermSByteArrayType(): C.TermS<Syn> = C.TermS.ByteArrayType
    private inline fun elabTermSIntArrayType(): C.TermS<Syn> = C.TermS.IntArrayType
    private inline fun elabTermSLongArrayType(): C.TermS<Syn> = C.TermS.LongArrayType
    private inline fun elabTermSListType(ctx: Context, term: S.Term.ListType): C.TermS<Syn> {
        val element = elabTermS(ctx, term.element, C.TermS.UniverseType)
        return C.TermS.ListType(element)
    }

    private inline fun elabTermSCompoundType(ctx: Context, term: S.Term.CompoundType): C.TermS<Syn> {
        val elements = term.elements.map {
            val value = elabTermS(ctx, it.value, C.TermS.UniverseType)
            context.setHover(it.key.range, lazy {
                Hover(markup(context.unifier.stringifyTermS(value)))
            })
            it.key.text to value
        }.toMap()
        return C.TermS.CompoundType(elements)
    }

    private inline fun elabTermSNodeType(): C.TermS<Syn> = C.TermS.NodeType
    private inline fun elabTermSFunType(ctx: Context, term: S.Term.FunType): C.TermS<Syn> {
        val dom = elabTermS(ctx, term.dom, C.TermS.UniverseType)
        if (term.name != null) {
            context.setHover(term.name.range, lazy {
                Hover(markup(context.unifier.stringifyTermS(dom)))
            })
        }
        val cod = elabTermS(ctx.bindS(term.name?.text, context.unifier.reflectTermS(ctx.values, dom)), term.cod, C.TermS.UniverseType)
        return C.TermS.FunType(term.name?.text, dom, cod)
    }

    private inline fun elabTermSCodeTypeZ(ctx: Context, term: S.Term.CodeZType): C.TermS<Syn> {
        val element = elabTypeZ(ctx, term.element)
        return C.TermS.CodeZType(element)
    }

    private inline fun elabTermSCodeTypeS(ctx: Context, term: S.Term.CodeSType): C.TermS<Syn> {
        val element = elabTermS(ctx.down(), term.element)
        return C.TermS.CodeSType(element)
    }

    private inline fun elabTermSTypeType(): C.TermS<Syn> = C.TermS.TypeType
    private inline fun elabTermSByteTag(term: S.Term.ByteTag): C.TermS<Syn> = C.TermS.ByteTag(term.data)
    private inline fun elabTermSShortTag(term: S.Term.ShortTag): C.TermS<Syn> = C.TermS.ShortTag(term.data)
    private inline fun elabTermSIntTag(term: S.Term.IntTag): C.TermS<Syn> = C.TermS.IntTag(term.data)
    private inline fun elabTermSLongTag(term: S.Term.LongTag): C.TermS<Syn> = C.TermS.LongTag(term.data)
    private inline fun elabTermSFloatTag(term: S.Term.FloatTag): C.TermS<Syn> = C.TermS.FloatTag(term.data)
    private inline fun elabTermSDoubleTag(term: S.Term.DoubleTag): C.TermS<Syn> = C.TermS.DoubleTag(term.data)
    private inline fun elabTermSStringTag(term: S.Term.StringTag): C.TermS<Syn> = C.TermS.StringTag(term.data)
    private inline fun elabTermSByteArrayTag(ctx: Context, term: S.Term.ByteArrayTag): C.TermS<Syn> {
        val elements = term.elements.map { elabTermS(ctx, it, C.TermS.ByteType) }
        return C.TermS.ByteArrayTag(elements)
    }

    private inline fun elabTermSIntArrayTag(ctx: Context, term: S.Term.IntArrayTag): C.TermS<Syn> {
        val elements = term.elements.map { elabTermS(ctx, it, C.TermS.IntType) }
        return C.TermS.IntArrayTag(elements)
    }

    private inline fun elabTermSLongArrayTag(ctx: Context, term: S.Term.LongArrayTag): C.TermS<Syn> {
        val elements = term.elements.map { elabTermS(ctx, it, C.TermS.LongType) }
        return C.TermS.LongArrayTag(elements)
    }

    private inline fun elabTermSListTag(ctx: Context, term: S.Term.ListTag, type: C.TermS.VListType?): C.TermS<Syn> {
        return if (term.elements.isEmpty()) {
            C.TermS.ListTag(emptyList(), C.TermS.VListType(lazyOf(C.TermS.EndType)))
        } else {
            val elements = mutableListOf<C.TermS<Syn>>()
            val head = elabTermS(ctx, term.elements.first(), type?.element?.value)
            elements += head
            term.elements.subList(1, term.elements.size).mapTo(elements) { elabTermS(ctx, it, head.type) }
            C.TermS.ListTag(elements, type ?: C.TermS.VListType(lazyOf(head.type)))
        }
    }

    private inline fun elabTermSCompoundTag(ctx: Context, term: S.Term.CompoundTag, type: C.TermS.VCompoundType?): C.TermS<Syn> {
        val elements = term.elements.map {
            val value = elabTermS(ctx, it.value, type?.elements?.get(it.key.text)?.value)
            context.setHover(it.key.range, lazy {
                Hover(markup(context.unifier.stringifyTermS(context.unifier.reifyTermS(ctx.values, value.type))))
            })
            it.key.text to value
        }.toMap()
        return C.TermS.CompoundTag(elements, type ?: C.TermS.VCompoundType(elements.mapValues { lazyOf(it.value.type) }))
    }

    private inline fun checkTermSNode(ctx: Context, term: S.Term): C.TermS<Syn> {
        return when (term) {
            is S.Term.StringTag -> {
                val name = elabTermS(ctx, term)
                C.TermS.CompoundChildNode(name)
            }

            is S.Term.ListTag -> {
                when (val size = term.elements.size) {
                    0 -> C.TermS.AllElementsNode
                    1 -> {
                        val pattern = elabTermS(ctx, term.elements.first())
                        when (context.unifier.force(pattern.type)) {
                            is C.TermS.VCompoundType -> C.TermS.MatchElementNode(pattern)
                            is C.TermS.IntType -> C.TermS.IndexedElementNode(pattern)
                            else -> errorTermS(invalidNode(term.range))
                        }
                    }

                    else -> errorTermS(sizeMismatched(1, size, term.range))
                }
            }

            is S.Term.CompoundTag -> {
                val pattern = elabTermS(ctx, term)
                C.TermS.MatchObjectNode(pattern)
            }

            else -> errorTermS(invalidNode(term.range))
        }
    }

    private inline fun elabTermSGet(ctx: Context, term: S.Term.Get): C.TermS<Syn> {
        val target = elabTermS(ctx, term.target)
        val path = elabTermS(ctx, term.path, C.TermS.VListType(lazyOf(C.TermS.NodeType)))
        return C.TermS.Get(target, path, C.TermS.VCodeZType(lazyOf(C.TypeZ.EndType) /* TODO */))
    }

    private inline fun synthTermSAbs(ctx: Context, term: S.Term.Abs): C.TermS<Syn> {
        val anno = term.anno?.let { elabTermS(ctx, it, C.TermS.UniverseType) } ?: context.unifier.fresh()
        context.setHover(term.name.range, lazy {
            Hover(markup(context.unifier.stringifyTermS(anno)))
        })
        if (term.anno == null) {
            context.addInlayHint(lazy {
                val part = InlayHintLabelPart(": ${context.unifier.stringifyTermS(anno)}")
                InlayHint(term.name.range.end, forRight(listOf(part)))
            })
        }
        val a = context.unifier.reflectTermS(ctx.values, anno)
        val body = elabTermS(ctx.bindS(term.name.text, a), term.body)
        return C.TermS.Abs(
            term.name.text, anno, body, C.TermS.VFunType(
                term.name.text,
                lazyOf(a),
                C.Clos(ctx.values, lazy { context.unifier.reifyTermS(ctx.values, body.type) }),
            )
        )
    }

    private inline fun checkTermSAbs(ctx: Context, term: S.Term.Abs, type: C.TermS.VFunType): C.TermS<Syn> {
        val dom = context.unifier.reifyTermS(ctx.values, type.dom.value)
        context.setHover(term.name.range, lazy {
            Hover(markup(context.unifier.stringifyTermS(dom)))
        })
        context.addInlayHint(lazy {
            val part = InlayHintLabelPart(": ${context.unifier.stringifyTermS(dom)}")
            InlayHint(term.name.range.end, forRight(listOf(part)))
        })
        val cod = type.cod(context.unifier, lazyOf(C.TermS.Var(term.name.text, ctx.size, type.dom.value)))
        val body = elabTermS(ctx.bindS(term.name.text, type.dom.value), term.body, cod)
        return C.TermS.Abs(term.name.text, dom, body, type)
    }

    private inline fun checkTermSApply(ctx: Context, term: S.Term.Apply, type: C.TermS<Sem>): C.TermS<Syn> {
        val operand = elabTermS(ctx, term.operand)
        val operator = elabTermS(
            ctx, term.operator, C.TermS.VFunType(
                null,
                lazyOf(operand.type),
                C.Clos(ctx.values, lazy { context.unifier.reifyTermS(ctx.values, type) }),
            )
        )
        return C.TermS.Apply(operator, operand, type)
    }

    private inline fun synthTermSApply(ctx: Context, term: S.Term.Apply): C.TermS<Syn> {
        val operator = elabTermS(ctx, term.operator)
        return when (val operatorType = context.unifier.force(operator.type)) {
            is C.TermS.VFunType -> {
                val operand = elabTermS(ctx, term.operand, operatorType.dom.value)
                val cod = operatorType.cod(context.unifier, lazy { context.unifier.reflectTermS(ctx.values, operand) })
                C.TermS.Apply(operator, operand, cod)
            }

            else -> {
                val dom = context.unifier.reflectTermS(ctx.values, context.unifier.fresh())
                val cod = context.unifier.fresh()
                val operatorType2 = C.TermS.VFunType(null, lazyOf(dom), C.Clos(ctx.values, lazyOf(cod)))
                tryUnify(ctx, operatorType, operatorType2, term.operator.range) {
                    val operand = elabTermS(ctx, term.operand, dom)
                    C.TermS.Apply(operator, operand, context.unifier.reflectTermS(ctx.values, cod))
                }
            }
        }
    }

    private inline fun checkTermSQuoteTypeType(ctx: Context, term: S.Term.Quote): C.TermS<Syn> {
        val element = elabTypeZ(ctx, term.element)
        return C.TermS.QuoteTypeZ(element)
    }

    private inline fun checkTermSQuoteVCodeTypeZ(ctx: Context, term: S.Term.Quote, type: C.TermS.VCodeZType): C.TermS<Syn> {
        val element = elabTermZ(ctx, term.element, type.element.value)
        return C.TermS.QuoteTermZ(element, type)
    }

    private inline fun checkTermSQuoteVCodeTypeS(ctx: Context, term: S.Term.Quote, type: C.TermS.VCodeSType): C.TermS<Syn> {
        val element = elabTermS(ctx.down(), term.element, type.element.value)
        return C.TermS.QuoteTermS(element, type)
    }

    private inline fun synthTermSQuoteZ(ctx: Context, term: S.Term.Quote): C.TermS<Syn> {
        return when (term.element) {
            is S.TypeZ -> {
                val element = elabTypeZ(ctx, term.element)
                C.TermS.QuoteTypeZ(element)
            }

            is S.TermZ -> {
                val element = elabTermZ(ctx, term.element)
                C.TermS.QuoteTermZ(element, C.TermS.VCodeZType(lazyOf(element.type)))
            }

            else -> errorTermS(objZExpected(term.element.range))
        }
    }

    private inline fun synthTermSQuoteS(ctx: Context, term: S.Term.Quote): C.TermS<Syn> {
        val element = elabTermS(ctx.down(), term.element)
        return C.TermS.QuoteTermS(element, C.TermS.VCodeSType(lazyOf(element.type)))
    }

    private inline fun synthTermSSplice(ctx: Context, term: S.Term.Splice, type: C.TermS<Sem>?): C.TermS<Syn> {
        val element = elabTermS(ctx.up(), term.element, type?.let { C.TermS.VCodeSType(lazyOf(it)) })
        return when (val elementType = context.unifier.force(element.type)) {
            is C.TermS.VCodeSType -> C.TermS.Splice(element, elementType.element.value)
            else -> errorTermS(codeTypeExpected(context.unifier, context.unifier.reifyTermS(ctx.values, elementType), term.range))
        }
    }

    private inline fun elabTermSLet(ctx: Context, term: S.Term.Let, type: C.TermS<Sem>?): C.TermS<Syn> {
        val anno = term.anno?.let { elabTermS(ctx, it, C.TermS.UniverseType) }
        val a = anno?.let { context.unifier.reflectTermS(ctx.values, it) }
        val init = elabTermS(ctx, term.body, a)
        context.setHover(term.name.range, lazy {
            Hover(markup(context.unifier.stringifyTermS(anno ?: context.unifier.reifyTermS(ctx.values, init.type))))
        })
        if (term.anno == null) {
            context.addInlayHint(lazy {
                val part = InlayHintLabelPart(": ${context.unifier.stringifyTermS(context.unifier.reifyTermS(ctx.values, init.type))}")
                InlayHint(term.name.range.end, forRight(listOf(part)))
            })
        }
        val next = elabTermS(ctx.bindS(term.name.text, a ?: init.type, lazy { context.unifier.reflectTermS(ctx.values, init) }), term.next, type)
        return C.TermS.Let(term.name.text, init, next, type ?: next.type)
    }

    private inline fun synthTermSVar(ctx: Context, term: S.Term.Var): C.TermS<Syn> {
        return when (val level = ctx.levels[term.name]) {
            null -> errorTermS(notFound(term.name, term.range))
            else -> C.TermS.Var(term.name, level, ctx.typesS[level])
        }
    }

    private inline fun elabTermSSub(ctx: Context, term: S.Term, type: C.TermS<Sem>): C.TermS<Syn> {
        val synth = elabTermS(ctx, term)
        return tryUnify(ctx, synth.type, type, term.range) { synth }
    }

    private inline fun tryUnify(
        ctx: Context,
        term1: C.TermS<Sem>,
        term2: C.TermS<Sem>,
        range: Range,
        block: () -> C.TermS<Syn>,
    ): C.TermS<Syn> {
        return if (context.unifier.unifyTermS(ctx.size, term1, term2)) {
            block()
        } else {
            errorTermS(typeSMismatched(context.unifier, context.unifier.reifyTermS(ctx.values, term1), context.unifier.reifyTermS(ctx.values, term2), range))
        }
    }

    private class Context private constructor(
        val typesZ: PersistentMap<String, C.TypeZ<Sem>>,
        val levels: PersistentMap<String, Int>,
        val typesS: PersistentList<C.TermS<Sem>>,
        val values: PersistentList<Lazy<C.TermS<Sem>>>,
        val lvl: Int,
    ) {
        val size: Int get() = typesS.size

        fun bindZ(
            name: String,
            type: C.TypeZ<Sem>,
        ): Context = Context(
            typesZ = typesZ + (name to type),
            levels = levels,
            typesS = typesS,
            values = values,
            lvl = lvl,
        )

        fun bindS(
            name: String?,
            type: C.TermS<Sem>,
            term: Lazy<C.TermS<Sem>>? = null,
        ): Context = Context(
            typesZ = typesZ,
            levels = name?.let { levels + (name to size) } ?: levels,
            typesS = typesS + type,
            values = values + (term ?: lazyOf(C.TermS.Var(name, size, type))),
            lvl = lvl,
        )

        fun up(): Context = Context(typesZ, levels, typesS, values, lvl.inc())
        fun down(): Context = Context(typesZ, levels, typesS, values, lvl.dec())

        companion object {
            operator fun invoke(): Context = Context(
                persistentMapOf(),
                persistentMapOf(),
                persistentListOf(),
                persistentListOf(),
                0,
            )
        }
    }

    private fun errorTypeZ(
        diagnostic: Diagnostic,
    ): C.TypeZ<Syn> {
        context.addDiagnostic(diagnostic)
        return C.TypeZ.Hole
    }

    private fun errorTermZ(
        diagnostic: Diagnostic,
        type: C.TypeZ<Sem>? = null,
    ): C.TermZ {
        context.addDiagnostic(diagnostic)
        return type?.let { C.TermZ.Hole(type) } ?: HoleZ
    }

    private fun errorTermS(
        diagnostic: Diagnostic,
        type: C.TermS<Sem>? = null,
    ): C.TermS<Syn> {
        context.addDiagnostic(diagnostic)
        return type?.let { C.TermS.Hole(it) } ?: HoleS
    }

    companion object : Phase<S.Root, C.Root> {
        private val HoleZ: C.TermZ = C.TermZ.Hole(C.TypeZ.EndType)
        private val HoleS: C.TermS<End> = C.TermS.Hole(C.TermS.EndType)

        override operator fun invoke(
            context: Phase.Context,
            input: S.Root,
        ): C.Root = Elab(context).elabRoot(input)
    }
}
