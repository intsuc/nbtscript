package nbts

import nbts.Ast._

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
    target().map(stringify).foreach(println); Some(1)

extension (access: Access) def apply(): Seq[Tag] = access.path.get(access.tag)

def stringify(tag: Tag): String =
  tag match
  case EndTag => ""
  case StringTag(data) => quote(data)
  case CompoundTag(data) => data.map((name, tag) => s"${quote(name)}: ${stringify(tag)}").mkString("{", ", ", "}")
  case ByteTag(data) => s"${data}b"
  case ShortTag(data) => s"${data}s"
  case IntTag(data) => s"$data"
  case LongTag(data) => s"${data}L"
  case FloatTag(data) => s"${data}f"
  case DoubleTag(data) => s"${data}d"
  case ByteArrayTag(data) => data.map(_.toString + "b").mkString("[B;", ", ", "]")
  case IntArrayTag(data) => data.map(_.toString).mkString("[I;", ", ", "]")
  case LongArrayTag(data) => data.map(_.toString + "L").mkString("[L;", ", ", "]")
  case ListTag(data, _) => data.map(stringify).mkString("[", ", ", "]")

def quote(string: String): String = s""""${string.replaceAllLiterally("\"", "\\\"")}""""
