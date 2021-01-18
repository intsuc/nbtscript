package nbts

object Ast:
  enum Tag:
    case String(data: java.lang.String)
    case Compound(data: Map[java.lang.String, Tag])
    case Byte(data: scala.Byte)
    case Short(data: scala.Short)
    case Int(data: scala.Int)
    case Long(data: scala.Long)
    case Float(data: scala.Float)
    case Double(data: scala.Double)
    case ByteArray(data: Seq[scala.Byte])
    case IntArray(data: Seq[scala.Int])
    case LongArray(data: Seq[scala.Long])
    case List(data: Seq[Tag])

  enum Node:
    case MatchRootObject(pattern: Tag.Compound)
    case MatchObject(name: String, pattern: Tag.Compound)
    case AllElements
    case MatchElement(pattern: Tag.Compound)
    case IndexedElement(index: Int)
    case CompoundChild(name: String)

  final case class Path(nodes: Seq[Node])

  final case class Access(tag: Tag, path: Option[Path])

  enum Statement:
    case Insert(index: Int, target: Access, source: Access)
    case Prepend(target: Access, source: Access)
    case Append(target: Access, source: Access)
    case Set(target: Access, source: Access)
    case Remove(target: Access)
    case Get(target: Access)
    case GetNumeric(target: Access, scale: Double)
    case Merge(target: Access, source: Access)
    case Print(target: Access)

  final case class Source(statements: Seq[Statement])
