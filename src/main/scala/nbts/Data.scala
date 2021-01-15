package nbts

extension (target: CompoundTag)
  def insert(index: Int, path: Path, source: Seq[Tag]): Option[Int] = ??? // TODO

  def set(path: Path, source: Seq[Tag]): Option[Int] =
    path.set(target, source.last) match
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
