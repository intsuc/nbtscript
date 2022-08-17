package nbtscript.phase

import kotlinx.collections.immutable.*
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either.forRight
import nbtscript.ast.Core as C
import nbtscript.ast.Core.Value as TypeS
import nbtscript.ast.Surface as S

// TODO: create report messages lazily
class Elab private constructor(
    private val context: Phase.Context = Phase.Context(),
) {
    private fun elabRoot(
        root: S.Root,
    ): C.Root {
        val body = elabTermZ(persistentMapOf(), root.body)
        return C.Root(body)
    }

    private fun elabTypeZ(
        type: S.Term,
    ): C.TypeZ = when (type) {
        is S.Term.ByteType -> C.TypeZ.ByteZ
        is S.Term.ShortType -> C.TypeZ.ShortZ
        is S.Term.IntType -> C.TypeZ.IntZ
        is S.Term.LongType -> C.TypeZ.LongZ
        is S.Term.FloatType -> C.TypeZ.FloatZ
        is S.Term.DoubleType -> C.TypeZ.DoubleZ
        is S.Term.StringType -> C.TypeZ.StringZ
        is S.Term.ByteArrayType -> C.TypeZ.ByteArrayZ
        is S.Term.IntArrayType -> C.TypeZ.IntArrayZ
        is S.Term.LongArrayType -> C.TypeZ.LongArrayZ
        is S.Term.ListType -> {
            val element = elabTypeZ(type.element)
            C.TypeZ.ListZ(element)
        }

        is S.Term.CompoundType -> {
            val elements = type.elements.map { it.key.text to elabTypeZ(it.value) }.toMap()
            C.TypeZ.CompoundZ(elements)
        }

        is S.Term.Hole -> C.TypeZ.Hole
        else -> {
            context.addDiagnostic(typeZExpected(type.range))
            C.TypeZ.Hole
        }
    }.also {
        context.setHover(type.range, lazy { Hover(markup("universe")) })
    }

    private fun elabTermZ(
        ctx: PersistentMap<String, C.TypeZ>,
        term: S.Term,
        type: C.TypeZ? = null,
    ): C.TermZ = when {
        term is S.Term.UniverseType -> errorZ(termZExpected(term.range))
        term is S.Term.EndType -> errorZ(termZExpected(term.range))
        term is S.Term.ByteType -> errorZ(termZExpected(term.range))
        term is S.Term.ShortType -> errorZ(termZExpected(term.range))
        term is S.Term.IntType -> errorZ(termZExpected(term.range))
        term is S.Term.LongType -> errorZ(termZExpected(term.range))
        term is S.Term.FloatType -> errorZ(termZExpected(term.range))
        term is S.Term.DoubleType -> errorZ(termZExpected(term.range))
        term is S.Term.StringType -> errorZ(termZExpected(term.range))
        term is S.Term.ByteArrayType -> errorZ(termZExpected(term.range))
        term is S.Term.IntArrayType -> errorZ(termZExpected(term.range))
        term is S.Term.LongArrayType -> errorZ(termZExpected(term.range))
        term is S.Term.ListType -> errorZ(termZExpected(term.range))
        term is S.Term.CompoundType -> errorZ(termZExpected(term.range))
        term is S.Term.FunctionType -> errorZ(termZExpected(term.range))
        term is S.Term.CodeType -> errorZ(termZExpected(term.range))
        term is S.Term.TypeType -> errorZ(termZExpected(term.range))
        term is S.Term.ByteTag && type is C.TypeZ.ByteZ? -> C.TermZ.ByteTag(term.data, C.TypeZ.ByteZ)
        term is S.Term.ShortTag && type is C.TypeZ.ShortZ? -> C.TermZ.ShortTag(term.data, C.TypeZ.ShortZ)
        term is S.Term.IntTag && type is C.TypeZ.IntZ? -> C.TermZ.IntTag(term.data, C.TypeZ.IntZ)
        term is S.Term.LongTag && type is C.TypeZ.LongZ? -> C.TermZ.LongTag(term.data, C.TypeZ.LongZ)
        term is S.Term.FloatTag && type is C.TypeZ.FloatZ? -> C.TermZ.FloatTag(term.data, C.TypeZ.FloatZ)
        term is S.Term.DoubleTag && type is C.TypeZ.DoubleZ? -> C.TermZ.DoubleTag(term.data, C.TypeZ.DoubleZ)
        term is S.Term.StringTag && type is C.TypeZ.StringZ? -> C.TermZ.StringTag(term.data, C.TypeZ.StringZ)
        term is S.Term.ByteArrayTag && type is C.TypeZ.ByteArrayZ? -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.ByteZ) }
            C.TermZ.ByteArrayTag(elements, C.TypeZ.ByteArrayZ)
        }

        term is S.Term.IntArrayTag && type is C.TypeZ.IntArrayZ? -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.IntZ) }
            C.TermZ.IntArrayTag(elements, C.TypeZ.IntArrayZ)
        }

        term is S.Term.LongArrayTag && type is C.TypeZ.LongArrayZ? -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.LongZ) }
            C.TermZ.LongArrayTag(elements, C.TypeZ.LongArrayZ)
        }

        term is S.Term.ListTag && type is C.TypeZ.ListZ? -> {
            if (term.elements.isEmpty()) C.TermZ.ListTag(emptyList(), C.TypeZ.ListZ(C.TypeZ.EndZ))
            else {
                val elements = mutableListOf<C.TermZ>()
                val head = elabTermZ(ctx, term.elements.first(), type?.element)
                elements += head
                term.elements.subList(1, term.elements.size).mapTo(elements) { elabTermZ(ctx, it, head.type) }
                C.TermZ.ListTag(elements, C.TypeZ.ListZ(head.type))
            }
        }

        term is S.Term.CompoundTag && type is C.TypeZ.CompoundZ? -> {
            val elements = term.elements.map { it.key.text to elabTermZ(ctx, it.value, type?.elements?.get(it.key.text)) }.toMap()
            val elementTypes = elements.mapValues { it.value.type }
            C.TermZ.CompoundTag(elements, C.TypeZ.CompoundZ(elementTypes))
        }

        term is S.Term.IndexedElement -> errorZ(termZExpected(term.range))
        term is S.Term.Abs -> errorZ(termZExpected(term.range))
        term is S.Term.Apply -> errorZ(termZExpected(term.range))
        term is S.Term.Quote -> errorZ(termZExpected(term.range))
        term is S.Term.Splice -> {
            val element = elabTermS(Context(), term.element, type?.let { TypeS.CodeS(it) })
            when (val elementType = context.unifier.forceS(element.type)) {
                is TypeS.CodeS -> C.TermZ.Splice(element, elementType.element)
                else -> errorZ(codeExpected(context.unifier.reify(persistentListOf(), elementType), term.range))
            }
        }

        term is S.Term.Let -> errorZ(termZExpected(term.range))
        term is S.Term.Function -> {
            val anno = term.anno?.let { elabTypeZ(it) }
            val body = elabTermZ(ctx, term.body, anno)
            context.setHover(term.name.range, lazy { Hover(markup(stringifyTypeZ(anno ?: body.type))) })
            if (term.anno == null) {
                context.addInlayHint(lazy {
                    val part = InlayHintLabelPart(": ${stringifyTypeZ(body.type)}")
                    InlayHint(term.name.range.end, forRight(listOf(part)))
                })
            }
            val next = elabTermZ(ctx + (term.name.text to (anno ?: body.type)), term.next, type)
            C.TermZ.Function(term.name.text, body, next, next.type)
        }

        term is S.Term.Var && type == null -> {
            if (ctx.contains(term.name)) C.TermZ.Run(term.name, ctx[term.name]!!)
            else errorZ(notFound(term.name, term.range))
        }

        term is S.Term.Hole -> {
            context.addInlayHint(lazy {
                val part = InlayHintLabelPart("_").apply {
                    tooltip = forRight(markup(type?.let { stringifyTypeZ(it) } ?: "?"))
                }
                InlayHint(term.range.end, forRight(listOf(part)))
            })
            C.TermZ.Hole(C.TypeZ.EndZ)
        }

        else -> {
            if (type == null) error("failed: inference")
            else {
                val inferred = elabTermZ(ctx, term)
                if (context.unifier.unifyZ(inferred.type, type)) inferred
                else errorZ(typeZMismatched(type, inferred.type, term.range))
            }
        }
    }.also {
        context.setHover(term.range, lazy { Hover(markup(stringifyTypeZ(it.type))) })
        context.setCompletionItems(term.range, lazy {
            ctx.entries.map { (name, type) ->
                CompletionItem(name).apply {
                    kind = CompletionItemKind.Function
                    labelDetails = CompletionItemLabelDetails().apply {
                        detail = " : ${stringifyTypeZ(type)}"
                    }
                }
            }
        })
    }

    private fun elabTermS(
        ctx: Context,
        term: S.Term,
        type: TypeS? = null,
    ): C.TermS {
        @Suppress("NAME_SHADOWING") val type = type?.let { context.unifier.forceS(it) }
        return when {
            term is S.Term.UniverseType && type is TypeS.UniverseS? -> C.TermS.UniverseS(TypeS.UniverseS)
            term is S.Term.EndType && type is TypeS.UniverseS? -> C.TermS.EndS(TypeS.UniverseS)
            term is S.Term.ByteType && type is TypeS.UniverseS? -> C.TermS.ByteS(TypeS.UniverseS)
            term is S.Term.ShortType && type is TypeS.UniverseS? -> C.TermS.ShortS(TypeS.UniverseS)
            term is S.Term.IntType && type is TypeS.UniverseS? -> C.TermS.IntS(TypeS.UniverseS)
            term is S.Term.LongType && type is TypeS.UniverseS? -> C.TermS.LongS(TypeS.UniverseS)
            term is S.Term.FloatType && type is TypeS.UniverseS? -> C.TermS.FloatS(TypeS.UniverseS)
            term is S.Term.DoubleType && type is TypeS.UniverseS? -> C.TermS.DoubleS(TypeS.UniverseS)
            term is S.Term.StringType && type is TypeS.UniverseS? -> C.TermS.StringS(TypeS.UniverseS)
            term is S.Term.ByteArrayType && type is TypeS.UniverseS? -> C.TermS.ByteArrayS(TypeS.UniverseS)
            term is S.Term.IntArrayType && type is TypeS.UniverseS? -> C.TermS.IntArrayS(TypeS.UniverseS)
            term is S.Term.LongArrayType && type is TypeS.UniverseS? -> C.TermS.LongArrayS(TypeS.UniverseS)
            term is S.Term.ListType && type is TypeS.UniverseS? -> {
                val element = elabTermS(ctx, term.element, TypeS.UniverseS)
                C.TermS.ListS(element, TypeS.UniverseS)
            }

            term is S.Term.CompoundType && type is TypeS.UniverseS? -> {
                val elements = term.elements.map { it.key.text to elabTermS(ctx, it.value, TypeS.UniverseS) }.toMap()
                C.TermS.CompoundS(elements, TypeS.UniverseS)
            }

            term is S.Term.FunctionType && type is TypeS.UniverseS? -> {
                val dom = elabTermS(ctx, term.dom, TypeS.UniverseS)
                if (term.name != null) {
                    context.setHover(term.name.range, lazy { Hover(markup(stringifyTermS(dom))) })
                }
                val cod = elabTermS(ctx.bind(term.name?.text, dom.type), term.cod, TypeS.UniverseS)
                C.TermS.FunctionS(term.name?.text, dom, cod, TypeS.UniverseS)
            }

            term is S.Term.CodeType && type is TypeS.UniverseS? -> {
                val element = elabTypeZ(term.element)
                C.TermS.CodeS(element, TypeS.UniverseS)
            }

            term is S.Term.TypeType && type is TypeS.UniverseS? -> C.TermS.TypeZ(TypeS.UniverseS)
            term is S.Term.ByteTag && type is TypeS.ByteS? -> C.TermS.ByteTag(term.data, TypeS.ByteS)
            term is S.Term.ShortTag && type is TypeS.ShortS? -> C.TermS.ShortTag(term.data, TypeS.ShortS)
            term is S.Term.IntTag && type is TypeS.IntS? -> C.TermS.IntTag(term.data, TypeS.IntS)
            term is S.Term.LongTag && type is TypeS.LongS? -> C.TermS.LongTag(term.data, TypeS.LongS)
            term is S.Term.FloatTag && type is TypeS.FloatS? -> C.TermS.FloatTag(term.data, TypeS.FloatS)
            term is S.Term.DoubleTag && type is TypeS.DoubleS? -> C.TermS.DoubleTag(term.data, TypeS.DoubleS)
            term is S.Term.StringTag && type is TypeS.StringS? -> C.TermS.StringTag(term.data, TypeS.StringS)
            term is S.Term.ByteArrayTag && type is TypeS.ByteArrayS? -> {
                val elements = term.elements.map { elabTermS(ctx, it, TypeS.ByteS) }
                C.TermS.ByteArrayTag(elements, TypeS.ByteArrayS)
            }

            term is S.Term.IntArrayTag && type is TypeS.IntArrayS? -> {
                val elements = term.elements.map { elabTermS(ctx, it, TypeS.IntS) }
                C.TermS.IntArrayTag(elements, TypeS.IntArrayS)
            }

            term is S.Term.LongArrayTag && type is TypeS.LongArrayS? -> {
                val elements = term.elements.map { elabTermS(ctx, it, TypeS.LongS) }
                C.TermS.LongArrayTag(elements, TypeS.LongArrayS)
            }

            term is S.Term.ListTag && type is TypeS.ListS? -> {
                if (term.elements.isEmpty()) {
                    C.TermS.ListTag(emptyList(), TypeS.ListS(lazyOf(TypeS.EndS)))
                } else {
                    val elements = mutableListOf<C.TermS>()
                    val head = elabTermS(ctx, term.elements.first(), type?.element?.value)
                    elements += head
                    term.elements.subList(1, term.elements.size).mapTo(elements) { elabTermS(ctx, it, head.type) }
                    C.TermS.ListTag(elements, TypeS.ListS(lazyOf(head.type)))
                }
            }

            term is S.Term.CompoundTag && type is TypeS.CompoundS? -> {
                val elements = term.elements.map { it.key.text to elabTermS(ctx, it.value, type?.elements?.get(it.key.text)?.value) }.toMap()
                val elementTypes = elements.mapValues { lazyOf(it.value.type) }
                C.TermS.CompoundTag(elements, TypeS.CompoundS(elementTypes))
            }

            term is S.Term.IndexedElement -> {
                val target = elabTermZ(persistentMapOf(), term.target, C.TypeZ.ByteArrayZ /* TODO */)
                val index = elabTermS(ctx, term.index, TypeS.IntS)
                C.TermS.IndexedElement(target, index, TypeS.CodeS(C.TypeZ.ByteZ /* TODO */))
            }

            term is S.Term.Abs && type == null -> {
                val anno = term.anno?.let { elabTermS(ctx, it, TypeS.UniverseS) } ?: context.unifier.fresh(TypeS.UniverseS)
                context.setHover(term.name.range, lazy { Hover(markup(stringifyTermS(anno))) })
                val a = context.unifier.reflect(ctx.values, anno)
                val body = elabTermS(ctx.bind(term.name.text, a), term.body)
                C.TermS.Abs(
                    term.name.text, anno, body, TypeS.FunctionS(
                        null,
                        lazyOf(a),
                        C.Clos(ctx.values, lazy { context.unifier.reify(ctx.values, body.type) })
                    )
                )
            }

            term is S.Term.Apply -> {
                if (type == null) {
                    val operator = elabTermS(ctx, term.operator)
                    when (val operatorType = context.unifier.forceS(operator.type)) {
                        is TypeS.FunctionS -> {
                            val operand = elabTermS(ctx, term.operand, operatorType.dom.value)
                            val cod = operatorType.cod(context.unifier, lazy { context.unifier.reflect(ctx.values, operand) })
                            C.TermS.Apply(operator, operand, cod)
                        }

                        else -> errorS(arrowExpected(context.unifier.reify(ctx.values, operatorType), term.operator.range))
                    }
                } else {
                    val operand = elabTermS(ctx, term.operand)
                    val operator = elabTermS(
                        ctx, term.operator, TypeS.FunctionS(
                            null,
                            lazyOf(operand.type),
                            C.Clos(ctx.values, lazy { context.unifier.reify(ctx.values, type) })
                        )
                    )
                    C.TermS.Apply(operator, operand, type)
                }
            }

            term is S.Term.Quote && type is TypeS.CodeS? -> {
                val element = elabTermZ(persistentMapOf(), term.element, type?.element)
                C.TermS.Quote(element, TypeS.CodeS(element.type))
            }

            term is S.Term.Let -> {
                val anno = term.anno?.let { elabTermS(ctx, it, TypeS.UniverseS) }
                val a = anno?.let { context.unifier.reflect(ctx.values, it) }
                val init = elabTermS(ctx, term.init, a)
                context.setHover(term.name.range, lazy { Hover(markup(stringifyTermS(anno ?: context.unifier.reify(ctx.values, init.type)))) })
                if (term.anno == null) {
                    context.addInlayHint(lazy {
                        val part = InlayHintLabelPart(": ${stringifyTermS(context.unifier.reify(ctx.values, init.type))}")
                        InlayHint(term.name.range.end, forRight(listOf(part)))
                    })
                }
                val next = elabTermS(ctx.bind(term.name.text, a ?: init.type, lazy { context.unifier.reflect(ctx.values, init) }), term.next, type)
                C.TermS.Let(term.name.text, init, next, type ?: next.type)
            }

            term is S.Term.Var && type == null -> {
                when (val level = ctx.levels[term.name]) {
                    null -> errorS(notFound(term.name, term.range))
                    else -> C.TermS.Var(term.name, level, ctx.types[level])
                }
            }

            term is S.Term.Hole -> {
                context.addInlayHint(lazy {
                    val part = InlayHintLabelPart("_").apply {
                        tooltip = forRight(markup(type?.let { stringifyTermS(context.unifier.reify(ctx.values, it)) } ?: "?"))
                    }
                    InlayHint(term.range.end, forRight(listOf(part)))
                })
                C.TermS.Hole(TypeS.EndS)
            }

            else -> {
                if (type == null) error("failed: inference")
                else {
                    val inferred = elabTermS(ctx, term)
                    if (context.unifier.unifyS(ctx.size, inferred.type, type)) inferred
                    else errorS(
                        typeSMismatched(
                            context.unifier.reify(ctx.values, type),
                            context.unifier.reify(ctx.values, inferred.type),
                            term.range
                        )
                    )
                }
            }
        }.also {
            context.setHover(term.range, lazy { Hover(markup(stringifyTermS(context.unifier.reify(ctx.values, it.type)))) })
            context.setCompletionItems(term.range, lazy {
                ctx.levels.map { (name, level) ->
                    CompletionItem(name).apply {
                        kind = CompletionItemKind.Variable
                        labelDetails = CompletionItemLabelDetails().apply {
                            detail = " : ${stringifyTermS(context.unifier.reify(ctx.values, ctx.types[level]))}"
                        }
                        detail = stringifyTermS(context.unifier.reify(ctx.values, ctx.values[level].value))
                    }
                }
            })
        }
    }

    private class Context private constructor(
        val levels: PersistentMap<String, Int>,
        val types: PersistentList<TypeS>,
        val values: PersistentList<Lazy<C.Value>>,
    ) {
        val size: Int get() = types.size

        fun bind(
            name: String?,
            type: TypeS,
            value: Lazy<C.Value>? = null,
        ): Context = Context(
            levels = name?.let { levels + (name to size) } ?: levels,
            types = types + type,
            values = values + (value ?: lazyOf(C.Value.Var(name, size, lazyOf(type)))),
        )

        companion object {
            operator fun invoke(): Context = Context(
                persistentMapOf(),
                persistentListOf(),
                persistentListOf(),
            )
        }
    }

    private fun errorZ(
        diagnostic: Diagnostic,
    ): C.TermZ {
        context.addDiagnostic(diagnostic)
        return C.TermZ.Hole(C.TypeZ.EndZ)
    }

    private fun errorS(
        diagnostic: Diagnostic,
    ): C.TermS {
        context.addDiagnostic(diagnostic)
        return C.TermS.Hole(TypeS.EndS)
    }

    companion object : Phase<S.Root, C.Root> {
        override operator fun invoke(
            context: Phase.Context,
            input: S.Root,
        ): C.Root = Elab(context).elabRoot(input)
    }
}
