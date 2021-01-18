package nbts

import java.nio.file.{Path, Paths}
import scala.io.Source
import scala.util.CommandLineParser.FromString

given FromString[Path] = Paths.get(_)

@main def run(path: Path): Unit =
  val text = Source.fromFile(path.toFile).mkString
  val source = NbtsParser(text)
  source.statements.foreach(interpret)
