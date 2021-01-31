package nbts

import nbts.Ast._
import scala.Console.{CYAN, GREEN, YELLOW, MAGENTA, RESET}
import scala.collection.mutable

class Interpreter:
  private val functions: mutable.Map[String, Seq[Expression]] = mutable.Map.empty
  private val global: CompoundTag = CompoundTag()

  def interpret(expressions: Seq[Expression]): Unit =
    expressions.foreach(interpret)

  private def interpret(expression: Expression): Seq[Tag] =
    expression match
    case Expression.Access(Accessor.Single(tag)) =>
      Seq(tag)
    case Expression.Access(Accessor.Local(tag, path)) =>
      path.get(tag)
    case Expression.Access(Accessor.Global(path)) =>
      path.get(global)
    case Expression.Insert(index, Targets(tag, path), source) =>
      var result = 0
      val targets = path.getOrCreate(tag, ListTag())
      val sources = interpret(source)
      for target <- targets do
        target match
        case target: CollectionTag[?] =>
          var inserted = false
          var normalized = if index < 0 then target.size + index + 1 else index
          for source <- sources do
            try if target.add(normalized, source.copy()) then
              normalized += 1
              inserted = true
            catch case _: IndexOutOfBoundsException => return Seq.empty
          result += (if inserted then 1 else 0)
        case _ => return Seq.empty
      Seq(IntTag(result))
    case Expression.Set(Targets(tag, path), source) =>
      interpret(source) match
      case Seq() => Seq.empty
      case source =>
        path.set(tag, source.last) match
        case 0 => Seq.empty
        case result => Seq(IntTag(result))
    case Expression.Remove(Targets(tag, path)) =>
      path.remove(tag) match
      case 0 => Seq.empty
      case result => Seq(IntTag(result))
    case Expression.Get(target) =>
      interpret(target) match
      case Seq(target) =>
        target match
        case target: NumericTag => Seq(IntTag(DoubleTag.floor(target.asDouble)))
        case target: CollectionTag[?] => Seq(IntTag(target.size))
        case target: CompoundTag => Seq(IntTag(target.size))
        case target: StringTag => Seq(IntTag(target.size))
        case _ => Seq.empty
      case _ => Seq.empty
    case Expression.GetNumeric(target, scale) =>
      interpret(target) match
      case Seq(target) =>
        target match
        case target: NumericTag => Seq(IntTag(DoubleTag.floor(target.asDouble * scale)))
        case _ => Seq.empty
      case _ => Seq.empty
    case Expression.Merge(target, source) =>
      ??? // TODO
    case Expression.Print(target) =>
      val tags = interpret(target).map(Interpreter.stringify)
      tags.dropRight(1).foreach(tag => print(s"$tag, "))
      tags.lastOption.map(print)
      println()
      Seq(IntTag(1))
    case Expression.Function(name, body) =>
      functions(name) = body
      Seq(IntTag(1))
    case Expression.Run(name) =>
      functions.get(name) match
      case Some(body) => interpret(body); Seq(IntTag(1))
      case None => Seq.empty
    case Expression.If(target, body) =>
      interpret(target) match
      case Seq() => Seq.empty
      case _ => interpret(body); Seq(IntTag(1))
    case Expression.Operate(left, operator, right) =>
      (interpret(left), interpret(right)) match
      case (Seq(left), Seq(right)) =>
        (left, right) match
        case (left: IntTag, right: IntTag) => Seq(IntTag(
          ((left: Int, right: Int) =>
            operator match
            case Operator.+ => left + right
            case Operator.- => left - right
            case Operator.* => left * right
            case Operator./ => Math.floorDiv(left, right)
            case Operator.% => Math.floorMod(left, right)
            case Operator.< => if left < right then 1 else 0
            case Operator.<= => if left <= right then 1 else 0
            case Operator.> => if left > right then 1 else 0
            case Operator.>= => if left >= right then 1 else 0
          )(left.asInt, right.asInt)))
        case _ => Seq.empty
      case _ => Seq.empty

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
  private def stringify(tag: Tag): String =
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

  private def quote(string: String, color: String): String =
    if """[^ "\[\].\{\}:;,]+""".r.matches(string) then
      s"$color$string$RESET"
    else
      s""""$color${string.replace("\"", "\\\"")}$RESET""""
