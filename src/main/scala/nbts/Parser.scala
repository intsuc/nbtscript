package nbts

import nbts.Ast._
import scala.collection.mutable
import scala.util.parsing.combinator._

object NbtsParser extends RegexParsers:
  def apply(text: String): Source =
    parseAll(source, text) match
    case Success(matched, _) => matched
    case Failure(message, _) => throw Exception(message)
    case Error(message, _) => throw Exception(message)

  def source: Parser[Source] = statements ^^ { Source(_) }

  def statements: Parser[Seq[Statement]] = rep(statement <~ ";")

  def statement: Parser[Statement]
    = "insert" ~> int ~ accessor ~ accessor ^^ { case index ~ target ~ source => Statement.Insert(index, target, source) }
    | "prepend" ~> accessor ~ accessor ^^ { Statement.Prepend(_, _) }
    | "append" ~> accessor ~ accessor ^^ { Statement.Append(_, _) }
    | "set" ~> accessor ~ accessor ^^ { Statement.Set(_, _) }
    | "remove" ~> accessor ^^ { Statement.Remove(_) }
    | "get" ~> accessor ^^ { Statement.Get(_) }
    | "get_numeric" ~> accessor ~ double ^^ { Statement.GetNumeric(_, _) }
    | "merge" ~> accessor ~ accessor ^^ { Statement.Merge(_, _) }
    | "print" ~> accessor ^^ { Statement.Print(_) }
    | "function" ~> string ~ ("{" ~> statements) <~ "}" ^^ { Statement.Function(_, _) }
    | "if" ~> accessor ~ ("{" ~> statements) <~ "}" ^^ { Statement.If(_, _) }
    | string ^^ { Statement.Call(_) }

  def accessor: Parser[Accessor]
    = path ^^ { Accessor.Global(_) }
    | tag ~ path ^^ { Accessor.Local(_, _) }
    | tag ^^ { Accessor.Single(_) }

  def tag: Parser[Tag]
    = compound
    | "[B;" ~> repsep(byte, ",") <~ "]" ^^ { data => ByteArrayTag(data.toArray) }
    | "[I;" ~> repsep(int, ",") <~ "]" ^^ { data => IntArrayTag(data.toArray) }
    | "[L;" ~> repsep(long, ",") <~ "]" ^^ { data => LongArrayTag(data.toArray) }
    | "[" ~> repsep(tag, ",") <~ "]" ^^ { data => ListTag().addAll(data) }
    | "false" ^^ { _ => ByteTag.Zero }
    | "true" ^^ { _ => ByteTag.One }
    | byte ^^ { ByteTag(_) }
    | short ^^ { ShortTag(_) }
    | long ^^ { LongTag(_) }
    | float ^^ { FloatTag(_) }
    | double ^^ { DoubleTag(_) }
    | int ^^ { IntTag(_) }
    | string ^^ { StringTag(_) }

  def path: Parser[Path] = rep1("." ~> node) ^^ { Path(_) }

  def node: Parser[Node]
    = compound ^^ { Node.MatchRootObject(_) }
    | (string <~ not(' ')) ~ compound ^^ { Node.MatchObject(_, _) }
    | "[]" ^^ { _ => Node.AllElements }
    | "[" ~> compound <~ "]" ^^ { Node.MatchElement(_) }
    | "[" ~> int <~ "]" ^^ { Node.IndexedElement(_) }
    | string ^^ { Node.CompoundChild(_) }

  def string: Parser[String]
    = "\"" ~> """([^"]|(?<=\\)")*""".r <~ "\"" ^^ { _.replace("\\\"", "\"") }
    | """[^ "\[\].\{\}:;,]+""".r

  def compound: Parser[CompoundTag] = "{" ~> repsep((string <~ ":") ~ tag, ",") <~ "}" ^^ { entries => CompoundTag(mutable.Map.empty ++ entries.map(_ -> _)) }
  def byte: Parser[Byte] = integer <~ "b" ^^ { _.toByte }
  def short: Parser[Short] = integer <~ "s" ^^ { _.toShort }
  def int: Parser[Int] = integer ^^ { _.toInt }
  def long: Parser[Long] = integer <~ "L" ^^ { _.toLong }
  def float: Parser[Float] = real <~ "f" ^^ { _.toFloat }
  def double: Parser[Double] = real <~ "d" ^^ { _.toDouble }
  def integer: Parser[String] = """[-+]?[0-9]+""".r
  def real: Parser[String] = integer ~ "." ~ integer ^^ { case s ~ _ ~ b => s"$s.$b" }
