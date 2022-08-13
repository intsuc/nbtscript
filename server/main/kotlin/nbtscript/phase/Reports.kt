package nbtscript.phase

import org.eclipse.lsp4j.Diagnostic

@JvmInline
value class Reports(
    private val reports: MutableList<Report> = mutableListOf(),
) {
    val diagnostics: List<Diagnostic> get() = reports.map { it.toDiagnostic() }

    operator fun plusAssign(report: Report) {
        reports += report
    }
}
