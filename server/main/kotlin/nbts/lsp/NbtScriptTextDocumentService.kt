package nbts.lsp

import nbts.phase.*
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.jsonrpc.messages.Either.forLeft
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
        run(text, params.position).hover?.value
    }

    override fun inlayHint(params: InlayHintParams): CompletableFuture<List<InlayHint>> = supplyAsync {
        val uri = Uri(params.textDocument.uri)
        val text = texts[uri]!!
        run(text).inlayHints.map { it.value }
    }

    override fun completion(params: CompletionParams): CompletableFuture<Either<List<CompletionItem>, CompletionList>> = supplyAsync {
        val uri = Uri(params.textDocument.uri)
        val text = texts[uri]!!
        forLeft(run(text, params.position).completionItems?.value ?: emptyList())
    }

    private fun diagnose(uri: Uri, text: String) {
        client.publishDiagnostics(PublishDiagnosticsParams(uri.value, run(text).diagnostics))
    }

    // TODO: cache
    private fun run(
        text: String,
        position: Position? = null,
    ): Phase.Context = Phase.Context(position).apply {
        (Parse..Elab..Zonk)(this, text)
    }
}
