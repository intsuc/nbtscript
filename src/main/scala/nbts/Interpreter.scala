package nbts

import nbts.Ast._
import scala.Console.{CYAN, GREEN, YELLOW, MAGENTA, RESET}
import scala.collection.mutable
import scala.util.Random

class Interpreter:
  import Interpreter._
  private val functions: mutable.Map[String, (Seq[String], Seq[Expression])] = mutable.Map.empty
  private val global: CompoundTag = CompoundTag()
  private val random: Random = Random()

  def interpret(expressions: Seq[Expression])(using quotations: Map[String, Expression]): Unit =
    expressions.foreach(interpret)

  private def interpret(expression: Expression)(using quotations: Map[String, Expression]): Seq[Tag] =
    expression match
    case Expression.Access(accessor) =>
      accessor match
      case Accessor.Single(tag) => Seq(tag)
      case Accessor.Local(tag, path) => path.get(tag)
      case Accessor.Global(path) => path.get(global)
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
      val tags = interpret(target).map(stringify)
      tags.dropRight(1).foreach(tag => print(s"$tag, "))
      tags.lastOption.map(print)
      println()
      Seq(IntTag(1))
    case Expression.Function(name, parameters, body) =>
      functions(name) = (parameters, body)
      Seq(IntTag(1))
    case Expression.Run(name, arguments) =>
      functions.get(name) match
      case Some((parameters, body)) => interpret(body)(using parameters.zip(arguments).toMap); Seq(IntTag(1))
      case None => Seq.empty
    case Expression.Quotation(name) =>
      quotations.get(name) match
      case Some(expression) => interpret(expression)
      case None => Seq.empty
    case Expression.If(target, body) =>
      interpret(target) match
      case Seq() => Seq.empty
      case _ => interpret(body); Seq(IntTag(1))
    case Expression.Unless(target, body) =>
      interpret(target) match
      case Seq() => interpret(body); Seq(IntTag(1))
      case _ => Seq.empty
    case Expression.Operate(left, operator, right) =>
      (interpret(left), interpret(right)) match
      case (Seq(left), Seq(right)) =>
        (left, right) match
        case (IntTag(left), IntTag(right)) => Seq(IntTag(
          operator match
          case Operator.+ => left + right
          case Operator.- => left - right
          case Operator.* => left * right
          case Operator./ => Math.floorDiv(left, right)
          case Operator.% => Math.floorMod(left, right)
          case Operator.`=` => if left == right then 1 else 0
          case Operator.< => if left < right then 1 else 0
          case Operator.<= => if left <= right then 1 else 0
          case Operator.> => if left > right then 1 else 0
          case Operator.>= => if left >= right then 1 else 0))
        case _ => Seq.empty
      case _ => Seq.empty
    case Expression.Matches(target, min, max) =>
      interpret(target) match
      case Seq(target) =>
        target match
        case IntTag(data) => Seq(IntTag(if min to max contains data then 1 else 0))
        case _ => Seq.empty
      case _ => Seq.empty
    case Expression.To(target, typ, scale) =>
      interpret(target) match
      case Seq(target) =>
        target match
        case IntTag(data) => Seq(
          typ match
          case Type.Byte => ByteTag((data.toDouble * scale).toInt.toByte)
          case Type.Short => ShortTag((data.toDouble * scale).toInt.toShort)
          case Type.Int => IntTag((data.toDouble * scale).toInt)
          case Type.Long => LongTag((data.toDouble * scale).toLong)
          case Type.Float => FloatTag((data.toDouble * scale).toFloat)
          case Type.Double => DoubleTag(data.toDouble * scale))
        case _ => Seq.empty
      case _ => Seq.empty
    case Expression.Random(probability) =>
      Seq(IntTag(if random.nextFloat() < probability then 1 else 0))

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
  extension (left: Tag) private def <= (right: Tag): Boolean =
    (left, right) match
    case (left: CompoundTag, right: CompoundTag) => left.keys.forall(name => right.get(name).map(left(name) <= _).getOrElse(false))
    case (left: ListTag, right: ListTag) if left.isEmpty => right.isEmpty
    case (left: ListTag, right: ListTag) => left.toSet subsetOf right.toSet
    case (left, right) => left == right

  extension (self: Node)
    private def get(target: Tag): Seq[Tag] =
      (self, target) match
      case (Node.MatchRootObject(pattern), target: CompoundTag) => Seq(target).filter(pattern <= _)
      case (Node.MatchObject(name, pattern), target: CompoundTag) => target.get(name).toSeq.filter(pattern <= _)
      case (Node.AllElements, target: CollectionTag[?]) => target.toSeq
      case (Node.MatchElement(pattern), target: ListTag) => target.toSeq.filter(pattern <= _)
      case (Node.IndexedElement(index), target: CollectionTag[?]) => target.lift(index).toSeq
      case (Node.CompoundChild(name), target: CompoundTag) => target.get(name).toSeq
      case _ => Seq.empty

    private def getOrCreate(target: Tag, source: => Tag): Seq[Tag] =
      (self, target) match
      case (Node.MatchRootObject(_), _) => get(target)
      case (Node.MatchObject(name, pattern), target: CompoundTag) =>
        target.get(name) match
        case Some(tag) => Seq(tag).filter(pattern <= _)
        case None =>
          val tag = pattern.copy()
          target(name) = tag
          Seq(tag)
      case (Node.AllElements, target: CollectionTag[?]) =>
        if target.isEmpty then
          val tag = source
          if target.add(0, tag) then Seq(tag) else Seq.empty
        else target.toSeq
      case (Node.MatchElement(pattern), target: ListTag) =>
        val filtered = target.filter(pattern <= _)
        if filtered.isEmpty then
          val tag = pattern.copy()
          target += tag
          Seq(tag)
        else filtered.toSeq
      case (Node.IndexedElement(_), target) => get(target)
      case (Node.CompoundChild(name), target: CompoundTag) =>
        val tag = target.get(name) match
        case Some(tag) => tag
        case None =>
          val tag = source
          target(name) = tag; tag
        Seq(tag)
      case _ => Seq.empty

    private def preferredParent: Tag =
      self match
      case Node.MatchRootObject(_) => CompoundTag()
      case Node.MatchObject(_, _) => CompoundTag()
      case Node.AllElements => ListTag()
      case Node.MatchElement(_) => ListTag()
      case Node.IndexedElement(_) => ListTag()
      case Node.CompoundChild(_) => CompoundTag()

    private def set(target: Tag, source: => Tag): Int =
      (self, target) match
      case (Node.MatchRootObject(_), _) => 0
      case (Node.MatchObject(name, pattern), target: CompoundTag) =>
        target.get(name).map(tag =>
          if pattern <= tag then
            val src = source
            if target == src then 0
            else target(name) = src; 1
          else 0
        ).getOrElse(0)
      case (Node.AllElements, target: CollectionTag[?]) =>
        if target.isEmpty then
          target.add(0, source); 1
        else
          val size = target.size
          val result = size - target.count(source == _)
          if result != 0 then
            target.clear()
            if !target.add(0, source) then 0
            else
              for index <- 1 until size do target.add(index, source)
              result
          else 0
      case (Node.MatchElement(pattern), target: ListTag) =>
        if target.isEmpty then
          target += source; 1
        else
          val size = target.size
          var result = 0
          for index <- 0 until size do
            val tag = source
            if pattern <= target(index) && tag != target(index) && target.set(index, tag) then result += 1
          result
      case (Node.IndexedElement(index), target: CollectionTag[?]) =>
        target.lift(index) match
        case Some(tag) =>
          val src = source
          if src != tag && target.set(index, src) then 1 else 0
        case None => 0
      case (Node.CompoundChild(name), target: CompoundTag) =>
        val src = source
        target.put(name, src).map(tag => if src == tag then 0 else 1).getOrElse(1)
      case _ => 0

    private def remove(target: Tag): Int =
      (self, target) match
      case (Node.MatchRootObject(_), _) => 0
      case (Node.MatchObject(name, pattern), target: CompoundTag) =>
        target.get(name).map(tag =>
          if pattern <= tag then
            target -= name; 1
          else 0
        ).getOrElse(0)
      case (Node.AllElements, target: CollectionTag[?]) =>
        val size = target.size
        target.clear()
        size
      case (Node.MatchElement(pattern), target: ListTag) =>
        var result = 0
        for index <- (0 until target.size).reverse do
          if pattern <= target(index) then
            target.remove(index)
            result += 1
        result
      case (Node.IndexedElement(index), target: CollectionTag[?]) =>
        target.lift(index) match
        case Some(_) => target.remove(index); 1
        case None => 0
      case (Node.CompoundChild(name), target: CompoundTag) =>
        if target contains name then
          target -= name; 1
        else 0
      case _ => 0

  extension (self: Path)
    private def get(target: Tag): Seq[Tag] =
      self.nodes.foldLeft(mutable.Buffer(target))((context, node) => context flatMap (node.get(_))).toSeq

    private def getOrCreateParents(target: Tag): mutable.Buffer[Tag] =
      val targets = mutable.Buffer(target)
      if self.nodes.size >= 2 then self.nodes.sliding(2).foldLeft(targets) {
        case (targets, Seq(left, right)) => targets.flatMap(left.getOrCreate(_, right.preferredParent))
      } else targets

    private def getOrCreate(target: Tag, source: => Tag): Seq[Tag] =
      getOrCreateParents(target).flatMap(self.nodes.last.getOrCreate(_, source)).toSeq

    private def set(target: Tag, source: => Tag): Int =
      getOrCreateParents(target).map(self.nodes.last.set(_, source)).sum

    private def remove(target: Tag): Int =
      self.nodes.dropRight(1).foldLeft(mutable.Buffer(target))((context, node) => context flatMap (node.get(_))).map(self.nodes.last.remove(_)).sum

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
