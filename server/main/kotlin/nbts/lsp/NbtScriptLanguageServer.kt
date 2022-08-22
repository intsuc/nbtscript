package nbts.lsp

import org.eclipse.lsp4j.*
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
                hoverProvider = forLeft(true)
                inlayHintProvider = forLeft(true)
                completionProvider = CompletionOptions(false, emptyList())
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
