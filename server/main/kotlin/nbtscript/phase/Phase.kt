package nbtscript.phase

import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.InlayHint

fun interface Phase<A, B> {
    operator fun invoke(context: Context, input: A): B

    class Context {
        private val _diagnostics: MutableList<Diagnostic> = mutableListOf()
        private val _inlayHints: MutableList<InlayHint> = mutableListOf()

        val diagnostics: List<Diagnostic> = _diagnostics
        val inlayHints: List<InlayHint> = _inlayHints

        fun addDiagnostic(diagnostic: Diagnostic) {
            _diagnostics += diagnostic
        }

        fun addInlayHint(hint: InlayHint) {
            _inlayHints += hint
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun <A, B, C> Phase<A, B>.rangeTo(other: Phase<B, C>): Phase<A, C> = Phase { context, a ->
    other(context, this(context, a))
}
