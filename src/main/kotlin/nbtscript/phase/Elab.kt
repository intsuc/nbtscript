package nbtscript.phase

import kotlinx.collections.immutable.*
import nbtscript.phase.Report.*
import nbtscript.ast.Core as C
import nbtscript.ast.Core.Value as TypeS
import nbtscript.ast.Surface as S

class Elab private constructor(
    private val reports: Reports = Reports(),
) {
    private fun elabRoot(
        root: S.Root,
    ): C.Root {
        val body = elabTermZ(persistentMapOf(), root.body)
        return C.Root(body)
    }

    private fun elabTypeZ(
        type: S.TypeZ,
    ): C.TypeZ = when (type) {
        is S.TypeZ.ByteZ -> C.TypeZ.ByteZ
        is S.TypeZ.ShortZ -> C.TypeZ.ShortZ
        is S.TypeZ.IntZ -> C.TypeZ.IntZ
        is S.TypeZ.LongZ -> C.TypeZ.LongZ
        is S.TypeZ.FloatZ -> C.TypeZ.FloatZ
        is S.TypeZ.DoubleZ -> C.TypeZ.DoubleZ
        is S.TypeZ.StringZ -> C.TypeZ.StringZ
        is S.TypeZ.ByteArrayZ -> C.TypeZ.ByteArrayZ
        is S.TypeZ.IntArrayZ -> C.TypeZ.IntArrayZ
        is S.TypeZ.LongArrayZ -> C.TypeZ.LongArrayZ
        is S.TypeZ.ListZ -> {
            val element = elabTypeZ(type.element)
            C.TypeZ.ListZ(element)
        }

        is S.TypeZ.CompoundZ -> {
            val elements = type.elements.map { it.key to elabTypeZ(it.value) }.toMap()
            C.TypeZ.CompoundZ(elements)
        }

        is S.TypeZ.Hole -> C.TypeZ.Hole
    }

    private fun elabTermZ(
        ctx: PersistentMap<String, C.TypeZ>,
        term: S.TermZ,
        type: C.TypeZ? = null,
    ): C.TermZ = when {
        term is S.TermZ.ByteTag && type is C.TypeZ.ByteZ? -> C.TermZ.ByteTag(term.data, C.TypeZ.ByteZ)
        term is S.TermZ.ShortTag && type is C.TypeZ.ShortZ? -> C.TermZ.ShortTag(term.data, C.TypeZ.ShortZ)
        term is S.TermZ.IntTag && type is C.TypeZ.IntZ? -> C.TermZ.IntTag(term.data, C.TypeZ.IntZ)
        term is S.TermZ.LongTag && type is C.TypeZ.LongZ? -> C.TermZ.LongTag(term.data, C.TypeZ.LongZ)
        term is S.TermZ.FloatTag && type is C.TypeZ.FloatZ? -> C.TermZ.FloatTag(term.data, C.TypeZ.FloatZ)
        term is S.TermZ.DoubleTag && type is C.TypeZ.DoubleZ? -> C.TermZ.DoubleTag(term.data, C.TypeZ.DoubleZ)
        term is S.TermZ.StringTag && type is C.TypeZ.StringZ? -> C.TermZ.StringTag(term.data, C.TypeZ.StringZ)
        term is S.TermZ.ByteArrayTag && type is C.TypeZ.ByteArrayZ? -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.ByteZ) }
            C.TermZ.ByteArrayTag(elements, C.TypeZ.ByteArrayZ)
        }

        term is S.TermZ.IntArrayTag && type is C.TypeZ.IntArrayZ? -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.IntZ) }
            C.TermZ.IntArrayTag(elements, C.TypeZ.IntArrayZ)
        }

        term is S.TermZ.LongArrayTag && type is C.TypeZ.LongArrayZ? -> {
            val elements = term.elements.map { elabTermZ(ctx, it, C.TypeZ.LongZ) }
            C.TermZ.LongArrayTag(elements, C.TypeZ.LongArrayZ)
        }

        term is S.TermZ.ListTag && type is C.TypeZ.ListZ? -> {
            if (term.elements.isEmpty()) C.TermZ.ListTag(emptyList(), C.TypeZ.ListZ(C.TypeZ.EndZ))
            else {
                val elements = mutableListOf<C.TermZ>()
                val head = elabTermZ(ctx, term.elements.first(), type?.element)
                elements += head
                term.elements.subList(1, term.elements.size).mapTo(elements) { elabTermZ(ctx, it, head.type) }
                C.TermZ.ListTag(elements, C.TypeZ.ListZ(head.type))
            }
        }

        term is S.TermZ.CompoundTag && type is C.TypeZ.CompoundZ? -> {
            val elements = term.elements.map { it.key to elabTermZ(ctx, it.value, type?.elements?.get(it.key)) }.toMap()
            val elementTypes = elements.mapValues { it.value.type }
            C.TermZ.CompoundTag(elements, C.TypeZ.CompoundZ(elementTypes))
        }

        term is S.TermZ.Function -> {
            val body = elabTermZ(ctx, term.body)
            val next = elabTermZ(ctx + (term.name to body.type), term.next, type)
            C.TermZ.Function(term.name, body, next, next.type)
        }

        term is S.TermZ.Run -> {
            if (ctx.contains(term.name)) C.TermZ.Run(term.name, ctx[term.name]!!)
            else errorZ(NotFound(term.name, term.range))
        }

        term is S.TermZ.Splice -> {
            val element = elabTermS(Context(), term.element, type?.let { TypeS.CodeS(it) })
            when (val elementType = element.type) {
                is TypeS.CodeS -> C.TermZ.Splice(element, elementType.element)
                else -> errorZ(CodeExpected(elementType, term.range))
            }
        }

        term is S.TermZ.Hole -> C.TermZ.Hole(C.TypeZ.EndZ)
        else -> {
            if (type == null) error("failed: inference")
            else {
                val inferred = elabTermZ(ctx, term)
                if (convZ(inferred.type, type)) inferred
                else errorZ(TypeZMismatched(type, inferred.type, term.range))
            }
        }
    }

    private fun elabTermS(
        ctx: Context,
        term: S.TermS,
        type: TypeS? = null,
    ): C.TermS = when {
        term is S.TermS.UniverseS && type is TypeS.TypeS? -> C.TermS.TypeS(TypeS.TypeS)
        term is S.TermS.EndS && type is TypeS.TypeS? -> C.TermS.EndS(TypeS.TypeS)
        term is S.TermS.ByteS && type is TypeS.TypeS? -> C.TermS.ByteS(TypeS.TypeS)
        term is S.TermS.ShortS && type is TypeS.TypeS? -> C.TermS.ShortS(TypeS.TypeS)
        term is S.TermS.IntS && type is TypeS.TypeS? -> C.TermS.IntS(TypeS.TypeS)
        term is S.TermS.LongS && type is TypeS.TypeS? -> C.TermS.LongS(TypeS.TypeS)
        term is S.TermS.FloatS && type is TypeS.TypeS? -> C.TermS.FloatS(TypeS.TypeS)
        term is S.TermS.DoubleS && type is TypeS.TypeS? -> C.TermS.DoubleS(TypeS.TypeS)
        term is S.TermS.StringS && type is TypeS.TypeS? -> C.TermS.StringS(TypeS.TypeS)
        term is S.TermS.ByteArrayS && type is TypeS.TypeS? -> C.TermS.ByteArrayS(TypeS.TypeS)
        term is S.TermS.IntArrayS && type is TypeS.TypeS? -> C.TermS.IntArrayS(TypeS.TypeS)
        term is S.TermS.LongArrayS && type is TypeS.TypeS? -> C.TermS.LongArrayS(TypeS.TypeS)
        term is S.TermS.ListS && type is TypeS.TypeS? -> {
            val element = elabTermS(ctx, term.element, TypeS.TypeS)
            C.TermS.ListS(element, TypeS.TypeS)
        }

        term is S.TermS.CompoundS && type is TypeS.TypeS? -> {
            val elements = term.elements.map { it.key to elabTermS(ctx, it.value, TypeS.TypeS) }.toMap()
            C.TermS.CompoundS(elements, TypeS.TypeS)
        }

        term is S.TermS.ArrowS && type is TypeS.TypeS? -> {
            val dom = elabTermS(ctx, term.dom, TypeS.TypeS)
            val cod = elabTermS(ctx.bind(term.name, dom.type), term.cod, TypeS.TypeS)
            C.TermS.ArrowS(term.name, dom, cod, TypeS.TypeS)
        }

        term is S.TermS.CodeS && type is TypeS.TypeS? -> {
            val element = elabTypeZ(term.element)
            C.TermS.CodeS(element, TypeS.TypeS)
        }

        term is S.TermS.TypeZ && type is TypeS.TypeS? -> C.TermS.TypeZ(TypeS.TypeS)
        term is S.TermS.ByteTag && type is TypeS.ByteS? -> C.TermS.ByteTag(term.data, TypeS.ByteS)
        term is S.TermS.ShortTag && type is TypeS.ShortS? -> C.TermS.ShortTag(term.data, TypeS.ShortS)
        term is S.TermS.IntTag && type is TypeS.IntS? -> C.TermS.IntTag(term.data, TypeS.IntS)
        term is S.TermS.LongTag && type is TypeS.LongS? -> C.TermS.LongTag(term.data, TypeS.LongS)
        term is S.TermS.FloatTag && type is TypeS.FloatS? -> C.TermS.FloatTag(term.data, TypeS.FloatS)
        term is S.TermS.DoubleTag && type is TypeS.DoubleS? -> C.TermS.DoubleTag(term.data, TypeS.DoubleS)
        term is S.TermS.StringTag && type is TypeS.StringS? -> C.TermS.StringTag(term.data, TypeS.StringS)
        term is S.TermS.ByteArrayTag && type is TypeS.ByteArrayS? -> {
            val elements = term.elements.map { elabTermS(ctx, it, TypeS.ByteS) }
            C.TermS.ByteArrayTag(elements, TypeS.ByteArrayS)
        }

        term is S.TermS.IntArrayTag && type is TypeS.IntArrayS? -> {
            val elements = term.elements.map { elabTermS(ctx, it, TypeS.IntS) }
            C.TermS.IntArrayTag(elements, TypeS.IntArrayS)
        }

        term is S.TermS.LongArrayTag && type is TypeS.LongArrayS? -> {
            val elements = term.elements.map { elabTermS(ctx, it, TypeS.LongS) }
            C.TermS.LongArrayTag(elements, TypeS.LongArrayS)
        }

        term is S.TermS.ListTag && type is TypeS.ListS? -> {
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

        term is S.TermS.CompoundTag && type is TypeS.CompoundS? -> {
            val elements = term.elements.map { it.key to elabTermS(ctx, it.value, type?.elements?.get(it.key)?.value) }.toMap()
            val elementTypes = elements.mapValues { lazyOf(it.value.type) }
            C.TermS.CompoundTag(elements, TypeS.CompoundS(elementTypes))
        }

        term is S.TermS.Abs && type == null -> {
            val anno = elabTermS(ctx, term.anno)
            val body = elabTermS(ctx, term.body)
            C.TermS.Abs(
                term.name, anno, body, TypeS.ArrowS(
                    null,
                    lazy { reflect(ctx.values, anno) },
                    C.Clos(ctx.values, lazy { reify(ctx.values, body.type) })
                )
            )
        }

        term is S.TermS.Apply -> {
            if (type == null) {
                val operator = elabTermS(ctx, term.operator)
                when (val operatorType = operator.type) {
                    is TypeS.ArrowS -> {
                        val operand = elabTermS(ctx, term.operand)
                        val cod = operatorType.cod(lazy { reflect(ctx.values, operand) })
                        C.TermS.Apply(operator, operand, cod)
                    }

                    else -> errorS(ArrowExpected(operatorType, term.operator.range))
                }
            } else {
                val operand = elabTermS(ctx, term.operand)
                val operator = elabTermS(
                    ctx, term.operator, TypeS.ArrowS(
                        null,
                        lazyOf(operand.type),
                        C.Clos(ctx.values, lazy { reify(ctx.values, type) })
                    )
                )
                C.TermS.Apply(operator, operand, type)
            }
        }

        term is S.TermS.Quote && type is TypeS.CodeS? -> {
            val element = elabTermZ(persistentMapOf(), term.element, type?.element)
            C.TermS.Quote(element, TypeS.CodeS(element.type))
        }

        term is S.TermS.Let -> {
            val init = elabTermS(ctx, term.init)
            val next = elabTermS(ctx.bind(term.name, init.type, lazy { reflect(ctx.values, init) }), term.next, type)
            C.TermS.Let(term.name, init, next, type ?: next.type)
        }

        term is S.TermS.Var && type == null -> {
            when (val level = ctx.levels[term.name]) {
                null -> errorS(NotFound(term.name, term.range))
                else -> C.TermS.Var(term.name, level, ctx.types[level])
            }
        }

        term is S.TermS.Hole -> C.TermS.Hole(TypeS.EndS)
        else -> {
            if (type == null) error("failed: inference")
            else {
                val inferred = elabTermS(ctx, term)
                if (convS(ctx.size, inferred.type, type)) inferred
                else errorS(TypeSMismatched(type, inferred.type, term.range))
            }
        }
    }

    private fun convZ(
        type1: C.TypeZ,
        type2: C.TypeZ,
    ): Boolean = when {
        type1 == type2 -> true
        type1 is C.TypeZ.EndZ && type2 is C.TypeZ.EndZ -> true
        type1 is C.TypeZ.ByteZ && type2 is C.TypeZ.ByteZ -> true
        type1 is C.TypeZ.ShortZ && type2 is C.TypeZ.ShortZ -> true
        type1 is C.TypeZ.IntZ && type2 is C.TypeZ.IntZ -> true
        type1 is C.TypeZ.LongZ && type2 is C.TypeZ.LongZ -> true
        type1 is C.TypeZ.FloatZ && type2 is C.TypeZ.FloatZ -> true
        type1 is C.TypeZ.DoubleZ && type2 is C.TypeZ.DoubleZ -> true
        type1 is C.TypeZ.StringZ && type2 is C.TypeZ.StringZ -> true
        type1 is C.TypeZ.ByteArrayZ && type2 is C.TypeZ.ByteArrayZ -> true
        type1 is C.TypeZ.IntArrayZ && type2 is C.TypeZ.IntArrayZ -> true
        type1 is C.TypeZ.LongArrayZ && type2 is C.TypeZ.LongArrayZ -> true
        type1 is C.TypeZ.ListZ && type2 is C.TypeZ.ListZ -> convZ(type1.element, type2.element)
        type1 is C.TypeZ.CompoundZ && type2 is C.TypeZ.CompoundZ -> {
            type1.elements.keys == type2.elements.keys && type1.elements.all {
                convZ(it.value, type2.elements[it.key]!!)
            }
        }

        else -> false
    }

    private fun convS(
        level: Int,
        value1: C.Value,
        value2: C.Value,
    ): Boolean = when {
        value1 == value2 -> true
        value1 is C.Value.TypeS && value2 is C.Value.TypeS -> true
        value1 is C.Value.EndS && value2 is C.Value.EndS -> true
        value1 is C.Value.ByteS && value2 is C.Value.ByteS -> true
        value1 is C.Value.ShortS && value2 is C.Value.ShortS -> true
        value1 is C.Value.IntS && value2 is C.Value.IntS -> true
        value1 is C.Value.LongS && value2 is C.Value.LongS -> true
        value1 is C.Value.FloatS && value2 is C.Value.FloatS -> true
        value1 is C.Value.DoubleS && value2 is C.Value.DoubleS -> true
        value1 is C.Value.StringS && value2 is C.Value.StringS -> true
        value1 is C.Value.ByteArrayS && value2 is C.Value.ByteArrayS -> true
        value1 is C.Value.IntArrayS && value2 is C.Value.IntArrayS -> true
        value1 is C.Value.LongArrayS && value2 is C.Value.LongArrayS -> true
        value1 is C.Value.ListS && value2 is C.Value.ListS -> convS(level, value1.element.value, value2.element.value)
        value1 is C.Value.CompoundS && value2 is C.Value.CompoundS -> {
            value1.elements.keys == value2.elements.keys && value1.elements.all {
                convS(level, it.value.value, value2.elements[it.key]!!.value)
            }
        }

        value1 is C.Value.ArrowS && value2 is C.Value.ArrowS -> {
            convS(level, value1.dom.value, value2.dom.value) && lazyOf(C.Value.Var(null, level, value1.dom)).let { operand ->
                convS(level.inc(), value1.cod(operand), value2.cod(operand))
            }
        }

        value1 is C.Value.TypeZ && value2 is C.Value.TypeZ -> true
        value1 is C.Value.EndTag && value2 is C.Value.EndTag -> true
        value1 is C.Value.ByteTag && value2 is C.Value.ByteTag -> value1.data == value2.data
        value1 is C.Value.ShortTag && value2 is C.Value.ShortTag -> value1.data == value2.data
        value1 is C.Value.IntTag && value2 is C.Value.IntTag -> value1.data == value2.data
        value1 is C.Value.LongTag && value2 is C.Value.LongTag -> value1.data == value2.data
        value1 is C.Value.FloatTag && value2 is C.Value.FloatTag -> value1.data == value2.data
        value1 is C.Value.DoubleTag && value2 is C.Value.DoubleTag -> value1.data == value2.data
        value1 is C.Value.StringTag && value2 is C.Value.StringTag -> value1.data == value2.data
        value1 is C.Value.ByteArrayTag && value2 is C.Value.ByteArrayTag -> {
            (value1.elements zip value2.elements).all { (element1, element2) -> convS(level, element1.value, element2.value) }
        }

        value1 is C.Value.IntArrayTag && value2 is C.Value.IntArrayTag -> {
            (value1.elements zip value2.elements).all { (element1, element2) -> convS(level, element1.value, element2.value) }
        }

        value1 is C.Value.LongArrayTag && value2 is C.Value.LongArrayTag -> {
            (value1.elements zip value2.elements).all { (element1, element2) -> convS(level, element1.value, element2.value) }
        }

        value1 is C.Value.ListTag && value2 is C.Value.ListTag -> {
            (value1.elements zip value2.elements).all { (element1, element2) -> convS(level, element1.value, element2.value) }
        }

        value1 is C.Value.CompoundTag && value2 is C.Value.CompoundTag -> {
            value1.elements.keys == value2.elements.keys && value1.elements.all {
                convS(level, it.value.value, value2.elements[it.key]!!.value)
            }
        }

        value1 is C.Value.Abs && value2 is C.Value.Abs -> {
            val operand = lazyOf(C.Value.Var(null, level, value1.anno))
            convS(level.inc(), value1.body(operand), value2.body(operand))
        }

        value1 is C.Value.Apply && value2 is C.Value.Apply -> {
            convS(level, value1.operator, value2.operator) && convS(level, value1.operand.value, value2.operand.value)
        }

        value1 is C.Value.Quote && value2 is C.Value.Quote -> false // ?
        value1 is C.Value.Var && value2 is C.Value.Var -> value1.level == value2.level
        value1 is C.Value.Hole && value2 is C.Value.Hole -> false // ?
        else -> false
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
        report: Report,
    ): C.TermZ {
        reports += report
        return C.TermZ.Hole(C.TypeZ.EndZ)
    }

    private fun errorS(
        report: Report,
    ): C.TermS {
        reports += report
        return C.TermS.Hole(TypeS.EndS)
    }

    companion object : Phase<S.Root, C.Root> {
        override operator fun invoke(
            reports: Reports,
            input: S.Root,
        ): C.Root = Elab(reports).elabRoot(input)
    }
}
