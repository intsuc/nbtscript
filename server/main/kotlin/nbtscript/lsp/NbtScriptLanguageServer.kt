package nbtscript.lsp

import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.jsonrpc.messages.Either.forLeft
import org.eclipse.lsp4j.services.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

class NbtScriptLanguageServer : LanguageServer, LanguageClientAware {
    private val notebooks: NbtScriptNotebookDocumentService = NbtScriptNotebookDocumentService()
    private val documents: NbtScriptTextDocumentService = NbtScriptTextDocumentService()
    private val workspaces: NbtScriptWorkspaceService = NbtScriptWorkspaceService()

    override fun connect(client: LanguageClient) {
        documents.connect(client)
    }

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        val result = InitializeResult().apply {
            capabilities = ServerCapabilities().apply {
                textDocumentSync = forLeft(TextDocumentSyncKind.Full)
                inlayHintProvider = forLeft(true)
            }
        }
        return completedFuture(result)
    }

    override fun shutdown(): CompletableFuture<Any> = completedFuture(null)

    override fun exit(): Unit = Unit

    override fun getNotebookDocumentService(): NotebookDocumentService = notebooks

    override fun getTextDocumentService(): TextDocumentService = documents

    override fun getWorkspaceService(): WorkspaceService = workspaces
}
