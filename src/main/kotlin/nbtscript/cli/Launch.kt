package nbtscript.cli

import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import nbtscript.lsp.NbtScriptLanguageServer
import org.eclipse.lsp4j.launch.LSPLauncher

@ExperimentalCli
object Launch : Subcommand("launch", "Launch language server") {
    override fun execute() {
        val server = NbtScriptLanguageServer()
        val launcher = LSPLauncher.createServerLauncher(server, System.`in`, System.out)
        val client = launcher.remoteProxy
        server.connect(client)
        launcher.startListening()
    }
}
