package nbtscript.phase

import nbtscript.ast.Core.TermS
import nbtscript.ast.Core.TypeZ
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DiagnosticSeverity.Error
import org.eclipse.lsp4j.Range

sealed class Report(
    private val severity: DiagnosticSeverity,
) {
    abstract val range: Range
    abstract val message: String
    fun toDiagnostic(): Diagnostic = Diagnostic(range, message, severity, "nbtscript")

    data class CharExpected(val expected: Char, override val range: Range) : Report(Error) {
        override val message: String get() = "expected: '$expected'"
    }

    data class WordExpected(override val range: Range) : Report(Error) {
        override val message: String get() = "expected: word"
    }

    data class EndOfFileExpected(override val range: Range) : Report(Error) {
        override val message: String get() = "expected: end of file"
    }

    data class TypeZExpected(override val range: Range) : Report(Error) {
        override val message: String get() = "expected: type₀"
    }

    data class TermZExpected(override val range: Range) : Report(Error) {
        override val message: String get() = "expected: term₀"
    }

    data class TermSExpected(override val range: Range) : Report(Error) {
        override val message: String get() = "expected: term₁"
    }

    data class NotFound(val name: String, override val range: Range) : Report(Error) {
        override val message: String get() = "not found: '$name'"
    }

    data class ArrowExpected(val actual: TermS, override val range: Range) : Report(Error) {
        override val message: String get() = "expected: arrow\nactual: '${stringifyTermS(actual)}'"
    }

    data class CodeExpected(val actual: TermS, override val range: Range) : Report(Error) {
        override val message: String get() = "expected: code\nactual: '${stringifyTermS(actual)}'"
    }

    data class TypeZMismatched(val expected: TypeZ, val actual: TypeZ, override val range: Range) : Report(Error) {
        override val message: String get() = "expected: '${stringifyTypeZ(expected)}'\nactual: '${stringifyTypeZ(actual)}'"
    }

    data class TypeSMismatched(val expected: TermS, val actual: TermS, override val range: Range) : Report(Error) {
        override val message: String get() = "expected: '${stringifyTermS(expected)}'\nactual: '${stringifyTermS(actual)}'"
    }
}
