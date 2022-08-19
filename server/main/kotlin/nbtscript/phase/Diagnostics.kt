package nbtscript.phase

import nbtscript.ast.Core.TermS
import nbtscript.ast.Core.TypeZ
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DiagnosticSeverity.Error
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

fun charExpected(
    expected: Char,
    range: Range
): Diagnostic = makeDiagnostic(
    range,
    "expected: '$expected'",
    Error,
)

fun wordExpected(
    range: Range,
): Diagnostic = makeDiagnostic(
    range,
    "expected: word",
    Error,
)

fun endOfFileExpected(
    range: Range,
): Diagnostic = makeDiagnostic(
    range,
    "expected: end of file",
    Error,
)

fun typeZExpected(
    range: Range,
): Diagnostic = makeDiagnostic(
    range,
    "expected: type₀",
    Error,
)

fun termExpected(
    range: Range,
): Diagnostic = makeDiagnostic(
    range,
    "expected: term",
    Error,
)

fun termZExpected(
    range: Range,
): Diagnostic = makeDiagnostic(
    range,
    "expected: term₀",
    Error,
)

fun termSExpected(
    range: Range,
): Diagnostic = makeDiagnostic(
    range,
    "expected: term₁",
    Error,
)

fun notFound(
    name: String,
    range: Range,
): Diagnostic = makeDiagnostic(
    range,
    "not found: '$name'",
    Error,
)

fun collectionTypeExpected(
    actual: TypeZ,
    range: Range,
): Diagnostic = makeDiagnostic(
    range,
    "expected: collection type\nactual: '${stringifyTypeZ(actual)}'",
    Error,
)

fun functionTypeExpected(
    unifier: Unifier,
    actual: TermS,
    range: Range,
): Diagnostic = makeDiagnostic(
    range,
    "expected: function type\nactual: '${unifier.stringifyTermS(actual)}'",
    Error,
)

fun codeTypeExpected(
    unifier: Unifier,
    actual: TermS,
    range: Range,
): Diagnostic = makeDiagnostic(
    range,
    "expected: code type\nactual: '${unifier.stringifyTermS(actual)}'",
    Error,
)

fun typeZMismatched(
    expected: TypeZ,
    actual: TypeZ,
    range: Range,
): Diagnostic = makeDiagnostic(
    range,
    "expected: '${stringifyTypeZ(expected)}'\nactual: '${stringifyTypeZ(actual)}'",
    Error,
)

fun typeSMismatched(
    unifier: Unifier,
    expected: TermS,
    actual: TermS,
    range: Range,
): Diagnostic = makeDiagnostic(
    range,
    "expected: '${unifier.stringifyTermS(expected)}'\nactual: '${unifier.stringifyTermS(actual)}'",
    Error,
)

fun unsolvedMeta(
    index: Int,
): Diagnostic = makeDiagnostic(
    Range(Position(0, 0), Position(0, 0)), // ?
    "unsolved meta: '?${index.toSubscript()}'",
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
