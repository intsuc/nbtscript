package nbtscript.phase

import kotlinx.collections.immutable.*
import nbtscript.ast.Core.Kind.Sem
import nbtscript.ast.Core.Kind.Syn
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either.forRight
import nbtscript.ast.Core as C
import nbtscript.ast.Surface as S

// TODO: create report messages lazily
class Elab private constructor(
    private val context: Phase.Context = Phase.Context(),
) {
    private fun elabRoot(
        root: S.Root,
    ): C.Root {
        val body = elabTermZ(Context(), root.body)
        return C.Root(body)
    }

    private fun elabObjZ(
        ctx: Context,
        term: S.Term,
    ): C.ObjZ<Syn> = when {
        term !is S.TypeZ && term !is S.TermZ -> errorZ(objZExpected(term.range))
        term is S.Term.ByteType -> C.TypeZ.ByteType.Syn
        term is S.Term.ShortType -> C.TypeZ.ShortType.Syn
        term is S.Term.IntType -> C.TypeZ.IntType.Syn
        term is S.Term.LongType -> C.TypeZ.LongType.Syn
        term is S.Term.FloatType -> C.TypeZ.FloatType.Syn
        term is S.Term.DoubleType -> C.TypeZ.DoubleType.Syn
        term is S.Term.StringType -> C.TypeZ.StringType.Syn
        term is S.Term.CollectionType -> {
            val element = elabTypeZ(ctx, term.element)
            C.TypeZ.CollectionType(element)
        }

        term is S.Term.ByteArrayType -> C.TypeZ.ByteArrayType.Syn
        term is S.Term.IntArrayType -> C.TypeZ.IntArrayType.Syn
        term is S.Term.LongArrayType -> C.TypeZ.LongArrayType.Syn
        term is S.Term.ListType -> {
            val element = elabTypeZ(ctx, term.element)
            C.TypeZ.ListType(element)
        }

        term is S.Term.CompoundType -> {
            val elements = term.elements.map {
                val value = elabTypeZ(ctx, it.value)
                context.setHover(it.key.range, lazy {
                    Hover(markup(context.unifier.stringifyTypeZ(value)))
                })
                it.key.text to value
            }.toMap()
            C.TypeZ.CompoundType(elements)
        }

        term is S.Term.ByteTag -> C.TermZ.ByteTag(term.data)
        term is S.Term.ShortTag -> C.TermZ.ShortTag(term.data)
        term is S.Term.IntTag -> C.TermZ.IntTag(term.data)
        term is S.Term.LongTag -> C.TermZ.LongTag(term.data)
        term is S.Term.FloatTag -> C.TermZ.FloatTag(term.data)
        term is S.Term.DoubleTag -> C.TermZ.DoubleTag(term.data)
        term is S.Term.StringTag -> C.TermZ.StringTag(term.data)
        term is S.Term.ByteArrayTag -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.ByteType.Sem) }
            C.TermZ.ByteArrayTag(elements)
        }

        term is S.Term.IntArrayTag -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.IntType.Sem) }
            C.TermZ.IntArrayTag(elements)
        }

        term is S.Term.LongArrayTag -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.LongType.Sem) }
            C.TermZ.LongArrayTag(elements)
        }

        term is S.Term.ListTag -> {
            if (term.elements.isEmpty()) C.TermZ.ListTag(emptyList(), C.TypeZ.ListType(C.TypeZ.EndType.Sem))
            else {
                val elements = mutableListOf<C.TermZ>()
                val head = elabTermZ(ctx, term.elements.first())
                elements += head
                term.elements.subList(1, term.elements.size).mapTo(elements) { elabTermZ(ctx, it, head.type) }
                C.TermZ.ListTag(elements, C.TypeZ.ListType(head.type))
            }
        }

        term is S.Term.CompoundTag -> {
            val elements = term.elements.map {
                val value = elabTermZ(ctx, it.value)
                context.setHover(it.key.range, lazy {
                    Hover(markup(context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(ctx.values, value.type))))
                })
                it.key.text to value
            }.toMap()
            val elementTypes = elements.mapValues { it.value.type }
            C.TermZ.CompoundTag(elements, C.TypeZ.CompoundType(elementTypes))
        }

        term is S.Term.Splice -> {
            val element = elabTermS(ctx, term.element)
            when (val elementType = context.unifier.force(element.type)) {
                is C.TermS.VCodeType -> C.TermZ.Splice(element, elementType.element.value)
                else -> errorZ(codeTypeExpected(context.unifier, context.unifier.reifyTermS(persistentListOf(), elementType), term.range))
            }
        }

        term is S.Term.Fun -> {
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
            val next = elabTermZ(ctx.bindZ(term.name.text, anno ?: body.type), term.next)
            C.TermZ.Fun(term.name.text, body, next, next.type)
        }

        term is S.Term.Var -> {
            if (ctx.typesZ.contains(term.name)) C.TermZ.Run(term.name, ctx.typesZ[term.name]!!)
            else errorZ(notFound(term.name, term.range))
        }

        term is S.Term.Hole -> errorZ(objZExpected(term.range))
        else -> errorZ(objZExpected(term.range)) // unreachable?
    }.also {
        context.setHover(term.range, lazy {
            when (it) {
                is C.TypeZ -> Hover(markup("type₀"))
                is C.TermZ -> Hover(markup(context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(ctx.values, it.type))))
            }
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

    private fun elabTypeZ(
        ctx: Context,
        type: S.Term,
    ): C.TypeZ<Syn> = when (type) {
        !is S.TypeZ -> {
            context.addDiagnostic(typeZExpected(type.range))
            C.TypeZ.Hole.Syn
        }

        is S.Term.ByteType -> C.TypeZ.ByteType.Syn
        is S.Term.ShortType -> C.TypeZ.ShortType.Syn
        is S.Term.IntType -> C.TypeZ.IntType.Syn
        is S.Term.LongType -> C.TypeZ.LongType.Syn
        is S.Term.FloatType -> C.TypeZ.FloatType.Syn
        is S.Term.DoubleType -> C.TypeZ.DoubleType.Syn
        is S.Term.StringType -> C.TypeZ.StringType.Syn
        is S.Term.CollectionType -> {
            val element = elabTypeZ(ctx, type.element)
            C.TypeZ.CollectionType(element)
        }

        is S.Term.ByteArrayType -> C.TypeZ.ByteArrayType.Syn
        is S.Term.IntArrayType -> C.TypeZ.IntArrayType.Syn
        is S.Term.LongArrayType -> C.TypeZ.LongArrayType.Syn
        is S.Term.ListType -> {
            val element = elabTypeZ(ctx, type.element)
            C.TypeZ.ListType(element)
        }

        is S.Term.CompoundType -> {
            val elements = type.elements.map {
                val value = elabTypeZ(ctx, it.value)
                context.setHover(it.key.range, lazy {
                    Hover(markup(context.unifier.stringifyTypeZ(value)))
                })
                it.key.text to value
            }.toMap()
            C.TypeZ.CompoundType(elements)
        }

        is S.Term.Splice -> {
            val element = elabTermS(ctx, type.element, C.TermS.TypeType.Sem)
            C.TypeZ.Splice(element)
        }

        is S.Term.Hole -> {
            context.addDiagnostic(typeZExpected(type.range))
            C.TypeZ.Hole.Syn
        }
    }.also {
        context.setHover(type.range, lazy {
            Hover(markup("type₀"))
        })
    }

    private fun elabTermZ(
        ctx: Context,
        term: S.Term,
        type: C.TypeZ<Sem>? = null,
    ): C.TermZ = when {
        term !is S.TermZ -> errorZ(termZExpected(term.range))
        term is S.Term.ByteTag && type is C.TypeZ.ByteType? -> C.TermZ.ByteTag(term.data)
        term is S.Term.ShortTag && type is C.TypeZ.ShortType? -> C.TermZ.ShortTag(term.data)
        term is S.Term.IntTag && type is C.TypeZ.IntType? -> C.TermZ.IntTag(term.data)
        term is S.Term.LongTag && type is C.TypeZ.LongType? -> C.TermZ.LongTag(term.data)
        term is S.Term.FloatTag && type is C.TypeZ.FloatType? -> C.TermZ.FloatTag(term.data)
        term is S.Term.DoubleTag && type is C.TypeZ.DoubleType? -> C.TermZ.DoubleTag(term.data)
        term is S.Term.StringTag && type is C.TypeZ.StringType? -> C.TermZ.StringTag(term.data)
        term is S.Term.ByteArrayTag && type is C.TypeZ.CollectionType? -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.ByteType.Sem) }
            C.TermZ.ByteArrayTag(elements)
        }

        term is S.Term.IntArrayTag && type is C.TypeZ.CollectionType? -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.IntType.Sem) }
            C.TermZ.IntArrayTag(elements)
        }

        term is S.Term.LongArrayTag && type is C.TypeZ.CollectionType? -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.LongType.Sem) }
            C.TermZ.LongArrayTag(elements)
        }

        term is S.Term.ListTag && type is C.TypeZ.CollectionType? -> {
            if (term.elements.isEmpty()) C.TermZ.ListTag(emptyList(), C.TypeZ.ListType(C.TypeZ.EndType.Sem))
            else {
                val elements = mutableListOf<C.TermZ>()
                val head = elabTermZ(ctx, term.elements.first(), type?.element)
                elements += head
                term.elements.subList(1, term.elements.size).mapTo(elements) { elabTermZ(ctx, it, head.type) }
                C.TermZ.ListTag(elements, type ?: C.TypeZ.ListType(head.type))
            }
        }

        term is S.Term.CompoundTag && type is C.TypeZ.CompoundType? -> {
            val elements = term.elements.map {
                val value = elabTermZ(ctx, it.value, type?.elements?.get(it.key.text))
                context.setHover(it.key.range, lazy {
                    Hover(markup(context.unifier.stringifyTypeZ(context.unifier.reifyTypeZ(ctx.values, value.type))))
                })
                it.key.text to value
            }.toMap()
            C.TermZ.CompoundTag(elements, type ?: C.TypeZ.CompoundType(elements.mapValues { it.value.type }))
        }

        term is S.Term.Splice -> {
            val element = elabTermS(ctx, term.element, type?.let { C.TermS.VCodeType(lazyOf(it)) })
            when (val elementType = context.unifier.force(element.type)) {
                is C.TermS.VCodeType -> C.TermZ.Splice(element, elementType.element.value)
                else -> errorZ(codeTypeExpected(context.unifier, context.unifier.reifyTermS(persistentListOf(), elementType), term.range))
            }
        }

        term is S.Term.Fun -> {
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
            C.TermZ.Fun(term.name.text, body, next, next.type)
        }

        term is S.Term.Var && type == null -> {
            if (ctx.typesZ.contains(term.name)) C.TermZ.Run(term.name, ctx.typesZ[term.name]!!)
            else errorZ(notFound(term.name, term.range))
        }

        term is S.Term.Hole -> errorZ(termZExpected(term.range), type)
        else -> {
            if (type == null) error("failed: inference")
            else {
                val inferred = elabTermZ(ctx, term)
                if (context.unifier.subTypeZ(inferred.type, type)) inferred
                else errorZ(typeZMismatched(ctx.values, context.unifier, type, inferred.type, term.range))
            }
        }
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

    private fun elabTermS(
        ctx: Context,
        term: S.Term,
        type: C.TermS<Sem>? = null,
    ): C.TermS<Syn> {
        @Suppress("NAME_SHADOWING") val type = type?.let { context.unifier.force(it) }
        return when {
            term !is S.TermS -> errorS(termZExpected(term.range))
            term is S.Term.UniverseType && type is C.TermS.UniverseType? -> C.TermS.UniverseType.Syn
            term is S.Term.EndType && type is C.TermS.UniverseType? -> C.TermS.EndType.Syn
            term is S.Term.ByteType && type is C.TermS.UniverseType? -> C.TermS.ByteType.Syn
            term is S.Term.ShortType && type is C.TermS.UniverseType? -> C.TermS.ShortType.Syn
            term is S.Term.IntType && type is C.TermS.UniverseType? -> C.TermS.IntType.Syn
            term is S.Term.LongType && type is C.TermS.UniverseType? -> C.TermS.LongType.Syn
            term is S.Term.FloatType && type is C.TermS.UniverseType? -> C.TermS.FloatType.Syn
            term is S.Term.DoubleType && type is C.TermS.UniverseType? -> C.TermS.DoubleType.Syn
            term is S.Term.StringType && type is C.TermS.UniverseType? -> C.TermS.StringType.Syn
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

            term is S.Term.NodeType && type is C.TermS.UniverseType? -> C.TermS.NodeType.Syn
            term is S.Term.FunType && type is C.TermS.UniverseType? -> {
                val dom = elabTermS(ctx, term.dom, C.TermS.UniverseType.Sem)
                if (term.name != null) {
                    context.setHover(term.name.range, lazy {
                        Hover(markup(context.unifier.stringifyTermS(dom)))
                    })
                }
                val cod = elabTermS(ctx.bindS(term.name?.text, context.unifier.reflectTermS(ctx.values, dom)), term.cod, C.TermS.UniverseType.Sem)
                C.TermS.FunType(term.name?.text, dom, cod)
            }

            term is S.Term.CodeType && type is C.TermS.UniverseType? -> {
                val element = elabTypeZ(ctx, term.element)
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
                    C.TermS.ListTag(elements, type ?: C.TermS.VListType(lazyOf(head.type)))
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
                C.TermS.CompoundTag(elements, type ?: C.TermS.VCompoundType(elements.mapValues { lazyOf(it.value.type) }))
            }

            type is C.TermS.NodeType -> {
                when (term) {
                    is S.Term.StringTag -> {
                        val name = elabTermS(ctx, term)
                        C.TermS.CompoundChildNode(name)
                    }

                    is S.Term.ListTag -> {
                        when (val size = term.elements.size) {
                            0 -> C.TermS.AllElementsNode.Syn
                            1 -> {
                                val pattern = elabTermS(ctx, term.elements.first())
                                when (context.unifier.force(pattern.type)) {
                                    is C.TermS.VCompoundType -> C.TermS.MatchElementNode(pattern)
                                    is C.TermS.IntType -> C.TermS.IndexedElementNode(pattern)
                                    else -> errorS(invalidNode(term.range))
                                }
                            }

                            else -> errorS(sizeMismatched(1, size, term.range))
                        }
                    }

                    is S.Term.CompoundTag -> {
                        val pattern = elabTermS(ctx, term)
                        C.TermS.MatchObjectNode(pattern)
                    }

                    else -> errorS(invalidNode(term.range))
                }
            }

            term is S.Term.Get -> {
                val target = elabTermS(ctx, term.target)
                val path = elabTermS(ctx, term.path, C.TermS.VListType(lazyOf(C.TermS.NodeType.Sem)))
                C.TermS.Get(target, path, C.TermS.VCodeType(lazyOf(C.TypeZ.EndType.Sem) /* TODO */))
            }

            term is S.Term.Abs && type == null -> {
                val anno = term.anno?.let { elabTermS(ctx, it, C.TermS.UniverseType.Sem) } ?: context.unifier.fresh()
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
                C.TermS.Abs(
                    term.name.text, anno, body, C.TermS.VFunType(
                        term.name.text,
                        lazyOf(a),
                        C.Clos(ctx.values, lazy { context.unifier.reifyTermS(ctx.values, body.type) }),
                    )
                )
            }

            term is S.Term.Abs && term.anno == null && type is C.TermS.VFunType -> {
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
                C.TermS.Abs(term.name.text, dom, body, type)
            }

            term is S.Term.Apply && type != null -> {
                val operand = elabTermS(ctx, term.operand)
                val operator = elabTermS(
                    ctx, term.operator, C.TermS.VFunType(
                        null,
                        lazyOf(operand.type),
                        C.Clos(ctx.values, lazy { context.unifier.reifyTermS(ctx.values, type) }),
                    )
                )
                C.TermS.Apply(operator, operand, type)
            }

            term is S.Term.Apply -> {
                val operator = elabTermS(ctx, term.operator)
                when (val operatorType = context.unifier.force(operator.type)) {
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

            term is S.Term.Quote && type is C.TermS.TypeType -> {
                val element = elabTypeZ(ctx, term.element)
                C.TermS.QuoteType(element)
            }

            term is S.Term.Quote && type is C.TermS.VCodeType -> {
                val element = elabTermZ(ctx, term.element, type.element.value)
                C.TermS.QuoteTerm(element, type)
            }

            term is S.Term.Quote && type == null -> {
                when (val element = elabObjZ(ctx, term.element)) {
                    is C.TypeZ -> C.TermS.QuoteType(element)
                    is C.TermZ -> C.TermS.QuoteTerm(element, C.TermS.VCodeType(lazyOf(element.type)))
                }
            }

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
                val next = elabTermS(ctx.bindS(term.name.text, a ?: init.type, lazy { context.unifier.reflectTermS(ctx.values, init) }), term.next, type)
                C.TermS.Let(term.name.text, init, next, type ?: next.type)
            }

            term is S.Term.Var && type == null -> {
                when (val level = ctx.levels[term.name]) {
                    null -> errorS(notFound(term.name, term.range))
                    else -> C.TermS.Var(term.name, level, ctx.typesS[level])
                }
            }

            term is S.Term.Hole -> errorS(termSExpected(term.range), type)
            else -> {
                if (type == null) error("failed: inference")
                else {
                    val inferred = elabTermS(ctx, term)
                    tryUnify(ctx, inferred.type, type, term.range) { inferred }
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
                            detail = " : ${context.unifier.stringifyTermS(context.unifier.reifyTermS(ctx.values, ctx.typesS[level]))}"
                        }
                        detail = context.unifier.stringifyTermS(context.unifier.reifyTermS(ctx.values, ctx.values[level].value))
                    }
                }
            })
        }
    }

    private inline fun tryUnify(
        ctx: Context,
        term1: C.TermS<Sem>,
        term2: C.TermS<Sem>,
        range: Range,
        block: () -> C.TermS<Syn>,
    ): C.TermS<Syn> =
        if (context.unifier.unifyTermS(ctx.size, term1, term2)) block()
        else errorS(
            typeSMismatched(
                context.unifier,
                context.unifier.reifyTermS(ctx.values, term1),
                context.unifier.reifyTermS(ctx.values, term2),
                range,
            )
        )

    private class Context private constructor(
        val typesZ: PersistentMap<String, C.TypeZ<Sem>>,
        val levels: PersistentMap<String, Int>,
        val typesS: PersistentList<C.TermS<Sem>>,
        val values: PersistentList<Lazy<C.TermS<Sem>>>,
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
        )

        companion object {
            operator fun invoke(): Context = Context(
                persistentMapOf(),
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
        return type?.let { C.TermZ.Hole(type) } ?: C.TermZ.Hole.Syn
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
