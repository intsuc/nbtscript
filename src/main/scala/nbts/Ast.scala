package nbts

object Ast:
  final case class Source(statements: Seq[Statement])

  enum Statement:
    case Insert(index: Int, targets: Accessor, sources: Accessor)
    case Prepend(targets: Accessor, sources: Accessor)
    case Append(targets: Accessor, sources: Accessor)
    case Set(targets: Accessor, sources: Accessor)
    case Remove(targets: Accessor)
    case Get(targets: Accessor)
    case GetNumeric(targets: Accessor, scale: Double)
    case Merge(targets: Accessor, sources: Accessor)
    case Print(targets: Accessor)
    case Function(name: String, body: Seq[Statement])
    case Call(name: String)
    case If(targets: Accessor, body: Seq[Statement])
    case Store(targets: Accessor, body: Statement)

  enum Accessor:
    case Single(tag: Tag)
    case Local(tag: Tag, path: Path)
    case Global(path: Path)
