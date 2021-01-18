package nbts

import nbts.Ast._
import scala.collection.mutable
import scala.util.parsing.combinator._

object NbtsParsers extends RegexParsers:
  def apply(text: String): Source =
    parseAll(source, text) match
    case Success(matched, _) => matched
    case Failure(message, _) => throw Exception(message)
    case Error(message, _) => throw Exception(message)

  def source: Parser[Source] = repsep(statement, ";") ^^ { Source(_) }

  def statement: Parser[Statement]
    = "insert" ~> int ~ access ~ access ^^ { case index ~ target ~ source => Statement.Insert(index, target, source) }
    | "prepend" ~> access ~ access ^^ { Statement.Prepend(_, _) }
    | "append" ~> access ~ access ^^ { Statement.Append(_, _) }
    | "set" ~> access ~ access ^^ { Statement.Set(_, _) }
    | "remove" ~> access ^^ { Statement.Remove(_) }
    | "get" ~> access ^^ { Statement.Get(_) }
    | "get_numeric" ~> access ~ double ^^ { Statement.GetNumeric(_, _) }
    | "merge" ~> access ~ access ^^ { Statement.Merge(_, _) }
    | "print" ~> access ^^ { Statement.Print(_) }

  def access: Parser[Access]
    = (tag <~ ".") ~ path ^^ { case tag ~ path => Access(tag, Some(path)) }
    | tag ^^ { Access(_, None) }

  def tag: Parser[Tag]
    = compound
    | "[B;" ~> repsep(byte, ",") <~ "]" ^^ { data => ByteArrayTag(data.toArray) }
    | "[I;" ~> repsep(int, ",") <~ "]" ^^ { data => IntArrayTag(data.toArray) }
    | "[L;" ~> repsep(long, ",") <~ "]" ^^ { data => LongArrayTag(data.toArray) }
    | "[" ~> repsep(tag, ",") <~ "]" ^^ { data => ListTag().addAll(data) }
    | byte ^^ { ByteTag(_) }
    | short ^^ { ShortTag(_) }
    | long ^^ { LongTag(_) }
    | float ^^ { FloatTag(_) }
    | double ^^ { DoubleTag(_) }
    | int ^^ { IntTag(_) }
    | string ^^ { StringTag(_) }

  def path: Parser[Path] = repsep(node, ".") ^^ { Path(_) }

  def node: Parser[Node]
    = compound ^^ { Node.MatchRootObject(_) }
    | string ~ compound ^^ { Node.MatchObject(_, _) }
    | "[]" ^^ { _ => Node.AllElements }
    | "[" ~> compound <~ "]" ^^ { Node.MatchElement(_) }
    | "[" ~> int <~ "]" ^^ { Node.IndexedElement(_) }
    | string ^^ { Node.CompoundChild(_) }

  def string: Parser[String] = "\"" ~> """([^"]|(?<=\\)")*""".r <~ "\""
  def compound: Parser[CompoundTag] = "{" ~> repsep((string <~ ":") ~ tag, ",") <~ "}" ^^ { entries => CompoundTag(mutable.Map.empty ++ entries.map(_ -> _)) }
  def byte: Parser[Byte] = integer <~ "b" ^^ { _.toByte }
  def short: Parser[Short] = integer <~ "s" ^^ { _.toShort }
  def int: Parser[Int] = integer ^^ { _.toInt }
  def long: Parser[Long] = integer <~ "L" ^^ { _.toLong }
  def float: Parser[Float] = real <~ "f" ^^ { _.toFloat }
  def double: Parser[Double] = real <~ "d" ^^ { _.toDouble }
  def integer: Parser[String] = """[-+]?(?:0|[1-9][0-9]*)""".r
  def real: Parser[String] = """[-+]?(?:[0-9]+[.]|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?""".r
