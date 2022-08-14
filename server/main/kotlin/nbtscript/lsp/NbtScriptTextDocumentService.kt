package nbtscript.lsp

import nbtscript.phase.Elab
import nbtscript.phase.Parse
import nbtscript.phase.Phase
import nbtscript.phase.rangeTo
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.TextDocumentService
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.supplyAsync

class NbtScriptTextDocumentService : TextDocumentService, LanguageClientAware {
    private lateinit var client: LanguageClient
    private val texts: MutableMap<Uri, String> = mutableMapOf()

    override fun connect(client: LanguageClient) {
        this.client = client
    }

    override fun didOpen(params: DidOpenTextDocumentParams) {
        val uri = Uri(params.textDocument.uri)
        val text = params.textDocument.text
        texts[uri] = text
        diagnose(uri, text)
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        val uri = Uri(params.textDocument.uri)
        val text = params.contentChanges.last().text
        texts[uri] = text
        diagnose(uri, text)
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        val uri = Uri(params.textDocument.uri)
        texts -= uri
    }

    override fun didSave(params: DidSaveTextDocumentParams): Unit = Unit

    override fun hover(params: HoverParams): CompletableFuture<Hover> = supplyAsync {
        val uri = Uri(params.textDocument.uri)
        val text = texts[uri]!!
        Phase.Context(params.position).apply {
            (Parse..Elab)(this, text)
        }.hover?.value
    }

    override fun inlayHint(params: InlayHintParams): CompletableFuture<List<InlayHint>> = supplyAsync {
        val uri = Uri(params.textDocument.uri)
        val text = texts[uri]!!
        run(text).inlayHints
    }

    private fun diagnose(uri: Uri, text: String) {
        client.publishDiagnostics(PublishDiagnosticsParams(uri.value, run(text).diagnostics))
    }

    // TODO: cache
    private fun run(text: String): Phase.Context = Phase.Context().apply {
        (Parse..Elab)(this, text)
    }
}
