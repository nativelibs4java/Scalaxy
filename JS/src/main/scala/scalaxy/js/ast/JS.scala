package scalaxy.js.ast

object JS {
  sealed trait Node extends Api {
    def needsParen = false
  }
  trait Api {
    self: Node =>

    def select(name: String, pos: SourcePos) = Select(this, Ident(name, pos), pos)
    def select(name: Node, pos: SourcePos) = Select(this, name, pos)

    def apply(args: List[Node], pos: SourcePos) = Apply(this, args, pos)
    def apply(name: String, args: List[Node], pos: SourcePos) = Apply(select(name, pos), args, pos)
    def apply(name: Node, args: List[Node], pos: SourcePos) = Apply(select(name, pos), args, pos)

    def op(op: String, rhs: Node, pos: SourcePos) = BinOp(this, op, rhs, pos)
    def preOp(op: String, pos: SourcePos) = PrefixOp(op, this, pos)
    def postOp(op: String, pos: SourcePos) = PostfixOp(this, op, pos)

    def assign(rhs: Node, pos: SourcePos): Assign = Assign(this, rhs, pos)
    def assign(ident: String, pos: SourcePos): Assign = assign(Ident(ident, pos), pos)
    def asVar(name: String, pos: SourcePos) = VarDef(name, this, pos)
  }
  def new_(name: String, pos: SourcePos) = New(Ident(name, pos), pos)

  case object NoNode extends Node
  case class BinOp(lhs: Node, op: String, rhs: Node, pos: SourcePos) extends Node {
    override def needsParen = true
  }
  case class PrefixOp(op: String, a: Node, pos: SourcePos) extends Node
  case class PostfixOp(a: Node, op: String, pos: SourcePos) extends Node
  case class Block(body: List[Node], pos: SourcePos) extends Node
  case class If(condition: Node, thenNode: Node, elseNode: Node, pos: SourcePos) extends Node
  case class Function(name: Option[String], args: List[Node], body: Block, pos: SourcePos) extends Node {
    override def needsParen = true
  }
  case class New(target: Node, pos: SourcePos) extends Node
  case class Ident(name: String, pos: SourcePos) extends Node
  case class Literal(value: Any, pos: SourcePos) extends Node
  case class Assign(lhs: Node, rhs: Node, pos: SourcePos) extends Node {
    override def needsParen = true
  }
  case class VarDef(name: String, rhs: Node, pos: SourcePos) extends Node
  case class Select(target: Node, name: Node, pos: SourcePos) extends Node
  case class Apply(target: Node, args: List[Node], pos: SourcePos) extends Node
  case class Return(returnNode: Node, pos: SourcePos) extends Node
  case class Interpolation(fragments: List[String], values: List[Node], pos: SourcePos) extends Node {
    override def needsParen = true
  }
  case class JSONObject(map: Map[String, Node], pos: SourcePos) extends Node
  case class JSONArray(values: List[Node], pos: SourcePos) extends Node

  def newEmptyJSON(pos: SourcePos) = JSONObject(Map(), pos) // TODO: JSONObject

  def path(path: String, pos: SourcePos): Node = {
    path.split("\\.").map(JS.Ident(_, pos): JS.Node).reduce(JS.Select(_, _, pos))
  }

