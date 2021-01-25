package nbts

import nbts.Ast._
import scala.Console.{CYAN, GREEN, YELLOW, MAGENTA, RESET}
import scala.collection.mutable

class Interpreter:
  private val functions: mutable.Map[String, Seq[Statement]] = mutable.Map.empty
  private val global: CompoundTag = CompoundTag()

  def interpret(statement: Statement): Option[Int] =
    statement match
    case Statement.Insert(index, Targets(tag, path), sources) =>
      Interpreter.insert(tag, index, path, sources())
    case Statement.Prepend(Targets(tag, path), sources) =>
      Interpreter.insert(tag, 0, path, sources())
    case Statement.Append(Targets(tag, path), sources) =>
      Interpreter.insert(tag, -1, path, sources())
    case Statement.Set(Targets(tag, path), sources) =>
      path.set(tag, sources().last) match
      case 0 => None
      case result => Some(result)
    case Statement.Remove(Targets(tag, path)) =>
      path.remove(tag) match
      case 0 => None
      case result => Some(result)
    case Statement.Get(Targets(tag, path)) =>
      path.get(tag) match
      case Seq(target) =>
        target match
        case target: NumericTag => Some(DoubleTag.floor(target.asDouble))
        case target: CollectionTag[?] => Some(target.size)
        case target: CompoundTag => Some(target.size)
        case target: StringTag => Some(target.size)
        case _ => None
      case _ => None
    case Statement.GetNumeric(Targets(tag, path), scale) =>
      path.get(tag) match
      case Seq(target) =>
        target match
        case target: NumericTag => Some(DoubleTag.floor(target.asDouble * scale))
        case _ => None
      case _ => None
    case Statement.Merge(Targets(tag, path), source) =>
      ??? // TODO
    case Statement.Print(targets) =>
      val tags = targets().map(Interpreter.stringify)
      tags.dropRight(1).foreach(tag => print(s"$tag, "))
      tags.lastOption.map(print)
      println()
      Some(1)
    case Statement.Function(name, body) =>
      functions(name) = body
      Some(1)
    case Statement.Call(name) =>
      functions.get(name) match
      case Some(body) => body.foreach(interpret); Some(1)
      case None => None

  object Targets:
    def unapply(accessor: Accessor): (Tag, Path) =
      accessor match
      case Accessor.Single(tag) => (tag, Path(Seq(Node.MatchRootObject(CompoundTag()))))
      case Accessor.Local(tag, path) => (tag, path)
      case Accessor.Global(path) => (global, path)

  extension (accessor: Accessor) def apply(): Seq[Tag] =
    accessor match
    case Accessor.Single(tag) => Seq(tag)
    case Accessor.Local(tag, path) => path.get(tag)
    case Accessor.Global(path) => path.get(global)

object Interpreter:
  def insert(target: Tag, index: Int, path: Path, sources: Seq[Tag]): Option[Int] =
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

  def stringify(tag: Tag): String =
    tag match
    case EndTag => ""
    case StringTag(data) => quote(data, GREEN)
    case CompoundTag(data) => data.map((name, tag) => s"""${quote(name, CYAN)}: ${stringify(tag)}""").mkString("{", ", ", "}")
    case ByteTag(data, false) => s"$YELLOW$data${MAGENTA}b$RESET"
    case ByteTag(data, true) => s"$YELLOW${if data == 1 then "true" else "false"}$RESET"
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
