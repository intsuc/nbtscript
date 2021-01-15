package nbts

import scala.collection.mutable

extension (left: Tag) def <= (right: Tag): Boolean =
  (left, right) match
  case (CompoundTag(left), CompoundTag(right)) => left.keys.forall(key => right.get(key).map(left(key) <= _).getOrElse(false))
  case (ListTag(left, _), ListTag(right, _)) if left.isEmpty => right.isEmpty
  case (ListTag(left, _), ListTag(right, _)) => left.toSet subsetOf right.toSet
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
      target.get(name) match
      case Some(element) =>
        if pattern <= element then context += element
      case None =>
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
        val element = pattern.copy
        target.put(name, element)
        context += element
    case (Node.AllElements, target: CollectionTag[?]) =>
      if target.isEmpty then
        val element = source
        if target.addTag(0, element) then context += element
      else context ++= target
    case (Node.MatchElement(pattern), target: ListTag) =>
      val filtered = target.filter(pattern <= _)
      if filtered.isEmpty then
        val element = pattern.copy
        target += element
        context += element
      else filtered.foreach(context += _)
    case (Node.IndexedElement(_), target) =>
      node.get(target, context)
    case (Node.CompoundChild(name), target: CompoundTag) =>
      target.get(name) match
      case Some(element) =>
        context += element
      case None =>
        val element = source
        target.put(name, element)
        context += element
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
      target.get(name) match
      case Some(element) =>
        if pattern <= element then
          val element = source
          if target == element then 0
          else target.put(name, element); 1
        else 0
      case None => 0
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
          val element = source
          if pattern <= target(index) && element != target(index) && target.setTag(index, element) then result += 1
        result
    case (Node.IndexedElement(index), target: CollectionTag[?]) =>
      val normalized = if index < 0 then target.size + index else index
      if (0 until target.size) contains normalized then
        val element = source
        if element == target(normalized) then 0
        else if target.setTag(normalized, element) then 1 else 0
      else 0
    case (Node.CompoundChild(name), target: CompoundTag) =>
      val element = source
      if element == target.put(name, element) then 0 else 1
    case _ => 0

  def remove(target: Tag): Int =
    (node, target) match
    case (Node.MatchRootObject(_), _) => 0
    case (Node.MatchObject(name, pattern), target: CompoundTag) =>
      target.get(name) match
      case Some(element) =>
        if pattern <= element then
          target -= name; 1
        else 0
      case None => 0
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
      if (0 until target.size) contains normalized then
        target.remove(normalized); 1
      else 0
    case (Node.CompoundChild(name), target: CompoundTag) =>
      if target contains name then
        target -= name; 1
      else 0
    case _ => 0
