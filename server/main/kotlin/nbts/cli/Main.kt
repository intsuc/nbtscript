package nbts.cli

import kotlinx.cli.ArgParser
import kotlinx.cli.ExperimentalCli
import kotlin.system.exitProcess

@ExperimentalCli
fun main(args: Array<String>): Unit = ArgParser("nbts").run {
    subcommands(
        Exec,
        Launch,
    )
    try {
        parse(args)
    } catch (t: Throwable) {
        System.err.println(t.message)
        exitProcess(1)
    }
}
