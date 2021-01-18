package nbts

object Ast:
  final case class Access(tag: Tag, path: Path)

  enum Statement:
    case Insert(index: Int, targets: Access, sources: Access)
    case Prepend(targets: Access, sources: Access)
    case Append(targets: Access, sources: Access)
    case Set(targets: Access, sources: Access)
    case Remove(targets: Access)
    case Get(targets: Access)
    case GetNumeric(targets: Access, scale: Double)
    case Merge(targets: Access, sources: Access)
    case Print(targets: Access)

  final case class Source(statements: Seq[Statement])
