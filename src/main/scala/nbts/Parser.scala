package nbts

import nbts.Ast._
import scala.collection.mutable
import scala.util.parsing.combinator._

object NbtsParser extends RegexParsers with PackratParsers:
  def apply(text: String): Source =
    parseAll(source, text) match
    case Success(matched, _) => matched
    case Failure(message, _) => throw Exception(message)
    case Error(message, _) => throw Exception(message)

  def source: Parser[Source] = expressions ^^ { Source(_) }

  def expressions: Parser[Seq[Expression]] = rep(expression)

  lazy val expression: PackratParser[Expression]
    = "insert" ~> int ~ accessor ~ expression ^^ { case index ~ target ~ source => Expression.Insert(index, target, source) }
    | "prepend" ~> accessor ~ expression ^^ { Expression.Insert(0, _, _) }
    | "append" ~> accessor ~ expression ^^ { Expression.Insert(-1, _, _) }
    | "set" ~> accessor ~ expression ^^ { Expression.Set(_, _) }
    | "remove" ~> accessor ^^ { Expression.Remove(_) }
    | "get" ~> expression ^^ { Expression.Get(_) }
    | "get_numeric" ~> expression ~ double ^^ { Expression.GetNumeric(_, _) }
    | "merge" ~> accessor ~ expression ^^ { Expression.Merge(_, _) }
    | "print" ~> expression ^^ { Expression.Print(_) }
    | "function" ~> string ~ expressions ^^ { Expression.Function(_, _) }
    | "run" ~> string ^^ { Expression.Run(_) }
    | "if" ~> expression ~ expressions^^ { Expression.If(_, _) }
    | "unless" ~> expression ~ expressions ^^ { Expression.Unless(_, _) }
    | (expression <~ "matches") ~ (int <~ "..") ~ int ^^ { case target ~ min ~ max => Expression.Matches(target, min, max) }
    | (expression <~ "to_byte") ~ double ^^ { Expression.To(_, Type.Byte, _) }
    | (expression <~ "to_short") ~ double ^^ { Expression.To(_, Type.Short, _) }
    | (expression <~ "to_int") ~ double ^^ { Expression.To(_, Type.Int, _) }
    | (expression <~ "to_long") ~ double ^^ { Expression.To(_, Type.Long, _) }
    | (expression <~ "to_float") ~ double ^^ { Expression.To(_, Type.Float, _) }
    | (expression <~ "to_double") ~ double ^^ { Expression.To(_, Type.Double, _) }
    | "random" ~> float ^^ { Expression.Random(_) }
    | expression ~ operator ~ expression ^^ { case left ~ operator ~ right => Expression.Operate(left, operator, right) }
    | "(" ~> expression <~ ")"
    | accessor ^^ { Expression.Access(_) }

  def accessor: Parser[Accessor]
    = path ^^ { Accessor.Global(_) }
    | (tag <~ not(' ')) ~ path ^^ { Accessor.Local(_, _) }
    | tag ^^ { Accessor.Single(_) }

  def operator: Parser[Operator]
    = "+" ^^ { _ => Operator.+ }
    | "-" ^^ { _ => Operator.- }
    | "*" ^^ { _ => Operator.* }
    | "/" ^^ { _ => Operator./ }
    | "%" ^^ { _ => Operator.% }
    | "=" ^^ { _ => Operator.`=` }
    | "<=" ^^ { _ => Operator.<= }
    | "<" ^^ { _ => Operator.< }
    | ">=" ^^ { _ => Operator.>= }
    | ">" ^^ { _ => Operator.> }

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

  def path: Parser[Path] = ("." ~> node) ~ rep(not(' ') ~> "." ~> node) ^^ { case head ~ tail => Path(head +: tail) }

  def node: Parser[Node]
    = compound ^^ { Node.MatchRootObject(_) }
    | (string <~ not(' ')) ~ compound ^^ { Node.MatchObject(_, _) }
    | "[]" ^^ { _ => Node.AllElements }
    | "[" ~> compound <~ "]" ^^ { Node.MatchElement(_) }
    | "[" ~> int <~ "]" ^^ { Node.IndexedElement(_) }
    | string ^^ { Node.CompoundChild(_) }

  def string: Parser[String]
    = "\"" ~> """([^"]|(?<=\\)")*""".r <~ "\"" ^^ { _.replace("\\\"", "\"") }
    | """[^\s"()\[\].\{\}:;,]+""".r

  def compound: Parser[CompoundTag] = "{" ~> repsep((string <~ ":") ~ tag, ",") <~ "}" ^^ { entries => CompoundTag(mutable.Map.empty ++ entries.map(_ -> _)) }
  def byte: Parser[Byte] = integer <~ "b" ^^ { _.toByte }
  def short: Parser[Short] = integer <~ "s" ^^ { _.toShort }
  def int: Parser[Int] = integer ^^ { _.toInt }
  def long: Parser[Long] = integer <~ "L" ^^ { _.toLong }
  def float: Parser[Float] = real <~ "f" ^^ { _.toFloat }
  def double: Parser[Double] = real <~ "d" ^^ { _.toDouble }
  def integer: Parser[String] = """[-+]?[0-9]+""".r
  def real: Parser[String] = """[-+]?([0-9]*\.[0-9]+)(e[-+]?[0-9]+)?""".r
