package nbtscript.phase

import kotlinx.collections.immutable.*
import nbtscript.ast.Core.Kind.Sem
import nbtscript.ast.Core.Kind.Syn
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either.forRight
import nbtscript.ast.Core as C
import nbtscript.ast.Surface as S

// TODO: eliminate redundant type reconstruction
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

    private fun elabObjZ(
        ctx: PersistentMap<String, C.TypeZ<Sem>>,
        term: S.Term,
    ): C.ObjZ<Syn> = when (term) {
        is S.Term.UniverseType -> errorZ(objZExpected(term.range))
        is S.Term.EndType -> errorZ(objZExpected(term.range))
        is S.Term.ByteType -> C.TypeZ.ByteType.Syn
        is S.Term.ShortType -> C.TypeZ.ShortType.Syn
        is S.Term.IntType -> C.TypeZ.IntType.Syn
        is S.Term.LongType -> C.TypeZ.LongType.Syn
        is S.Term.FloatType -> C.TypeZ.FloatType.Syn
        is S.Term.DoubleType -> C.TypeZ.DoubleType.Syn
        is S.Term.StringType -> C.TypeZ.StringType.Syn
        is S.Term.CollectionType -> {
            val element = elabTypeZ(term.element)
            C.TypeZ.CollectionType(element)
        }

        is S.Term.ByteArrayType -> C.TypeZ.ByteArrayType.Syn
        is S.Term.IntArrayType -> C.TypeZ.IntArrayType.Syn
        is S.Term.LongArrayType -> C.TypeZ.LongArrayType.Syn
        is S.Term.ListType -> {
            val element = elabTypeZ(term.element)
            C.TypeZ.ListType(element)
        }

        is S.Term.CompoundType -> {
            val elements = term.elements.map {
                val value = elabTypeZ(it.value)
                context.setHover(it.key.range, lazy {
                    Hover(markup(context.unifier.stringifyTypeZ(value)))
                })
                it.key.text to value
            }.toMap()
            C.TypeZ.CompoundType(elements)
        }

        is S.Term.FunctionType -> errorZ(objZExpected(term.range))
        is S.Term.CodeType -> errorZ(objZExpected(term.range))
        is S.Term.TypeType -> errorZ(objZExpected(term.range))
        is S.Term.ByteTag -> C.TermZ.ByteTag(term.data, C.TypeZ.ByteType.Sem)
        is S.Term.ShortTag -> C.TermZ.ShortTag(term.data, C.TypeZ.ShortType.Sem)
        is S.Term.IntTag -> C.TermZ.IntTag(term.data, C.TypeZ.IntType.Sem)
        is S.Term.LongTag -> C.TermZ.LongTag(term.data, C.TypeZ.LongType.Sem)
        is S.Term.FloatTag -> C.TermZ.FloatTag(term.data, C.TypeZ.FloatType.Sem)
        is S.Term.DoubleTag -> C.TermZ.DoubleTag(term.data, C.TypeZ.DoubleType.Sem)
        is S.Term.StringTag -> C.TermZ.StringTag(term.data, C.TypeZ.StringType.Sem)
        is S.Term.ByteArrayTag -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.ByteType.Sem) }
            C.TermZ.ByteArrayTag(elements, C.TypeZ.ByteArrayType.Sem)
        }

        is S.Term.IntArrayTag -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.IntType.Sem) }
            C.TermZ.IntArrayTag(elements, C.TypeZ.IntArrayType.Sem)
        }

        is S.Term.LongArrayTag -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.LongType.Sem) }
            C.TermZ.LongArrayTag(elements, C.TypeZ.LongArrayType.Sem)
        }

        is S.Term.ListTag -> {
            if (term.elements.isEmpty()) C.TermZ.ListTag(emptyList(), C.TypeZ.ListType(C.TypeZ.EndType.Sem))
            else {
                val elements = mutableListOf<C.TermZ>()
                val head = elabTermZ(ctx, term.elements.first())
                elements += head
                term.elements.subList(1, term.elements.size).mapTo(elements) { elabTermZ(ctx, it, head.type) }
                C.TermZ.ListTag(elements, C.TypeZ.ListType(head.type))
            }
        }

        is S.Term.CompoundTag -> {
            val elements = term.elements.map {
                val value = elabTermZ(ctx, it.value)
                context.setHover(it.key.range, lazy {
                    Hover(markup(context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(value.type))))
                })
                it.key.text to value
            }.toMap()
            val elementTypes = elements.mapValues { it.value.type }
            C.TermZ.CompoundTag(elements, C.TypeZ.CompoundType(elementTypes))
        }

        is S.Term.IndexedElement -> errorZ(objZExpected(term.range))
        is S.Term.Abs -> errorZ(objZExpected(term.range))
        is S.Term.Apply -> errorZ(objZExpected(term.range))
        is S.Term.Quote -> errorZ(objZExpected(term.range))
        is S.Term.Splice -> {
            val element = elabTermS(Context(), term.element)
            when (val elementType = context.unifier.force(element.type)) {
                is C.TermS.VCodeType -> C.TermZ.Splice(element, elementType.element.value)
                else -> errorZ(codeTypeExpected(context.unifier, context.unifier.reifyTermS(persistentListOf(), elementType), term.range))
            }
        }

        is S.Term.Let -> errorZ(objZExpected(term.range))
        is S.Term.Function -> {
            val anno = term.anno?.let { context.unifier.reflectTypeZ(elabTypeZ(it)) }
            val body = elabTermZ(ctx, term.body, anno)
            context.setHover(term.name.range, lazy {
                Hover(markup(context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(anno ?: body.type))))
            })
            if (term.anno == null) {
                context.addInlayHint(lazy {
                    val part = InlayHintLabelPart(": ${context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(body.type))}")
                    InlayHint(term.name.range.end, forRight(listOf(part)))
                })
            }
            val next = elabTermZ(ctx + (term.name.text to (anno ?: body.type)), term.next)
            C.TermZ.Function(term.name.text, body, next, next.type)
        }

        is S.Term.Var -> {
            if (ctx.contains(term.name)) C.TermZ.Run(term.name, ctx[term.name]!!)
            else errorZ(notFound(term.name, term.range))
        }

        is S.Term.Hole -> errorZ(objZExpected(term.range))
    }.also {
        context.setHover(term.range, lazy {
            when (it) {
                is C.TypeZ -> Hover(markup("universe"))
                is C.TermZ -> Hover(markup(context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(it.type))))
            }
        })
        context.setCompletionItems(term.range, lazy {
            ctx.entries.map { (name, type) ->
                CompletionItem(name).apply {
                    kind = CompletionItemKind.Function
                    labelDetails = CompletionItemLabelDetails().apply {
                        detail = " : ${context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(type))}"
                    }
                }
            }
        })
    }

    private fun elabTypeZ(
        type: S.Term,
    ): C.TypeZ<Syn> = when (type) {
        is S.Term.ByteType -> C.TypeZ.ByteType.Syn
        is S.Term.ShortType -> C.TypeZ.ShortType.Syn
        is S.Term.IntType -> C.TypeZ.IntType.Syn
        is S.Term.LongType -> C.TypeZ.LongType.Syn
        is S.Term.FloatType -> C.TypeZ.FloatType.Syn
        is S.Term.DoubleType -> C.TypeZ.DoubleType.Syn
        is S.Term.StringType -> C.TypeZ.StringType.Syn
        is S.Term.CollectionType -> {
            val element = elabTypeZ(type.element)
            C.TypeZ.CollectionType(element)
        }

        is S.Term.ByteArrayType -> C.TypeZ.ByteArrayType.Syn
        is S.Term.IntArrayType -> C.TypeZ.IntArrayType.Syn
        is S.Term.LongArrayType -> C.TypeZ.LongArrayType.Syn
        is S.Term.ListType -> {
            val element = elabTypeZ(type.element)
            C.TypeZ.ListType(element)
        }

        is S.Term.CompoundType -> {
            val elements = type.elements.map {
                val value = elabTypeZ(it.value)
                context.setHover(it.key.range, lazy {
                    Hover(markup(context.unifier.stringifyTypeZ(value)))
                })
                it.key.text to value
            }.toMap()
            C.TypeZ.CompoundType(elements)
        }

        is S.Term.Splice -> {
            val element = elabTermS(Context(), type.element, C.TermS.TypeType.Sem)
            C.TypeZ.Splice(element)
        }

        else -> {
            context.addDiagnostic(typeZExpected(type.range))
            C.TypeZ.Hole.Syn
        }
    }.also {
        context.setHover(type.range, lazy {
            Hover(markup("universe")) // TODO: use appropriate name
        })
    }

    private fun elabTermZ(
        ctx: PersistentMap<String, C.TypeZ<Sem>>,
        term: S.Term,
        type: C.TypeZ<Sem>? = null,
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
        term is S.Term.ByteTag && type is C.TypeZ.ByteType? -> C.TermZ.ByteTag(term.data, C.TypeZ.ByteType.Sem)
        term is S.Term.ShortTag && type is C.TypeZ.ShortType? -> C.TermZ.ShortTag(term.data, C.TypeZ.ShortType.Sem)
        term is S.Term.IntTag && type is C.TypeZ.IntType? -> C.TermZ.IntTag(term.data, C.TypeZ.IntType.Sem)
        term is S.Term.LongTag && type is C.TypeZ.LongType? -> C.TermZ.LongTag(term.data, C.TypeZ.LongType.Sem)
        term is S.Term.FloatTag && type is C.TypeZ.FloatType? -> C.TermZ.FloatTag(term.data, C.TypeZ.FloatType.Sem)
        term is S.Term.DoubleTag && type is C.TypeZ.DoubleType? -> C.TermZ.DoubleTag(term.data, C.TypeZ.DoubleType.Sem)
        term is S.Term.StringTag && type is C.TypeZ.StringType? -> C.TermZ.StringTag(term.data, C.TypeZ.StringType.Sem)
        term is S.Term.ByteArrayTag && type is C.TypeZ.CollectionType? -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.ByteType.Sem) }
            C.TermZ.ByteArrayTag(elements, C.TypeZ.ByteArrayType.Sem)
        }

        term is S.Term.IntArrayTag && type is C.TypeZ.CollectionType? -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.IntType.Sem) }
            C.TermZ.IntArrayTag(elements, C.TypeZ.IntArrayType.Sem)
        }

        term is S.Term.LongArrayTag && type is C.TypeZ.CollectionType? -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.LongType.Sem) }
            C.TermZ.LongArrayTag(elements, C.TypeZ.LongArrayType.Sem)
        }

        term is S.Term.ListTag && type is C.TypeZ.CollectionType? -> {
            if (term.elements.isEmpty()) C.TermZ.ListTag(emptyList(), C.TypeZ.ListType(C.TypeZ.EndType.Sem))
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
                    Hover(markup(context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(value.type))))
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
            val element = elabTermS(Context(), term.element, type?.let { C.TermS.VCodeType(lazyOf(it)) })
            when (val elementType = context.unifier.force(element.type)) {
                is C.TermS.VCodeType -> C.TermZ.Splice(element, elementType.element.value)
                else -> errorZ(codeTypeExpected(context.unifier, context.unifier.reifyTermS(persistentListOf(), elementType), term.range))
            }
        }

        term is S.Term.Let -> errorZ(termZExpected(term.range))
        term is S.Term.Function -> {
            val anno = term.anno?.let { context.unifier.reflectTypeZ(elabTypeZ(it)) }
            val body = elabTermZ(ctx, term.body, anno)
            context.setHover(term.name.range, lazy {
                Hover(markup(context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(anno ?: body.type))))
            })
            if (term.anno == null) {
                context.addInlayHint(lazy {
                    val part = InlayHintLabelPart(": ${context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(body.type))}")
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

        term is S.Term.Hole -> errorZ(termZExpected(term.range), type)
        else -> {
            if (type == null) error("failed: inference")
            else {
                val inferred = elabTermZ(ctx, term)
                if (context.unifier.subTypeZ(inferred.type, type)) inferred
                else errorZ(typeZMismatched(context.unifier, type, inferred.type, term.range))
            }
        }
    }.also {
        context.setHover(term.range, lazy {
            Hover(markup(context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(it.type))))
        })
        context.setCompletionItems(term.range, lazy {
            ctx.entries.map { (name, type) ->
                CompletionItem(name).apply {
                    kind = CompletionItemKind.Function
                    labelDetails = CompletionItemLabelDetails().apply {
                        detail = " : ${context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(type))}"
                    }
                }
            }
        })
    }

    private fun elabTermS(
        ctx: Context,
        term: S.Term,
        type: C.TermS<Sem>? = null,
    ): C.TermS<Syn> {
        @Suppress("NAME_SHADOWING") val type = type?.let { context.unifier.force(it) }
        return when {
            term is S.Term.UniverseType && type is C.TermS.UniverseType? -> C.TermS.UniverseType.Syn
            term is S.Term.EndType && type is C.TermS.UniverseType? -> C.TermS.EndType.Syn
            term is S.Term.ByteType && type is C.TermS.UniverseType? -> C.TermS.ByteType.Syn
            term is S.Term.ShortType && type is C.TermS.UniverseType? -> C.TermS.ShortType.Syn
            term is S.Term.IntType && type is C.TermS.UniverseType? -> C.TermS.IntType.Syn
            term is S.Term.LongType && type is C.TermS.UniverseType? -> C.TermS.LongType.Syn
            term is S.Term.FloatType && type is C.TermS.UniverseType? -> C.TermS.FloatType.Syn
            term is S.Term.DoubleType && type is C.TermS.UniverseType? -> C.TermS.DoubleType.Syn
            term is S.Term.StringType && type is C.TermS.UniverseType? -> C.TermS.StringType.Syn
            term is S.Term.CollectionType -> errorS(termSExpected(term.range))
            term is S.Term.ByteArrayType && type is C.TermS.UniverseType? -> C.TermS.ByteArrayType.Syn
            term is S.Term.IntArrayType && type is C.TermS.UniverseType? -> C.TermS.IntArrayType.Syn
            term is S.Term.LongArrayType && type is C.TermS.UniverseType? -> C.TermS.LongArrayType.Syn
            term is S.Term.ListType && type is C.TermS.UniverseType? -> {
                val element = elabTermS(ctx, term.element, C.TermS.UniverseType.Sem)
                C.TermS.ListType(element)
            }

            term is S.Term.CompoundType && type is C.TermS.UniverseType? -> {
                val elements = term.elements.map {
                    val value = elabTermS(ctx, it.value, C.TermS.UniverseType.Sem)
                    context.setHover(it.key.range, lazy {
                        Hover(markup(context.unifier.stringifyTermS(value)))
                    })
                    it.key.text to value
                }.toMap()
                C.TermS.CompoundType(elements)
            }

            term is S.Term.FunctionType && type is C.TermS.UniverseType? -> {
                val dom = elabTermS(ctx, term.dom, C.TermS.UniverseType.Sem)
                if (term.name != null) {
                    context.setHover(term.name.range, lazy {
                        Hover(markup(context.unifier.stringifyTermS(dom)))
                    })
                }
                val cod = elabTermS(ctx.bind(term.name?.text, dom.type), term.cod, C.TermS.UniverseType.Sem)
                C.TermS.FunctionType(term.name?.text, dom, cod)
            }

            term is S.Term.CodeType && type is C.TermS.UniverseType? /* ? */ -> {
                val element = elabTypeZ(term.element)
                C.TermS.CodeType(element)
            }

            term is S.Term.TypeType && type is C.TermS.UniverseType? -> C.TermS.TypeType.Syn
            term is S.Term.ByteTag && type is C.TermS.ByteType? -> C.TermS.ByteTag(term.data)
            term is S.Term.ShortTag && type is C.TermS.ShortType? -> C.TermS.ShortTag(term.data)
            term is S.Term.IntTag && type is C.TermS.IntType? -> C.TermS.IntTag(term.data)
            term is S.Term.LongTag && type is C.TermS.LongType? -> C.TermS.LongTag(term.data)
            term is S.Term.FloatTag && type is C.TermS.FloatType? -> C.TermS.FloatTag(term.data)
            term is S.Term.DoubleTag && type is C.TermS.DoubleType? -> C.TermS.DoubleTag(term.data)
            term is S.Term.StringTag && type is C.TermS.StringType? -> C.TermS.StringTag(term.data)
            term is S.Term.ByteArrayTag && type is C.TermS.ByteArrayType? -> {
                val elements = term.elements.map { elabTermS(ctx, it, C.TermS.ByteType.Sem) }
                C.TermS.ByteArrayTag(elements)
            }

            term is S.Term.IntArrayTag && type is C.TermS.IntArrayType? -> {
                val elements = term.elements.map { elabTermS(ctx, it, C.TermS.IntType.Sem) }
                C.TermS.IntArrayTag(elements)
            }

            term is S.Term.LongArrayTag && type is C.TermS.LongArrayType? -> {
                val elements = term.elements.map { elabTermS(ctx, it, C.TermS.LongType.Sem) }
                C.TermS.LongArrayTag(elements)
            }

            term is S.Term.ListTag && type is C.TermS.VListType? -> {
                if (term.elements.isEmpty()) {
                    C.TermS.ListTag(emptyList(), C.TermS.VListType(lazyOf(C.TermS.EndType.Sem)))
                } else {
                    val elements = mutableListOf<C.TermS<Syn>>()
                    val head = elabTermS(ctx, term.elements.first(), type?.element?.value)
                    elements += head
                    term.elements.subList(1, term.elements.size).mapTo(elements) { elabTermS(ctx, it, head.type) }
                    C.TermS.ListTag(elements, C.TermS.VListType(lazyOf(head.type)))
                }
            }

            term is S.Term.CompoundTag && type is C.TermS.VCompoundType? -> {
                val elements = term.elements.map {
                    val value = elabTermS(ctx, it.value, type?.elements?.get(it.key.text)?.value)
                    context.setHover(it.key.range, lazy {
                        Hover(markup(context.unifier.stringifyTermS(context.unifier.reifyTermS(ctx.values, value.type))))
                    })
                    it.key.text to value
                }.toMap()
                val elementTypes = elements.mapValues { lazyOf(it.value.type) }
                C.TermS.CompoundTag(elements, C.TermS.VCompoundType(elementTypes))
            }

            term is S.Term.IndexedElement -> {
                val target = elabTermZ(persistentMapOf(), term.target)
                when (val targetType = target.type) {
                    is C.TypeZ.CollectionType -> {
                        val index = elabTermS(ctx, term.index, C.TermS.IntType.Sem)
                        C.TermS.IndexedElement(target, index, C.TermS.VCodeType(lazyOf(targetType.element)))
                    }

                    else -> errorS(collectionTypeExpected(context.unifier, targetType, term.target.range))
                }
            }

            term is S.Term.Abs && type == null -> {
                val anno = term.anno?.let { elabTermS(ctx, it, C.TermS.UniverseType.Sem) } ?: context.unifier.fresh(C.TermS.UniverseType.Sem)
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
                val body = elabTermS(ctx.bind(term.name.text, a), term.body)
                C.TermS.Abs(
                    term.name.text, anno, body, C.TermS.VFunctionType(
                        term.name.text,
                        lazyOf(a),
                        C.Clos(ctx.values, lazy { context.unifier.reifyTermS(ctx.values, body.type) }),
                    )
                )
            }

            term is S.Term.Abs && term.anno == null && type is C.TermS.VFunctionType -> {
                val dom = context.unifier.reifyTermS(ctx.values, type.dom.value)
                context.setHover(term.name.range, lazy {
                    Hover(markup(context.unifier.stringifyTermS(dom)))
                })
                context.addInlayHint(lazy {
                    val part = InlayHintLabelPart(": ${context.unifier.stringifyTermS(dom)}")
                    InlayHint(term.name.range.end, forRight(listOf(part)))
                })
                val cod = type.cod(context.unifier, lazyOf(C.TermS.Var(term.name.text, ctx.size, type.dom.value)))
                val body = elabTermS(ctx.bind(term.name.text, type.dom.value), term.body, cod)
                C.TermS.Abs(term.name.text, dom, body, type)
            }

            term is S.Term.Apply -> {
                if (type == null) {
                    val operator = elabTermS(ctx, term.operator)
                    when (val operatorType = context.unifier.force(operator.type)) {
                        is C.TermS.VFunctionType -> {
                            val operand = elabTermS(ctx, term.operand, operatorType.dom.value)
                            val cod = operatorType.cod(context.unifier, lazy { context.unifier.reflectTermS(ctx.values, operand) })
                            C.TermS.Apply(operator, operand, cod)
                        }

                        else -> errorS(functionTypeExpected(context.unifier, context.unifier.reifyTermS(ctx.values, operatorType), term.operator.range))
                    }
                } else {
                    val operand = elabTermS(ctx, term.operand)
                    val operator = elabTermS(
                        ctx, term.operator, C.TermS.VFunctionType(
                            null,
                            lazyOf(operand.type),
                            C.Clos(ctx.values, lazy { context.unifier.reifyTermS(ctx.values, type) }),
                        )
                    )
                    C.TermS.Apply(operator, operand, type)
                }
            }

            term is S.Term.Quote && type is C.TermS.TypeType -> {
                val element = elabTypeZ(term.element)
                C.TermS.QuoteType(element)
            }

            term is S.Term.Quote && type is C.TermS.VCodeType -> {
                val element = elabTermZ(persistentMapOf(), term.element, type.element.value)
                C.TermS.QuoteTerm(element, type)
            }

            term is S.Term.Quote && type == null -> {
                when (val element = elabObjZ(persistentMapOf(), term.element)) {
                    is C.TypeZ -> C.TermS.QuoteType(element)
                    is C.TermZ -> C.TermS.QuoteTerm(element, C.TermS.VCodeType(lazyOf(element.type)))
                }
            }

            term is S.Term.Splice -> errorS(termZExpected(term.range))
            term is S.Term.Let -> {
                val anno = term.anno?.let { elabTermS(ctx, it, C.TermS.UniverseType.Sem) }
                val a = anno?.let { context.unifier.reflectTermS(ctx.values, it) }
                val init = elabTermS(ctx, term.init, a)
                context.setHover(term.name.range, lazy {
                    Hover(markup(context.unifier.stringifyTermS(anno ?: context.unifier.reifyTermS(ctx.values, init.type))))
                })
                if (term.anno == null) {
                    context.addInlayHint(lazy {
                        val part = InlayHintLabelPart(": ${context.unifier.stringifyTermS(context.unifier.reifyTermS(ctx.values, init.type))}")
                        InlayHint(term.name.range.end, forRight(listOf(part)))
                    })
                }
                val next = elabTermS(ctx.bind(term.name.text, a ?: init.type, lazy { context.unifier.reflectTermS(ctx.values, init) }), term.next, type)
                C.TermS.Let(term.name.text, init, next, type ?: next.type)
            }

            term is S.Term.Function -> errorS(termZExpected(term.range))
            term is S.Term.Var && type == null -> {
                when (val level = ctx.levels[term.name]) {
                    null -> errorS(notFound(term.name, term.range))
                    else -> C.TermS.Var(term.name, level, ctx.types[level])
                }
            }

            term is S.Term.Hole -> errorS(termSExpected(term.range), type)
            else -> {
                if (type == null) error("failed: inference")
                else {
                    val inferred = elabTermS(ctx, term)
                    if (context.unifier.unifyValue(ctx.size, inferred.type, type)) inferred
                    else errorS(
                        typeSMismatched(
                            context.unifier,
                            context.unifier.reifyTermS(ctx.values, type),
                            context.unifier.reifyTermS(ctx.values, inferred.type),
                            term.range
                        )
                    )
                }
            }
        }.also {
            context.setHover(term.range, lazy {
                Hover(markup(context.unifier.stringifyTermS(context.unifier.reifyTermS(ctx.values, it.type))))
            })
            context.setCompletionItems(term.range, lazy {
                ctx.levels.map { (name, level) ->
                    CompletionItem(name).apply {
                        kind = CompletionItemKind.Variable
                        labelDetails = CompletionItemLabelDetails().apply {
                            detail = " : ${context.unifier.stringifyTermS(context.unifier.reifyTermS(ctx.values, ctx.types[level]))}"
                        }
                        detail = context.unifier.stringifyTermS(context.unifier.reifyTermS(ctx.values, ctx.values[level].value))
                    }
                }
            })
        }
    }

    private class Context private constructor(
        val levels: PersistentMap<String, Int>,
        val types: PersistentList<C.TermS<Sem>>,
        val values: PersistentList<Lazy<C.TermS<Sem>>>,
    ) {
        val size: Int get() = types.size

        fun bind(
            name: String?,
            type: C.TermS<Sem>,
            term: Lazy<C.TermS<Sem>>? = null,
        ): Context = Context(
            levels = name?.let { levels + (name to size) } ?: levels,
            types = types + type,
            values = values + (term ?: lazyOf(C.TermS.Var(name, size, type))),
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
        type: C.TypeZ<Sem>? = null,
    ): C.TermZ {
        context.addDiagnostic(diagnostic)
        return C.TermZ.Hole(type ?: C.TypeZ.EndType.Sem)
    }

    private fun errorS(
        diagnostic: Diagnostic,
        type: C.TermS<Sem>? = null,
    ): C.TermS<Syn> {
        context.addDiagnostic(diagnostic)
        return type?.let { C.TermS.Hole(it) } ?: C.TermS.Hole.Syn
    }

    companion object : Phase<S.Root, C.Root> {
        override operator fun invoke(
            context: Phase.Context,
            input: S.Root,
        ): C.Root = Elab(context).elabRoot(input)
    }
}
