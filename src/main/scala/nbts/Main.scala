package nbts

import java.nio.file.{Path, Paths}
import nbts.Ast.Expression
import scala.collection.mutable
import scala.io.Source
import scala.util.CommandLineParser.FromString

given FromString[Path] = Paths.get(_)

@main def run(path: Path): Unit =
  val text = Source.fromFile(path.toFile).mkString
  val source = NbtsParser(text)
  val interpreter = Interpreter()
  interpreter.interpret(source.expressions)(using Map.empty)
