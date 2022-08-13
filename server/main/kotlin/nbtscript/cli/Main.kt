package nbtscript.cli

import kotlinx.cli.ArgParser
import kotlinx.cli.ExperimentalCli

@ExperimentalCli
fun main(args: Array<String>): Unit = ArgParser("nbts").run {
    subcommands(Launch)
    parse(args)
}
