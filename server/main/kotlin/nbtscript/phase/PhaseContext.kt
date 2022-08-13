package nbtscript.phase

import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.InlayHint

class PhaseContext {
    private val reports: MutableList<Report> = mutableListOf()
    private val _inlayHints: MutableList<InlayHint> = mutableListOf()

    val diagnostics: List<Diagnostic> get() = reports.map { it.toDiagnostic() }
    val inlayHints: List<InlayHint> = _inlayHints

    fun addReport(report: Report) {
        reports += report
    }

    fun addInlayHint(hint: InlayHint) {
        _inlayHints += hint
    }
}
