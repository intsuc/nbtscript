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
        is S.Term.ByteType -> C.TypeZ.ByteType
        is S.Term.ShortType -> C.TypeZ.ShortType
        is S.Term.IntType -> C.TypeZ.IntType
        is S.Term.LongType -> C.TypeZ.LongType
        is S.Term.FloatType -> C.TypeZ.FloatType
        is S.Term.DoubleType -> C.TypeZ.DoubleType
        is S.Term.StringType -> C.TypeZ.StringType
        is S.Term.CollectionType -> {
            val element = elabTypeZ(type.element)
            C.TypeZ.CollectionType(element)
        }

        is S.Term.ByteArrayType -> C.TypeZ.ByteArrayType
        is S.Term.IntArrayType -> C.TypeZ.IntArrayType
        is S.Term.LongArrayType -> C.TypeZ.LongArrayType
        is S.Term.ListType -> {
            val element = elabTypeZ(type.element)
            C.TypeZ.ListType(element)
        }

        is S.Term.CompoundType -> {
            val elements = type.elements.map {
                val value = elabTypeZ(it.value)
                context.setHover(it.key.range, lazy {
                    Hover(markup(stringifyTypeZ(value)))
                })
                it.key.text to value
            }.toMap()
            C.TypeZ.CompoundType(elements)
        }

        is S.Term.Hole -> C.TypeZ.Hole
        else -> {
            context.addDiagnostic(typeZExpected(type.range))
            C.TypeZ.Hole
        }
    }.also {
        context.setHover(type.range, lazy {
            Hover(markup("universe"))
        })
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
        term is S.Term.CollectionType -> errorZ(termZExpected(term.range))
        term is S.Term.ByteArrayType -> errorZ(termZExpected(term.range))
        term is S.Term.IntArrayType -> errorZ(termZExpected(term.range))
        term is S.Term.LongArrayType -> errorZ(termZExpected(term.range))
        term is S.Term.ListType -> errorZ(termZExpected(term.range))
        term is S.Term.CompoundType -> errorZ(termZExpected(term.range))
        term is S.Term.FunctionType -> errorZ(termZExpected(term.range))
        term is S.Term.CodeType -> errorZ(termZExpected(term.range))
        term is S.Term.TypeType -> errorZ(termZExpected(term.range))
        term is S.Term.ByteTag && type is C.TypeZ.ByteType? -> C.TermZ.ByteTag(term.data, C.TypeZ.ByteType)
        term is S.Term.ShortTag && type is C.TypeZ.ShortType? -> C.TermZ.ShortTag(term.data, C.TypeZ.ShortType)
        term is S.Term.IntTag && type is C.TypeZ.IntType? -> C.TermZ.IntTag(term.data, C.TypeZ.IntType)
        term is S.Term.LongTag && type is C.TypeZ.LongType? -> C.TermZ.LongTag(term.data, C.TypeZ.LongType)
        term is S.Term.FloatTag && type is C.TypeZ.FloatType? -> C.TermZ.FloatTag(term.data, C.TypeZ.FloatType)
        term is S.Term.DoubleTag && type is C.TypeZ.DoubleType? -> C.TermZ.DoubleTag(term.data, C.TypeZ.DoubleType)
        term is S.Term.StringTag && type is C.TypeZ.StringType? -> C.TermZ.StringTag(term.data, C.TypeZ.StringType)
        term is S.Term.ByteArrayTag && type is C.TypeZ.ByteArrayType? -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.ByteType) }
            C.TermZ.ByteArrayTag(elements, C.TypeZ.ByteArrayType)
        }

        term is S.Term.IntArrayTag && type is C.TypeZ.IntArrayType? -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.IntType) }
            C.TermZ.IntArrayTag(elements, C.TypeZ.IntArrayType)
        }

        term is S.Term.LongArrayTag && type is C.TypeZ.LongArrayType? -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.LongType) }
            C.TermZ.LongArrayTag(elements, C.TypeZ.LongArrayType)
        }

        term is S.Term.ListTag && type is C.TypeZ.ListType? -> {
            if (term.elements.isEmpty()) C.TermZ.ListTag(emptyList(), C.TypeZ.ListType(C.TypeZ.EndType))
            else {
                val elements = mutableListOf<C.TermZ>()
                val head = elabTermZ(ctx, term.elements.first(), type?.element)
                elements += head
                term.elements.subList(1, term.elements.size).mapTo(elements) { elabTermZ(ctx, it, head.type) }
                C.TermZ.ListTag(elements, C.TypeZ.ListType(head.type))
            }
        }

        term is S.Term.CompoundTag && type is C.TypeZ.CompoundType? -> {
            val elements = term.elements.map {
                val value = elabTermZ(ctx, it.value, type?.elements?.get(it.key.text))
                context.setHover(it.key.range, lazy {
                    Hover(markup(stringifyTypeZ(value.type)))
                })
                it.key.text to value
            }.toMap()
            val elementTypes = elements.mapValues { it.value.type }
            C.TermZ.CompoundTag(elements, C.TypeZ.CompoundType(elementTypes))
        }

        term is S.Term.IndexedElement -> errorZ(termZExpected(term.range))
        term is S.Term.Abs -> errorZ(termZExpected(term.range))
        term is S.Term.Apply -> errorZ(termZExpected(term.range))
        term is S.Term.Quote -> errorZ(termZExpected(term.range))
        term is S.Term.Splice -> {
            val element = elabTermS(Context(), term.element, type?.let { TypeS.CodeType(it) })
            when (val elementType = context.unifier.force(element.type)) {
                is TypeS.CodeType -> C.TermZ.Splice(element, elementType.element)
                else -> errorZ(codeExpected(context.unifier, context.unifier.reify(persistentListOf(), elementType), term.range))
            }
        }

        term is S.Term.Let -> errorZ(termZExpected(term.range))
        term is S.Term.Function -> {
            val anno = term.anno?.let { elabTypeZ(it) }
            val body = elabTermZ(ctx, term.body, anno)
            context.setHover(term.name.range, lazy {
                Hover(markup(stringifyTypeZ(anno ?: body.type)))
            })
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
            C.TermZ.Hole(C.TypeZ.EndType)
        }

        else -> {
            if (type == null) error("failed: inference")
            else {
                val inferred = elabTermZ(ctx, term)
                if (context.unifier.subTypeZ(inferred.type, type)) inferred
                else errorZ(typeZMismatched(type, inferred.type, term.range))
            }
        }
    }.also {
        context.setHover(term.range, lazy {
            Hover(markup(stringifyTypeZ(it.type)))
        })
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
        @Suppress("NAME_SHADOWING") val type = type?.let { context.unifier.force(it) }
        return when {
            term is S.Term.UniverseType && type is TypeS.UniverseType? -> C.TermS.UniverseType(TypeS.UniverseType)
            term is S.Term.EndType && type is TypeS.UniverseType? -> C.TermS.EndType(TypeS.UniverseType)
            term is S.Term.ByteType && type is TypeS.UniverseType? -> C.TermS.ByteType(TypeS.UniverseType)
            term is S.Term.ShortType && type is TypeS.UniverseType? -> C.TermS.ShortType(TypeS.UniverseType)
            term is S.Term.IntType && type is TypeS.UniverseType? -> C.TermS.IntType(TypeS.UniverseType)
            term is S.Term.LongType && type is TypeS.UniverseType? -> C.TermS.LongType(TypeS.UniverseType)
            term is S.Term.FloatType && type is TypeS.UniverseType? -> C.TermS.FloatType(TypeS.UniverseType)
            term is S.Term.DoubleType && type is TypeS.UniverseType? -> C.TermS.DoubleType(TypeS.UniverseType)
            term is S.Term.StringType && type is TypeS.UniverseType? -> C.TermS.StringType(TypeS.UniverseType)
            term is S.Term.CollectionType -> errorS(termSExpected(term.range))
            term is S.Term.ByteArrayType && type is TypeS.UniverseType? -> C.TermS.ByteArrayType(TypeS.UniverseType)
            term is S.Term.IntArrayType && type is TypeS.UniverseType? -> C.TermS.IntArrayType(TypeS.UniverseType)
            term is S.Term.LongArrayType && type is TypeS.UniverseType? -> C.TermS.LongArrayType(TypeS.UniverseType)
            term is S.Term.ListType && type is TypeS.UniverseType? -> {
                val element = elabTermS(ctx, term.element, TypeS.UniverseType)
                C.TermS.ListType(element, TypeS.UniverseType)
            }

            term is S.Term.CompoundType && type is TypeS.UniverseType? -> {
                val elements = term.elements.map {
                    val value = elabTermS(ctx, it.value, TypeS.UniverseType)
                    context.setHover(it.key.range, lazy {
                        Hover(markup(context.unifier.stringifyTermS(value)))
                    })
                    it.key.text to value
                }.toMap()
                C.TermS.CompoundType(elements, TypeS.UniverseType)
            }

            term is S.Term.FunctionType && type is TypeS.UniverseType? -> {
                val dom = elabTermS(ctx, term.dom, TypeS.UniverseType)
                if (term.name != null) {
                    context.setHover(term.name.range, lazy {
                        Hover(markup(context.unifier.stringifyTermS(dom)))
                    })
                }
                val cod = elabTermS(ctx.bind(term.name?.text, dom.type), term.cod, TypeS.UniverseType)
                C.TermS.FunctionType(term.name?.text, dom, cod, TypeS.UniverseType)
            }

            term is S.Term.CodeType && type is TypeS.UniverseType? -> {
                val element = elabTypeZ(term.element)
                C.TermS.CodeType(element, TypeS.UniverseType)
            }

            term is S.Term.TypeType && type is TypeS.UniverseType? -> C.TermS.TypeType(TypeS.UniverseType)
            term is S.Term.ByteTag && type is TypeS.ByteType? -> C.TermS.ByteTag(term.data, TypeS.ByteType)
            term is S.Term.ShortTag && type is TypeS.ShortType? -> C.TermS.ShortTag(term.data, TypeS.ShortType)
            term is S.Term.IntTag && type is TypeS.IntType? -> C.TermS.IntTag(term.data, TypeS.IntType)
            term is S.Term.LongTag && type is TypeS.LongType? -> C.TermS.LongTag(term.data, TypeS.LongType)
            term is S.Term.FloatTag && type is TypeS.FloatType? -> C.TermS.FloatTag(term.data, TypeS.FloatType)
            term is S.Term.DoubleTag && type is TypeS.DoubleType? -> C.TermS.DoubleTag(term.data, TypeS.DoubleType)
            term is S.Term.StringTag && type is TypeS.StringType? -> C.TermS.StringTag(term.data, TypeS.StringType)
            term is S.Term.ByteArrayTag && type is TypeS.ByteArrayType? -> {
                val elements = term.elements.map { elabTermS(ctx, it, TypeS.ByteType) }
                C.TermS.ByteArrayTag(elements, TypeS.ByteArrayType)
            }

            term is S.Term.IntArrayTag && type is TypeS.IntArrayType? -> {
                val elements = term.elements.map { elabTermS(ctx, it, TypeS.IntType) }
                C.TermS.IntArrayTag(elements, TypeS.IntArrayType)
            }

            term is S.Term.LongArrayTag && type is TypeS.LongArrayType? -> {
                val elements = term.elements.map { elabTermS(ctx, it, TypeS.LongType) }
                C.TermS.LongArrayTag(elements, TypeS.LongArrayType)
            }

            term is S.Term.ListTag && type is TypeS.ListType? -> {
                if (term.elements.isEmpty()) {
                    C.TermS.ListTag(emptyList(), TypeS.ListType(lazyOf(TypeS.EndType)))
                } else {
                    val elements = mutableListOf<C.TermS>()
                    val head = elabTermS(ctx, term.elements.first(), type?.element?.value)
                    elements += head
                    term.elements.subList(1, term.elements.size).mapTo(elements) { elabTermS(ctx, it, head.type) }
                    C.TermS.ListTag(elements, TypeS.ListType(lazyOf(head.type)))
                }
            }

            term is S.Term.CompoundTag && type is TypeS.CompoundType? -> {
                val elements = term.elements.map {
                    val value = elabTermS(ctx, it.value, type?.elements?.get(it.key.text)?.value)
                    context.setHover(it.key.range, lazy {
                        Hover(markup(context.unifier.stringifyTermS(context.unifier.reify(ctx.values, value.type))))
                    })
                    it.key.text to value
                }.toMap()
                val elementTypes = elements.mapValues { lazyOf(it.value.type) }
                C.TermS.CompoundTag(elements, TypeS.CompoundType(elementTypes))
            }

            term is S.Term.IndexedElement -> {
                val target = elabTermZ(persistentMapOf(), term.target, C.TypeZ.ByteArrayType /* TODO */)
                val index = elabTermS(ctx, term.index, TypeS.IntType)
                C.TermS.IndexedElement(target, index, TypeS.CodeType(C.TypeZ.ByteType /* TODO */))
            }

            term is S.Term.Abs && type == null -> {
                val anno = term.anno?.let { elabTermS(ctx, it, TypeS.UniverseType) } ?: context.unifier.fresh(TypeS.UniverseType)
                context.setHover(term.name.range, lazy {
                    Hover(markup(context.unifier.stringifyTermS(anno)))
                })
                if (term.anno == null) {
                    context.addInlayHint(lazy {
                        val part = InlayHintLabelPart(": ${context.unifier.stringifyTermS(anno)}")
                        InlayHint(term.name.range.end, forRight(listOf(part)))
                    })
                }
                val a = context.unifier.reflect(ctx.values, anno)
                val body = elabTermS(ctx.bind(term.name.text, a), term.body)
                C.TermS.Abs(
                    term.name.text, anno, body, TypeS.FunctionType(
                        null,
                        lazyOf(a),
                        C.Clos(ctx.values, lazy { context.unifier.reify(ctx.values, body.type) })
                    )
                )
            }

            term is S.Term.Apply -> {
                if (type == null) {
                    val operator = elabTermS(ctx, term.operator)
                    when (val operatorType = context.unifier.force(operator.type)) {
                        is TypeS.FunctionType -> {
                            val operand = elabTermS(ctx, term.operand, operatorType.dom.value)
                            val cod = operatorType.cod(context.unifier, lazy { context.unifier.reflect(ctx.values, operand) })
                            C.TermS.Apply(operator, operand, cod)
                        }

                        else -> errorS(arrowExpected(context.unifier, context.unifier.reify(ctx.values, operatorType), term.operator.range))
                    }
                } else {
                    val operand = elabTermS(ctx, term.operand)
                    val operator = elabTermS(
                        ctx, term.operator, TypeS.FunctionType(
                            null,
                            lazyOf(operand.type),
                            C.Clos(ctx.values, lazy { context.unifier.reify(ctx.values, type) })
                        )
                    )
                    C.TermS.Apply(operator, operand, type)
                }
            }

            term is S.Term.Quote && type is TypeS.CodeType? -> {
                val element = elabTermZ(persistentMapOf(), term.element, type?.element)
                C.TermS.Quote(element, TypeS.CodeType(element.type))
            }

            term is S.Term.Let -> {
                val anno = term.anno?.let { elabTermS(ctx, it, TypeS.UniverseType) }
                val a = anno?.let { context.unifier.reflect(ctx.values, it) }
                val init = elabTermS(ctx, term.init, a)
                context.setHover(term.name.range, lazy {
                    Hover(markup(context.unifier.stringifyTermS(anno ?: context.unifier.reify(ctx.values, init.type))))
                })
                if (term.anno == null) {
                    context.addInlayHint(lazy {
                        val part = InlayHintLabelPart(": ${context.unifier.stringifyTermS(context.unifier.reify(ctx.values, init.type))}")
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
                        tooltip = forRight(markup(type?.let { context.unifier.stringifyTermS(context.unifier.reify(ctx.values, it)) } ?: "?"))
                    }
                    InlayHint(term.range.end, forRight(listOf(part)))
                })
                C.TermS.Hole(TypeS.EndType)
            }

            else -> {
                if (type == null) error("failed: inference")
                else {
                    val inferred = elabTermS(ctx, term)
                    if (context.unifier.unifyValue(ctx.size, inferred.type, type)) inferred
                    else errorS(
                        typeSMismatched(
                            context.unifier,
                            context.unifier.reify(ctx.values, type),
                            context.unifier.reify(ctx.values, inferred.type),
                            term.range
                        )
                    )
                }
            }
        }.also {
            context.setHover(term.range, lazy {
                Hover(markup(context.unifier.stringifyTermS(context.unifier.reify(ctx.values, it.type))))
            })
            context.setCompletionItems(term.range, lazy {
                ctx.levels.map { (name, level) ->
                    CompletionItem(name).apply {
                        kind = CompletionItemKind.Variable
                        labelDetails = CompletionItemLabelDetails().apply {
                            detail = " : ${context.unifier.stringifyTermS(context.unifier.reify(ctx.values, ctx.types[level]))}"
                        }
                        detail = context.unifier.stringifyTermS(context.unifier.reify(ctx.values, ctx.values[level].value))
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
        return C.TermZ.Hole(C.TypeZ.EndType)
    }

    private fun errorS(
        diagnostic: Diagnostic,
    ): C.TermS {
        context.addDiagnostic(diagnostic)
        return C.TermS.Hole(TypeS.EndType)
    }

    companion object : Phase<S.Root, C.Root> {
        override operator fun invoke(
            context: Phase.Context,
            input: S.Root,
        ): C.Root = Elab(context).elabRoot(input)
    }
}
