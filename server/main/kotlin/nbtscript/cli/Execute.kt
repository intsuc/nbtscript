package nbtscript.cli

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import nbtscript.phase.*
import nbtscript.phase.Execute
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

@ExperimentalCli
object Execute : Subcommand("execute", "Execute") {
    private val path: String by argument(ArgType.String, "path")

    override fun execute() {
        try {
            val text = Files.readString(Paths.get(path))
            val result = stringifyTerm(Execute((Parse..Elab..Zonk..Stage)(Phase.Context(), text)).body)
            println(result)
        } catch (_: IOException) {
            throw Error("not found: '$path'")
        }
    }
}
