package nbtscript.phase

import kotlinx.collections.immutable.persistentListOf
import nbtscript.ast.Core.Kind.Syn
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.MarkupKind
import nbtscript.ast.Core as C
import nbtscript.ast.Staged as S

fun markup(
    value: String,
): MarkupContent = MarkupContent(
    MarkupKind.MARKDOWN,
    "```nbtscript\n$value\n```"
)

fun Unifier.stringifyTypeZ(
    type: C.TypeZ<Syn>,
): String = when (type) {
    is C.TypeZ.EndType -> "end"
    is C.TypeZ.ByteType -> "byte"
    is C.TypeZ.ShortType -> "short"
    is C.TypeZ.IntType -> "int"
    is C.TypeZ.LongType -> "long"
    is C.TypeZ.FloatType -> "float"
    is C.TypeZ.DoubleType -> "double"
    is C.TypeZ.StringType -> "string"
    is C.TypeZ.ByteArrayType -> "byte_array"
    is C.TypeZ.IntArrayType -> "int_array"
    is C.TypeZ.LongArrayType -> "long_array"
    is C.TypeZ.ListType -> "list ${stringifyTypeZ(type.element)}"
    is C.TypeZ.CollectionType -> "collection ${stringifyTypeZ(type.element)}"
    is C.TypeZ.CompoundType -> type.elements.entries.joinToString(", ", "compound {", "}") { "${it.key}: ${stringifyTypeZ(it.value)}" }
    is C.TypeZ.Splice -> "$${stringifyTermS(type.element)}"
    is C.TypeZ.Hole -> " "
}

fun Unifier.stringifyTermZ(
    term: C.TermZ,
): String = when (term) {
    is C.TermZ.ByteTag -> "${term.data}b"
    is C.TermZ.ShortTag -> "${term.data}s"
    is C.TermZ.IntTag -> "${term.data}"
    is C.TermZ.LongTag -> "${term.data}L"
    is C.TermZ.FloatTag -> "${term.data}f"
    is C.TermZ.DoubleTag -> "${term.data}d"
    is C.TermZ.StringTag -> term.data.quoted('"')
    is C.TermZ.ByteArrayTag -> term.elements.joinToString(", ", "[B;", "]") { stringifyTermZ(it) }
    is C.TermZ.IntArrayTag -> term.elements.joinToString(", ", "[I;", "]") { stringifyTermZ(it) }
    is C.TermZ.LongArrayTag -> term.elements.joinToString(", ", "[L;", "]") { stringifyTermZ(it) }
    is C.TermZ.ListTag -> term.elements.joinToString(", ", "[", "]") { stringifyTermZ(it) }
    is C.TermZ.CompoundTag -> term.elements.entries.joinToString(", ", "{", "}") { "${it.key}: ${stringifyTermZ(it.value)}" }
    is C.TermZ.Splice -> "$${stringifyTermS(term.element)}"
    is C.TermZ.Function -> "function ${term.name} = ${stringifyTermZ(term.body)};\n${stringifyTermZ(term.next)}"
    is C.TermZ.Run -> term.name
    is C.TermZ.Hole -> " "
}