  def prettyPrint(node: Node, depth: Int = 0): PosAnnotatedString = node match {
    case JSONObject(map, pos) =>
      reduceOr(
        map.toList.sortBy(_._1).map({
          case (name, value) =>
            indent(depth + 1) ++ prettyPrint(Literal(name, pos), depth + 1) ++
            a": " ++
            prettyPrint(value, depth + 1)
        }),
        pos("{\n"), ",\n", a"\n" ++ indent(depth) ++ a"}",
        pos("{}"))

    case JSONArray(values, pos) =>
      reduceOr(
        values.map(prettyPrint(_, depth)),
        pos("["), ", ", pos("]"),
        pos("[]"))

    case Block(body, pos) =>
      reduceOr(
        body.map(s => indent(depth + 1) ++ prettyPrint(s, depth + 1) ++ a";\n"),
        pos("{\n"), "", indent(depth) ++ a"}",
        pos("{}"))

    case If(condition, thenNode, elseNode, pos) =>
      pos("if (") ++ prettyPrint(condition, depth) ++ a") " ++ prettyPrint(thenNode, depth) ++
      (
        if (elseNode == NoNode) a""
        else a"\n" ++ indent(depth) ++ a"else " ++ prettyPrint(elseNode, depth)
      )

    case Interpolation(fragments, values, pos) =>
      (
        fragments.zip(values).map({ case (s, a) =>
          pos(s) ++ prettyPrint(a, depth)
        }) :+ pos(fragments.last)
      ).reduce(_ ++ _)

    case Return(returnNode, pos) =>
      pos("return ") ++ prettyPrint(returnNode, depth)

    case Function(name: Option[String], args, body: Block, pos) =>
      pos("function" ++ name.map(" " ++ _).getOrElse("") ++ "(") ++
        reduceOr(
          args.map(prettyPrint(_, depth)),
          a"", ", ", a"",
          a""
        ) ++
      a") " ++ prettyPrint(body, depth)

    case Ident(name: String, pos) =>
      pos(name.trim)

    case Literal(value: Any, pos) =>
      pos(value match {
        case s: String =>
          "'" +
          s.replaceAll("\\\\", "\\\\")
            .replaceAll("'", "\\\\'")
            .replaceAll("\n", "\\\\n")
            .replaceAll("\r", "\\\\r")
            .replaceAll("\b", "\\\\b")
            .replaceAll("\f", "\\\\f")
            .replaceAll("\t", "\\\\t")
            .replaceAll("\u000b", "\\\\v")
            .replaceAll("\0", "\\\\0") +
          "'" // TODO: proper JS escapes
        case c @ ((_: java.lang.Character) | (_: Char)) =>
          "'" + c + "'"
        case _ =>
          value.toString // TODO?
      })

    case NoNode =>
      a""

    case VarDef(name, rhs, pos) =>
      pos("var " + name + (if (rhs == NoNode) "" else " = ")) ++ prettyPrint(rhs, depth)

    case Assign(lhs, rhs, pos) =>
      prettyPrint(lhs, depth) ++
      pos(" = ") ++
      prettyPrint(rhs, depth)

    case BinOp(lhs, op, rhs, pos) =>
      prettyPrint(lhs, depth) ++
      pos(" " ++ op ++ " ") ++
      prettyPrint(rhs, depth)

    case PrefixOp(op, operand, pos) =>
      pos(op) ++
      prettyPrint(operand, depth)

    case New(target, pos) =>
      pos("new ") ++ prettyPrint(target, depth)

    case PostfixOp(operand, op, pos) =>
      prettyPrint(operand, depth) ++
      pos(op)

    case Select(target, name, pos) =>
      wrapWithParenIfNeeded(target, pos, depth) ++
      (
        name match {
          case Ident(_, _) =>
            pos(".") ++ prettyPrint(name, depth)
          case _ =>
            pos("[") ++ prettyPrint(name, depth) ++ a"]"
        }
      )

    case Apply(target, args, pos) =>
      val ts = prettyPrint(target, depth)
      wrapWithParenIfNeeded(target, pos, depth) ++
      reduceOr(
        args.map(prettyPrint(_, depth)),
        pos("("), ", ", pos(")"),
        pos("()")
      )
  }

  private def wrapWithParenIfNeeded(node: Node, pos: SourcePos, depth: Int) = {
    val ts = prettyPrint(node, depth)
    if (node.needsParen)
      pos("(") ++ ts ++ pos(")")
    else
      ts
  }

  private def reduceOr(
      col: List[PosAnnotatedString],
      prefix: PosAnnotatedString, sep: String, suffix: PosAnnotatedString,
      or: PosAnnotatedString) =
    if (col.isEmpty) or
    else prefix ++ col.reduce(_ ++ a"$sep" ++ _) ++ suffix

  private lazy val indent = new collection.mutable.HashMap[Int, PosAnnotatedString]() {
    override def default(depth: Int) = {
      val s = PosAnnotatedString((1 to depth).map(i => "  ").mkString(""))
      this(depth) = s
      s
    }
  }
}



