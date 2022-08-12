package nbtscript.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.TextDocumentService

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
        client.showMessage(MessageParams(MessageType.Info, uri.uri))
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        val uri = Uri(params.textDocument.uri)
        val text = params.contentChanges.last().text
        texts[uri] = text
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        val uri = Uri(params.textDocument.uri)
        texts -= uri
    }

    override fun didSave(params: DidSaveTextDocumentParams): Unit = Unit
}
