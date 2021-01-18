package nbts

import nbts.Ast._
import scala.Console.{CYAN, GREEN, YELLOW, MAGENTA, RESET}

def interpret(statement: Statement): Option[Int] =
  statement match
  case Statement.Insert(index, Access(tag, path), sources) =>
    tag.insert(index, path, sources())
  case Statement.Prepend(Access(tag, path), sources) =>
    tag.prepend(path, sources())
  case Statement.Append(Access(tag, path), sources) =>
    tag.append(path, sources())
  case Statement.Set(Access(tag, path), sources) =>
    tag.set(path, sources())
  case Statement.Remove(Access(tag, path)) =>
    tag.remove(path)
  case Statement.Get(Access(tag, path)) =>
    tag.get(path)
  case Statement.GetNumeric(Access(tag, path), scale) =>
    tag.getNumeric(path, scale)
  case Statement.Merge(Access(tag, path), source) =>
    ??? // TODO
  case Statement.Print(target) =>
    val stringified = target().map(stringify).iterator
    while
      print(stringified.next)
      stringified.hasNext
    do print(", ")
    println()
    Some(1)

extension (access: Access) def apply(): Seq[Tag] = access.path.get(access.tag)

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
  if """[^ "\[\].\{\}:]+""".r.matches(string) then
    s"$color$string$RESET"
  else
    s""""$color${string.replace("\"", "\\\"")}$RESET""""
