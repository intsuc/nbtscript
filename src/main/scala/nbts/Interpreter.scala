package nbts

import nbts.Ast._
import scala.Console.{CYAN, GREEN, YELLOW, MAGENTA, RESET}
import scala.collection.mutable

def interpret(statement: Statement)(using context: mutable.Map[String, Seq[Statement]]): Option[Int] =
  statement match
  case Statement.Insert(index, Access(tag, Some(path)), sources) =>
    tag.insert(index, path, sources())
  case Statement.Prepend(Access(tag, Some(path)), sources) =>
    tag.insert(0, path, sources())
  case Statement.Append(Access(tag, Some(path)), sources) =>
    tag.insert(-1, path, sources())
  case Statement.Set(Access(tag, Some(path)), sources) =>
    path.set(tag, sources().last) match
    case 0 => None
    case result => Some(result)
  case Statement.Remove(Access(tag, Some(path))) =>
    path.remove(tag) match
    case 0 => None
    case result => Some(result)
  case Statement.Get(Access(tag, Some(path))) =>
    path.get(tag) match
    case Seq(target) =>
      target match
      case target: NumericTag => Some(DoubleTag.floor(target.asDouble))
      case target: CollectionTag[?] => Some(target.size)
      case target: CompoundTag => Some(target.size)
      case target: StringTag => Some(target.size)
      case _ => None
    case _ => None
  case Statement.GetNumeric(Access(tag, Some(path)), scale) =>
    path.get(tag) match
    case Seq(target) =>
      target match
      case target: NumericTag => Some(DoubleTag.floor(target.asDouble * scale))
      case _ => None
    case _ => None
  case Statement.Merge(Access(tag, Some(path)), source) =>
    ??? // TODO
  case Statement.Print(target) =>
    val tags = target().map(stringify)
    tags.dropRight(1).foreach(tag => print(s"$tag, "))
    tags.lastOption.map(print)
    println()
    Some(1)
  case Statement.Function(name, body) =>
    context(name) = body
    Some(1)
  case Statement.Call(name) =>
    context.get(name) match
    case Some(body) => body.foreach(interpret); Some(1)
    case None => None

extension (target: Tag) def insert(index: Int, path: Path, sources: Seq[Tag]): Option[Int] =
  // TODO: rewrite more declaratively
  var result = 0
  val targets = path.getOrCreate(target, ListTag())
  for target <- targets do
    target match
    case target: CollectionTag[?] =>
      var inserted = false
      var normalized = if index < 0 then target.size + index + 1 else index
      for source <- sources do
        try if target.add(normalized, source.copy()) then
          normalized += 1
          inserted = true
        catch case _: IndexOutOfBoundsException => return None
      result += (if inserted then 1 else 0)
    case _ => return None
  Some(result)

extension (access: Access) def apply(): Seq[Tag] =
  access match
  case Access(tag, Some(path)) => path.get(tag)
  case Access(tag, None) => Seq(tag)

def stringify(tag: Tag): String =
  tag match
  case EndTag => ""
  case StringTag(data) => quote(data, GREEN)
  case CompoundTag(data) => data.map((name, tag) => s"""${quote(name, CYAN)}: ${stringify(tag)}""").mkString("{", ", ", "}")
  case ByteTag(data) => s"$YELLOW$data${MAGENTA}b$RESET"
  case ShortTag(data) => s"$YELLOW$data${MAGENTA}s$RESET"
  case IntTag(data) => s"$YELLOW$data$RESET"
  case LongTag(data) => s"$YELLOW$data${MAGENTA}L$RESET"
  case FloatTag(data) => s"$YELLOW$data${MAGENTA}f$RESET"
  case DoubleTag(data) => s"$YELLOW$data${MAGENTA}d$RESET"
  case ByteArrayTag(data) => data.map(YELLOW + _.toString + s"${MAGENTA}b$RESET").mkString(s"[${MAGENTA}B$RESET; ", ", ", "]")
  case IntArrayTag(data) => data.map(YELLOW + _.toString).mkString(s"[${MAGENTA}I$RESET; ", ", ", "]")
  case LongArrayTag(data) => data.map(YELLOW + _.toString + s"${MAGENTA}L$RESET").mkString(s"[${MAGENTA}L$RESET; ", ", ", "]")
  case ListTag(data, _) => data.map(stringify).mkString("[", ", ", "]")

def quote(string: String, color: String): String =
  if """[^ "\[\].\{\}:;,]+""".r.matches(string) then
    s"$color$string$RESET"
  else
    s""""$color${string.replace("\"", "\\\"")}$RESET""""
