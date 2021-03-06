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
    case Matches(target: Expression, min: Int, max: Int)
    case To(target: Expression, typ: Type, scale: Double)
    case Random(probability: Float)

  enum Accessor:
    case Single(tag: Tag)
    case Local(tag: Tag, path: Path)
    case Global(path: Path)

  final case class Path(nodes: Seq[Node])

  enum Node:
    case MatchRootObject(pattern: CompoundTag)
    case MatchObject(name: String, pattern: CompoundTag)
    case AllElements
    case MatchElement(pattern: CompoundTag)
    case IndexedElement(index: Int)
    case CompoundChild(name: String)

  enum Operator:
    case +, -, *, /, %, `=`, <, <=, >, >=

  enum Type:
    case Byte
    case Short
    case Int
    case Long
    case Float
    case Double
