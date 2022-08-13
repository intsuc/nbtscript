package nbtscript.phase

import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DiagnosticSeverity.Error
import org.eclipse.lsp4j.Range
import nbtscript.ast.Core as C
import nbtscript.ast.Core.Value as TypeS

sealed class Report(
    private val severity: DiagnosticSeverity,
) {
    abstract val range: Range
    abstract val message: String
    fun toDiagnostic(): Diagnostic = Diagnostic(range, message, severity, "nbtscript")

    data class NotFound(val name: String, override val range: Range) : Report(Error) {
        override val message: String get() = "not found: '$name'"
    }

    data class ArrowExpected(val actual: C.Value, override val range: Range) : Report(Error) {
        override val message: String get() = "expected: arrow\nactual: '$actual'"
    }

    data class CodeExpected(val actual: C.Value, override val range: Range) : Report(Error) {
        override val message: String get() = "expected: code\nactual: '$actual'"
    }

    data class TypeZMismatched(val expected: C.TypeZ, val actual: C.TypeZ, override val range: Range) : Report(Error) {
        override val message: String get() = "expected: '$expected'\nactual: '$actual'"
    }

    data class TypeSMismatched(val expected: TypeS, val actual: TypeS, override val range: Range) : Report(Error) {
        override val message: String get() = "expected: '$expected'\nactual: '$actual'"
    }
}
