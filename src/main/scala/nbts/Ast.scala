package nbts

object Ast:
  final case class Source(expressions: Seq[Expression])

  enum Expression:
    case Access(accessor: Accessor)
    case Insert(index: Int, target: Accessor, source: Expression)
    case Set(target: Accessor, source: Expression)
    case Remove(target: Accessor)
    case Get(target: Expression)
    case GetNumeric(target: Expression, scale: Double)
    case Merge(target: Accessor, source: Expression)
    case Print(target: Expression)
    case Function(name: String, body: Seq[Expression])
    case Run(name: String)
    case If(target: Expression, body: Seq[Expression])
    case Unless(target: Expression, body: Seq[Expression])
    case Operate(left: Expression, operator: Operator, right: Expression)
    case Random(probability: Float)

  enum Accessor:
    case Single(tag: Tag)
    case Local(tag: Tag, path: Path)
    case Global(path: Path)

  enum Operator:
    case +, -, *, /, %, `=`, <, <=, >, >=
