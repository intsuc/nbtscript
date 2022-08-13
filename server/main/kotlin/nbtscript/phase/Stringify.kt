package nbtscript.phase

import nbtscript.ast.Core.TypeZ
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.MarkupKind

fun stringifyTypeZ(type: TypeZ): String = when (type) {
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
    is TypeZ.CompoundZ -> "compound {${type.elements.entries.joinToString(", ") { "${it.key}: ${stringifyTypeZ(it.value)}" }}}"
    is TypeZ.Hole -> "hole"
}

fun markup(value: String): MarkupContent = MarkupContent(
    MarkupKind.MARKDOWN,
    "```nbtscript\n$value\n```"
)
