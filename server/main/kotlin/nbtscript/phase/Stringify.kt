package nbtscript.phase

import nbtscript.ast.Core.*
import nbtscript.ast.Staged.Term
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.MarkupKind

fun markup(
    value: String,
): MarkupContent = MarkupContent(
    MarkupKind.MARKDOWN,
    "```nbtscript\n$value\n```"
)

fun stringifyTypeZ(
    type: TypeZ,
): String = when (type) {
    is TypeZ.EndZ -> "end"
    is TypeZ.ByteZ -> "byte"
    is TypeZ.ShortZ -> "short"
    is TypeZ.IntZ -> "int"
    is TypeZ.LongZ -> "long"
    is TypeZ.FloatZ -> "float"
    is TypeZ.DoubleZ -> "double"
    is TypeZ.StringZ -> "string"
    is TypeZ.ByteArrayZ -> "byte_array"
    is TypeZ.IntArrayZ -> "int_array"
    is TypeZ.LongArrayZ -> "long_array"
    is TypeZ.ListZ -> "list ${stringifyTypeZ(type.element)}"
    is TypeZ.CompoundZ -> type.elements.entries.joinToString(", ", "compound {", "}") { "${it.key}: ${stringifyTypeZ(it.value)}" }
    is TypeZ.Hole -> "hole"
}

fun stringifyTermZ(
    term: TermZ,
): String = when (term) {
    is TermZ.ByteTag -> "${term.data}b"
    is TermZ.ShortTag -> "${term.data}s"
    is TermZ.IntTag -> "${term.data}"
    is TermZ.LongTag -> "${term.data}L"
    is TermZ.FloatTag -> "${term.data}f"
    is TermZ.DoubleTag -> "${term.data}d"
    is TermZ.StringTag -> term.data.quoted('"')
    is TermZ.ByteArrayTag -> term.elements.joinToString(", ", "[B;", "]") { stringifyTermZ(it) }
    is TermZ.IntArrayTag -> term.elements.joinToString(", ", "[I;", "]") { stringifyTermZ(it) }
    is TermZ.LongArrayTag -> term.elements.joinToString(", ", "[L;", "]") { stringifyTermZ(it) }
    is TermZ.ListTag -> term.elements.joinToString(", ", "[", "]") { stringifyTermZ(it) }
    is TermZ.CompoundTag -> term.elements.entries.joinToString(", ", "{", "}") { "${it.key}: ${stringifyTermZ(it.value)}" }
    is TermZ.Splice -> "$${stringifyTermS(term.element)}"
    is TermZ.Function -> "function ${term.name} = ${stringifyTermZ(term.body)};\n${stringifyTermZ(term.next)}"
    is TermZ.Run -> term.name
    is TermZ.Hole -> " "
}

fun stringifyTermS(
    term: TermS,
): String = when (term) {
    is TermS.UniverseS -> "universe"
    is TermS.EndS -> "end"
    is TermS.ByteS -> "byte"
    is TermS.ShortS -> "short"
    is TermS.IntS -> "int"
    is TermS.LongS -> "long"
    is TermS.FloatS -> "float"
    is TermS.DoubleS -> "double"
    is TermS.StringS -> "string"
    is TermS.ByteArrayS -> "byte_array"
    is TermS.IntArrayS -> "int_array"
    is TermS.LongArrayS -> "long_array"
    is TermS.ListS -> "list ${stringifyTermS(term.element)}"
    is TermS.CompoundS -> term.elements.entries.joinToString(", ", "compound {", "}") { "${it.key}: ${stringifyTermS(it.value)}" }
    is TermS.IndexedElement -> "${stringifyTermZ(term.target)}.[${stringifyTermS(term.index)}]"
    is TermS.FunctionS -> "${term.name?.let { "($it: ${stringifyTermS(term.dom)})" } ?: stringifyTermS(term.dom)} -> ${stringifyTermS(term.cod)}"
    is TermS.CodeS -> "code ${stringifyTypeZ(term.element)}"
    is TermS.TypeZ -> "type"
    is TermS.EndTag -> ""
    is TermS.ByteTag -> "${term.data}b"
    is TermS.ShortTag -> "${term.data}s"
    is TermS.IntTag -> "${term.data}"
    is TermS.LongTag -> "${term.data}L"
    is TermS.FloatTag -> "${term.data}f"
    is TermS.DoubleTag -> "${term.data}d"
    is TermS.StringTag -> term.data.quoted('"')
    is TermS.ByteArrayTag -> term.elements.joinToString(", ", "[B;", "]") { stringifyTermS(it) }
    is TermS.IntArrayTag -> term.elements.joinToString(", ", "[I;", "]") { stringifyTermS(it) }
    is TermS.LongArrayTag -> term.elements.joinToString(", ", "[L;", "]") { stringifyTermS(it) }
    is TermS.ListTag -> term.elements.joinToString(", ", "[", "]") { stringifyTermS(it) }
    is TermS.CompoundTag -> term.elements.entries.joinToString(", ", "{", "}") { "${it.key}: ${stringifyTermS(it.value)}" }
    is TermS.Abs -> "${term.name} => ${stringifyTermS(term.body)}"
    is TermS.Apply -> "${stringifyTermS(term.operator)}(${stringifyTermS(term.operand)})"
    is TermS.Quote -> "`${stringifyTermZ(term.element)}"
    is TermS.Let -> "let ${term.name} = ${stringifyTermS(term.init)};\n${stringifyTermS(term.next)}"
    is TermS.Var -> term.name ?: ""
    is TermS.Meta -> "?${term.index.toSubscript()}"
    is TermS.Hole -> " "
}

fun stringifyTerm(
    term: Term,
): String = when (term) {
    is Term.ByteTag -> "${term.data}b"
    is Term.ShortTag -> "${term.data}s"
    is Term.IntTag -> "${term.data}"
    is Term.LongTag -> "${term.data}L"
    is Term.FloatTag -> "${term.data}f"
    is Term.DoubleTag -> "${term.data}d"
    is Term.StringTag -> term.data.quoted('"')
    is Term.ByteArrayTag -> term.elements.joinToString(", ", "[B;", "]") { stringifyTerm(it) }
    is Term.IntArrayTag -> term.elements.joinToString(", ", "[I;", "]") { stringifyTerm(it) }
    is Term.LongArrayTag -> term.elements.joinToString(", ", "[L;", "]") { stringifyTerm(it) }
    is Term.ListTag -> term.elements.joinToString(", ", "[", "]") { stringifyTerm(it) }
    is Term.CompoundTag -> term.elements.entries.joinToString(", ", "{", "}") { "${it.key}: ${stringifyTerm(it.value)}" }
    is Term.IndexedElement -> "${stringifyTerm(term.target)}[${term.index}]"
    is Term.Function -> "function ${term.name} = ${stringifyTerm(term.body)};\n${stringifyTerm(term.next)}"
    is Term.Run -> term.name
    is Term.Hole -> " "
}

fun String.quoted(
    quote: Char,
): String =
    "$quote${
        this
            .replace("\\", "\\\\")
            .replace("$quote", "\\$quote")
    }$quote"

fun Int.toSubscript(): String = toString().map { it + ('₀' - '0') }.joinToString("")
