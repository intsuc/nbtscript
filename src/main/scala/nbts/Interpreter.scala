package nbts

import nbts.Ast._
import scala.Console.{CYAN, GREEN, YELLOW, MAGENTA, RESET}

def interpret(statement: Statement): Option[Int] =
  statement match
  case Statement.Insert(index, Access(tag, Some(path)), sources) =>
    tag.insert(index, path, sources())
  case Statement.Prepend(Access(tag, Some(path)), sources) =>
    tag.prepend(path, sources())
  case Statement.Append(Access(tag, Some(path)), sources) =>
    tag.append(path, sources())
  case Statement.Set(Access(tag, Some(path)), sources) =>
    tag.set(path, sources())
  case Statement.Remove(Access(tag, Some(path))) =>
    tag.remove(path)
  case Statement.Get(Access(tag, Some(path))) =>
    tag.get(path)
  case Statement.GetNumeric(Access(tag, Some(path)), scale) =>
    tag.getNumeric(path, scale)
  case Statement.Merge(Access(tag, Some(path)), source) =>
    ??? // TODO
  case Statement.Print(target) =>
    val tags = target().map(stringify)
    tags.dropRight(1).foreach(tag => print(s"$tag, "))
    tags.lastOption.map(print)
    println()
    Some(1)

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
