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

extension (node: Node)
  def get(target: Tag, context: mutable.Buffer[Tag]): Unit =
    (node, target) match
    case (Node.MatchRootObject(pattern), target: CompoundTag) =>
      if pattern <= target then context += target
    case (Node.MatchObject(name, pattern), target: CompoundTag) =>
      target.get(name).map(tag => if pattern <= tag then context += tag)
    case (Node.AllElements, target: CollectionTag[?]) =>
      context ++= target
    case (Node.MatchElement(pattern), target: ListTag) =>
      target.filter(pattern <= _).foreach(context += _)
    case (Node.IndexedElement(index), target: CollectionTag[?]) =>
      val normalized = if index < 0 then target.size + index else index
      if 0 until target.size contains normalized then context += target(normalized)
    case (Node.CompoundChild(name), target: CompoundTag) =>
      target.get(name).map(context += _)
    case _ =>

  def getOrCreate(target: Tag, source: => Tag, context: mutable.Buffer[Tag]): Unit =
    (node, target) match
    case (Node.MatchRootObject(_), _) =>
      node.get(target, context)
    case (Node.MatchObject(name, pattern), target: CompoundTag) =>
      target.get(name) match
      case Some(element) =>
        if pattern <= element then context += element
      case None =>
        val tag = pattern.copy
        target(name) = tag
        context += tag
    case (Node.AllElements, target: CollectionTag[?]) =>
      if target.isEmpty then
        val tag = source
        if target.addTag(0, tag) then context += tag
      else context ++= target
    case (Node.MatchElement(pattern), target: ListTag) =>
      val filtered = target.filter(pattern <= _)
      if filtered.isEmpty then
        val tag = pattern.copy
        target += tag
        context += tag
      else filtered.foreach(context += _)
    case (Node.IndexedElement(_), target) =>
      node.get(target, context)
    case (Node.CompoundChild(name), target: CompoundTag) =>
      val tag = target.get(name) match
      case Some(tag) => tag
      case None =>
        val tag = source
        target(name) = tag; tag
      context += tag
    case _ =>

  def preferredParent: Tag =
    node match
    case Node.MatchRootObject(_) => CompoundTag()
    case Node.MatchObject(_, _) => CompoundTag()
    case Node.AllElements => ListTag()
    case Node.MatchElement(_) => ListTag()
    case Node.IndexedElement(_) => ListTag()
    case Node.CompoundChild(_) => CompoundTag()

  def set(target: Tag, source: => Tag): Int =
    (node, target) match
    case (Node.MatchRootObject(_), _) => 0
    case (Node.MatchObject(name, pattern), target: CompoundTag) =>
      target.get(name).map(tag =>
        if pattern <= tag then
          val tag = source
          if target == tag then 0
          else target(name) = tag; 1
        else 0
      ).getOrElse(0)
    case (Node.AllElements, target: CollectionTag[?]) =>
      if target.isEmpty then
        target.addTag(0, source); 1
      else
        val size = target.size
        val result = size - target.count(source == _)
        if result != 0 then
          target.clear()
          if !target.addTag(0, source) then 0
          else
            for index <- 1 until size do target.addTag(index, source)
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
          if pattern <= target(index) && tag != target(index) && target.setTag(index, tag) then result += 1
        result
    case (Node.IndexedElement(index), target: CollectionTag[?]) =>
      val normalized = if index < 0 then target.size + index else index
      if 0 until target.size contains normalized then
        val tag = source
        if tag != target(normalized) && target.setTag(normalized, tag) then 1 else 0
      else 0
    case (Node.CompoundChild(name), target: CompoundTag) =>
      val src = source
      target.put(name, src).map(tag => if src == tag then 0 else 1).getOrElse(1)
    case _ => 0

  def remove(target: Tag): Int =
    (node, target) match
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
      val normalized = if index < 0 then target.size + index else index
      if 0 until target.size contains normalized then
        target.remove(normalized); 1
      else 0
    case (Node.CompoundChild(name), target: CompoundTag) =>
      if target contains name then
        target -= name; 1
      else 0
    case _ => 0

type Path = Seq[Node]

extension (path: Path)
  def get(target: Tag): Seq[Tag] =
    var targets = mutable.Buffer(target)
    path foreach { node =>
      val context = mutable.Buffer.empty[Tag]
      targets.foreach(node.get(_, context))
      targets = context
      if targets.isEmpty then throw Exception()
    }
    targets.toSeq

  def count(target: Tag): Int =
    var targets = mutable.Buffer(target)
    path forall { node =>
      val context = mutable.Buffer.empty[Tag]
      targets.foreach(node.get(_, context))
      targets = context
      if targets.isEmpty then false else true
    }
    targets.size

  private def getOrCreateParents(target: Tag): mutable.Buffer[Tag] =
    var targets = mutable.Buffer(target)
    path.dropRight(1).zipWithIndex foreach { (node, index) =>
      val context = mutable.Buffer.empty[Tag]
      targets.foreach(node.getOrCreate(_, path(index + 1).preferredParent, context))
      targets = context
      if targets.isEmpty then throw Exception()
    }
    targets

  def getOrCreate(target: Tag, source: => Tag): Seq[Tag] =
    val parents = path.getOrCreateParents(target)
    val context = mutable.Buffer.empty[Tag]
    parents.foreach(path.last.getOrCreate(_, source, context))
    context.toSeq

  def set(target: Tag, source: => Tag): Int =
    val parents = path.getOrCreateParents(target)
    parents.map(path.last.set(_, source)).sum

  def remove(target: Tag): Int =
    var targets = mutable.Buffer(target)
    path.dropRight(1) foreach { node =>
      val context = mutable.Buffer.empty[Tag]
      targets.foreach(node.get(_, context))
      targets = context
    }
    targets.map(path.last.remove(_)).sum
