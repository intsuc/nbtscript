package nbts

import scala.collection.mutable

extension (left: Tag) def <= (right: Tag): Boolean =
  (left, right) match
  case (left: CompoundTag, right: CompoundTag) => left.keys.forall(name => right.get(name).map(left(name) <= _).getOrElse(false))
  case (left: ListTag, right: ListTag) if left.isEmpty => right.isEmpty
  case (left: ListTag, right: ListTag) => left.toSet subsetOf right.toSet
  case (left, right) => left == right

enum Node:
  case MatchRootObject(pattern: CompoundTag)
  case MatchObject(name: String, pattern: CompoundTag)
  case AllElements
  case MatchElement(pattern: CompoundTag)
  case IndexedElement(index: Int)
  case CompoundChild(name: String)

  def get(target: Tag): Seq[Tag] =
    (this, target) match
    case (Node.MatchRootObject(pattern), target: CompoundTag) => Seq(target).filter(pattern <= _)
    case (Node.MatchObject(name, pattern), target: CompoundTag) => target.get(name).toSeq.filter(pattern <= _)
    case (Node.AllElements, target: CollectionTag[?]) => target.toSeq
    case (Node.MatchElement(pattern), target: ListTag) => target.toSeq.filter(pattern <= _)
    case (Node.IndexedElement(index), target: CollectionTag[?]) => target.lift(index).toSeq
    case (Node.CompoundChild(name), target: CompoundTag) => target.get(name).toSeq
    case _ => Seq.empty

  def getOrCreate(target: Tag, source: => Tag): Seq[Tag] =
    (this, target) match
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

  def preferredParent: Tag =
    this match
    case Node.MatchRootObject(_) => CompoundTag()
    case Node.MatchObject(_, _) => CompoundTag()
    case Node.AllElements => ListTag()
    case Node.MatchElement(_) => ListTag()
    case Node.IndexedElement(_) => ListTag()
    case Node.CompoundChild(_) => CompoundTag()

  def set(target: Tag, source: => Tag): Int =
    (this, target) match
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

  def remove(target: Tag): Int =
    (this, target) match
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

final case class Path(nodes: Seq[Node]):
  def get(target: Tag): Seq[Tag] =
    nodes.foldLeft(mutable.Buffer(target))(_ flatMap _.get).toSeq

  private def getOrCreateParents(target: Tag): mutable.Buffer[Tag] =
    val targets = mutable.Buffer(target)
    if nodes.size >= 2 then nodes.sliding(2).foldLeft(targets) {
      case (targets, Seq(left, right)) => targets.flatMap(left.getOrCreate(_, right.preferredParent))
    } else targets

  def getOrCreate(target: Tag, source: => Tag): Seq[Tag] =
    getOrCreateParents(target).flatMap(nodes.last.getOrCreate(_, source)).toSeq

  def set(target: Tag, source: => Tag): Int =
    getOrCreateParents(target).map(nodes.last.set(_, source)).sum

  def remove(target: Tag): Int =
    nodes.dropRight(1).foldLeft(mutable.Buffer(target))(_ flatMap _.get).map(nodes.last.remove).sum