fun Unifier.stringifyTermS(
    term: C.TermS<Syn>,
): String = when (term) {
    is C.TermS.UniverseType -> "universe"
    is C.TermS.EndType -> "end"
    is C.TermS.ByteType -> "byte"
    is C.TermS.ShortType -> "short"
    is C.TermS.IntType -> "int"
    is C.TermS.LongType -> "long"
    is C.TermS.FloatType -> "float"
    is C.TermS.DoubleType -> "double"
    is C.TermS.StringType -> "string"
    is C.TermS.ByteArrayType -> "byte_array"
    is C.TermS.IntArrayType -> "int_array"
    is C.TermS.LongArrayType -> "long_array"
    is C.TermS.ListType -> "list ${stringifyTermS(term.element)}"
    is C.TermS.CompoundType -> term.elements.entries.joinToString(", ", "compound {", "}") { "${it.key}: ${stringifyTermS(it.value)}" }
    is C.TermS.IndexedElement -> "${stringifyTermZ(term.target)}.[${stringifyTermS(term.index)}]"
    is C.TermS.FunctionType -> "${term.name?.let { "($it: ${stringifyTermS(term.dom)})" } ?: stringifyTermS(term.dom)} -> ${stringifyTermS(term.cod)}"
    is C.TermS.CodeType -> "code ${stringifyTypeZ(term.element)}"
    is C.TermS.TypeType -> "type"
    is C.TermS.EndTag -> ""
    is C.TermS.ByteTag -> "${term.data}b"
    is C.TermS.ShortTag -> "${term.data}s"
    is C.TermS.IntTag -> "${term.data}"
    is C.TermS.LongTag -> "${term.data}L"
    is C.TermS.FloatTag -> "${term.data}f"
    is C.TermS.DoubleTag -> "${term.data}d"
    is C.TermS.StringTag -> term.data.quoted('"')
    is C.TermS.ByteArrayTag -> term.elements.joinToString(", ", "[B;", "]") { stringifyTermS(it) }
    is C.TermS.IntArrayTag -> term.elements.joinToString(", ", "[I;", "]") { stringifyTermS(it) }
    is C.TermS.LongArrayTag -> term.elements.joinToString(", ", "[L;", "]") { stringifyTermS(it) }
    is C.TermS.ListTag -> term.elements.joinToString(", ", "[", "]") { stringifyTermS(it) }
    is C.TermS.CompoundTag -> term.elements.entries.joinToString(", ", "{", "}") { "${it.key}: ${stringifyTermS(it.value)}" }
    is C.TermS.Abs -> "${term.name} => ${stringifyTermS(term.body)}"
    is C.TermS.Apply -> "${stringifyTermS(term.operator)}(${stringifyTermS(term.operand)})"
    is C.TermS.QuoteType -> "`${stringifyTypeZ(term.element)}"
    is C.TermS.QuoteTerm -> "`${stringifyTermZ(term.element)}"
    is C.TermS.Let -> "let ${term.name} = ${stringifyTermS(term.init)};\n${stringifyTermS(term.next)}"
    is C.TermS.Var -> term.name ?: ""
    is C.TermS.Meta -> this[term.index]?.let { stringifyTermS(reifyTermS(persistentListOf(), it)) } ?: "?${term.index.toSubscript()}"
    is C.TermS.Hole -> " "
    else -> unreachable()
}

fun stringifyTerm(
    term: S.Term,
): String = when (term) {
    is S.Term.ByteTag -> "${term.data}b"
    is S.Term.ShortTag -> "${term.data}s"
    is S.Term.IntTag -> "${term.data}"
    is S.Term.LongTag -> "${term.data}L"
    is S.Term.FloatTag -> "${term.data}f"
    is S.Term.DoubleTag -> "${term.data}d"
    is S.Term.StringTag -> term.data.quoted('"')
    is S.Term.ByteArrayTag -> term.elements.joinToString(", ", "[B;", "]") { stringifyTerm(it) }
    is S.Term.IntArrayTag -> term.elements.joinToString(", ", "[I;", "]") { stringifyTerm(it) }
    is S.Term.LongArrayTag -> term.elements.joinToString(", ", "[L;", "]") { stringifyTerm(it) }
    is S.Term.ListTag -> term.elements.joinToString(", ", "[", "]") { stringifyTerm(it) }
    is S.Term.CompoundTag -> term.elements.entries.joinToString(", ", "{", "}") { "${it.key}: ${stringifyTerm(it.value)}" }
    is S.Term.IndexedElement -> "${stringifyTerm(term.target)}.[${term.index}]"
    is S.Term.Function -> "function ${term.name} = ${stringifyTerm(term.body)};\n${stringifyTerm(term.next)}"
    is S.Term.Run -> term.name
    is S.Term.Hole -> " "
}

fun String.quoted(
    quote: Char,
): String =
    "$quote${
        this
            .replace("\\", "\\\\")
            .replace("$quote", "\\$quote")
    }$quote"

fun Int.toSubscript(): String =
    this
        .toString()
        .map { it + ('₀' - '0') }
        .joinToString("")
