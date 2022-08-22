package nbts.cli

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import nbts.phase.*
import nbts.phase.Exec
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

@ExperimentalCli
object Exec : Subcommand("exec", "Execute") {
    private val path: String by argument(ArgType.String, "path")

    override fun execute() {
        try {
            val text = Files.readString(Paths.get(path))
            val result = stringifyTerm(Exec((Parse..Elab..Zonk..Stage)(Phase.Context(), text)).body)
            println(result)
        } catch (_: IOException) {
            throw Error("not found: '$path'")
        }
    }
}
