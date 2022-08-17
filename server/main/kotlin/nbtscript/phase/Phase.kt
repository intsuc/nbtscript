package nbtscript.phase

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.util.Ranges

fun interface Phase<A, B> {
    operator fun invoke(context: Context, input: A): B

    class Context(
        private var position: Position? = null,
    ) {
        private val _diagnostics: MutableList<Diagnostic> = mutableListOf()
        private val _inlayHints: MutableList<Lazy<InlayHint>> = mutableListOf()

        var hover: Lazy<Hover>? = null
            private set
        val diagnostics: List<Diagnostic> = _diagnostics
        val inlayHints: List<Lazy<InlayHint>> = _inlayHints
        var completionItems: Lazy<List<CompletionItem>>? = null
            private set

        fun setHover(range: Range, hover: Lazy<Hover>) {
            if (this.hover == null && position != null && Ranges.containsPosition(range, position)) {
                this.hover = hover
            }
        }

        fun addDiagnostic(diagnostic: Diagnostic) {
            _diagnostics += diagnostic
        }

        fun addInlayHint(hint: Lazy<InlayHint>) {
            _inlayHints += hint
        }

        fun setCompletionItems(range: Range, items: Lazy<List<CompletionItem>>) {
            if (completionItems == null && position != null && Ranges.containsPosition(range, position)) {
                completionItems = items
            }
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun <A, B, C> Phase<A, B>.rangeTo(other: Phase<B, C>): Phase<A, C> = Phase { context, a ->
    other(context, this(context, a))
}
