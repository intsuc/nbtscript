package nbts

object Ast:
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
