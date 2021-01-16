package nbts

extension (target: CompoundTag)
  def insert(index: Int, path: Path, sources: Seq[Tag]): Option[Int] =
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

  def prepend(path: Path, sources: Seq[Tag]): Option[Int] =
    insert(0, path, sources)

  def append(path: Path, sources: Seq[Tag]): Option[Int] =
    insert(-1, path, sources)

  def set(path: Path, sources: Seq[Tag]): Option[Int] =
    path.set(target, sources.last) match
    case 0 => None
    case result => Some(result)

  def remove(path: Path): Option[Int] =
    path.remove(target) match
    case 0 => None
    case result => Some(result)

  private def getSingle(path: Path): Option[Tag] =
    path.get(target) match
    case Seq(target) => Some(target)
    case _ => None

  def get(path: Path): Option[Int] =
    target.getSingle(path) match
    case Some(target) =>
      target match
      case target: NumericTag => Some(floor(target.asDouble))
      case target: CollectionTag[?] => Some(target.size)
      case target: CompoundTag => Some(target.size)
      case target: StringTag => Some(target.size)
      case _ => None
    case None => None

  def getNumeric(path: Path, scale: Double): Option[Int] =
    target.getSingle(path) match
    case Some(target) =>
      target match
      case target: NumericTag => Some(floor(target.asDouble * scale))
      case _ => None
    case _ => None

  def merge(source: CompoundTag): Option[Int] =
    source.keys foreach { name =>
      source.get(name) map {
        case source: CompoundTag =>
          target.get(name) map {
            case target: CompoundTag => target.merge(source)
            case _ => target(name) = source.copy()
          }
        case source => target(name) = source.copy()
      }
    }
    Some(1) // TODO: unchanged check
