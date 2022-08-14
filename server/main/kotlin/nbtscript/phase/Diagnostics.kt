package nbtscript.phase

import nbtscript.ast.Core.TermS
import nbtscript.ast.Core.TypeZ
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DiagnosticSeverity.Error
import org.eclipse.lsp4j.Range

fun charExpected(expected: Char, range: Range): Diagnostic = makeDiagnostic(
    range,
    "expected: '$expected'",
    Error,
)

fun wordExpected(range: Range): Diagnostic = makeDiagnostic(
    range,
    "expected: word",
    Error,
)

fun endOfFileExpected(range: Range): Diagnostic = makeDiagnostic(
    range,
    "expected: end of file",
    Error,
)

fun typeZExpected(range: Range): Diagnostic = makeDiagnostic(
    range,
    "expected: type₀",
    Error,
)

fun termZExpected(range: Range): Diagnostic = makeDiagnostic(
    range,
    "expected: term₀",
    Error,
)

fun termSExpected(range: Range): Diagnostic = makeDiagnostic(
    range,
    "expected: term₁",
    Error,
)

fun notFound(name: String, range: Range): Diagnostic = makeDiagnostic(
    range,
    "not found: '$name'",
    Error,
)

fun arrowExpected(actual: TermS, range: Range): Diagnostic = makeDiagnostic(
    range,
    "expected: arrow\nactual: '${stringifyTermS(actual)}'",
    Error,
)


fun codeExpected(actual: TermS, range: Range): Diagnostic = makeDiagnostic(
    range,
    "expected: code\nactual: '${stringifyTermS(actual)}'",
    Error,
)

fun typeZMismatched(expected: TypeZ, actual: TypeZ, range: Range): Diagnostic = makeDiagnostic(
    range,
    "expected: '${stringifyTypeZ(expected)}'\nactual: '${stringifyTypeZ(actual)}'",
    Error,
)

fun typeSMismatched(expected: TermS, actual: TermS, range: Range): Diagnostic = makeDiagnostic(
    range,
    "expected: '${stringifyTermS(expected)}'\nactual: '${stringifyTermS(actual)}'",
    Error,
)

private fun makeDiagnostic(
    range: Range,
    message: String,
    severity: DiagnosticSeverity,
): Diagnostic = Diagnostic(
    range,
    message,
    severity,
    "nbtscript",
)
