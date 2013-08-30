package scalaxy.js.ast

object JS {
  sealed trait Node extends Api {
    def needsParen = false
    def pos: SourcePos
  }
  trait Api {
    self: Node =>

    def select(name: String)(implicit pos: SourcePos) = Select(this, Ident(name))
    def select(name: Node)(implicit pos: SourcePos) = Select(this, name)

    def apply(args: List[Node])(implicit pos: SourcePos) = Apply(this, args)
    def apply(name: String, args: List[Node])(implicit pos: SourcePos) = Apply(select(name), args)
    def apply(name: Node, args: List[Node])(implicit pos: SourcePos) = Apply(select(name), args)

    def op(op: String, rhs: Node)(implicit pos: SourcePos) = BinOp(this, op, rhs)
    def preOp(op: String)(implicit pos: SourcePos) = PrefixOp(op, this)
    def postOp(op: String)(implicit pos: SourcePos) = PostfixOp(this, op)

    def assign(rhs: Node)(implicit pos: SourcePos): Assign = Assign(this, rhs)
    def assign(ident: String)(implicit pos: SourcePos): Assign = assign(Ident(ident))
    def asVar(name: String)(implicit pos: SourcePos) = VarDef(name, this)
  }
  def new_(name: String)(implicit pos: SourcePos) = New(Ident(name))

  val binaryOperators = Set(
    "+", "-", "*", "/", "%",
    "<", "<=", ">", ">=", "==", "!=", "===", "!==", "||", "&&", "^^",
    "<<", ">>", ">>>", "^", "&", "|"
  )
  case object NoNode extends Node {
    def pos: SourcePos = null
  }
  case class BinOp(lhs: Node, op: String, rhs: Node)(implicit val pos: SourcePos) extends Node {
    override def needsParen = true
  }
  case class Commented(comment: String, node: Node)(implicit val pos: SourcePos) extends Node 
  case class PrefixOp(op: String, a: Node)(implicit val pos: SourcePos) extends Node
  case class PostfixOp(a: Node, op: String)(implicit val pos: SourcePos) extends Node
  case class Block(body: List[Node])(implicit val pos: SourcePos) extends Node
  case class If(condition: Node, thenNode: Node, elseNode: Node)(implicit val pos: SourcePos) extends Node
  case class Function(name: Option[String], args: List[Node], body: Block)(implicit val pos: SourcePos) extends Node {
    override def needsParen = true
  }
  case class New(target: Node)(implicit val pos: SourcePos) extends Node
  case class Ident(name: String)(implicit val pos: SourcePos) extends Node
  case class Literal(value: Any)(implicit val pos: SourcePos) extends Node
  case class Assign(lhs: Node, rhs: Node)(implicit val pos: SourcePos) extends Node {
    override def needsParen = true
  }
  case class VarDef(name: String, rhs: Node)(implicit val pos: SourcePos) extends Node
  case class Select(target: Node, name: Node)(implicit val pos: SourcePos) extends Node
  case class Apply(target: Node, args: List[Node])(implicit val pos: SourcePos) extends Node
  case class Return(returnNode: Node)(implicit val pos: SourcePos) extends Node
  case class Interpolation(fragments: List[String], values: List[Node])(implicit val pos: SourcePos) extends Node {
    override def needsParen = true
  }
  case class JSONObject(map: Map[String, Node])(implicit val pos: SourcePos) extends Node
  case class JSONArray(values: List[Node])(implicit val pos: SourcePos) extends Node

  def newEmptyJSON(implicit pos: SourcePos) = JSONObject(Map()) // TODO: JSONObject

  def path(path: String)(implicit pos: SourcePos): Node = {
    path.split("\\.").map(JS.Ident(_): JS.Node).reduce(JS.Select(_, _))
  }

  def prettyPrint(node: Node, depth: Int = 0): PosAnnotatedString = {
    val pos = node.pos
    node match {
      case JSONObject(map) =>
        reduceOr(
          map.toList.sortBy(_._1).map({
            case (name, value) =>
              indent(depth + 1) ++ prettyPrint(Literal(name)(pos), depth + 1) ++
              a": " ++
              prettyPrint(value, depth + 1)
          }),
          pos("{\n"), ",\n", a"\n" ++ indent(depth) ++ a"}",
          pos("{}"))

      case JSONArray(values) =>
        reduceOr(
          values.map(prettyPrint(_, depth)),
          pos("["), ", ", pos("]"),
          pos("[]"))

      case Block(body) =>
        reduceOr(
          body.map(s => indent(depth + 1) ++ prettyPrint(s, depth + 1) ++ a";\n"),
          pos("{\n"), "", indent(depth) ++ a"}",
          pos("{}"))

      case If(condition, thenNode, elseNode) =>
        pos("if (") ++ prettyPrint(condition, depth) ++ a") " ++ prettyPrint(thenNode, depth) ++
        (
          if (elseNode == NoNode) a""
          else a"\n" ++ indent(depth) ++ a"else " ++ prettyPrint(elseNode, depth)
        )

      case Interpolation(fragments, values) =>
        (
          fragments.zip(values).map({ case (s, a) =>
            pos(s) ++ prettyPrint(a, depth)
          }) :+ pos(fragments.last)
        ).reduce(_ ++ _)

      case Return(returnNode) =>
        pos("return ") ++ prettyPrint(returnNode, depth)

      case Function(name: Option[String], args, body: Block) =>
        pos("function" ++ name.map(" " ++ _).getOrElse("") ++ "(") ++
          reduceOr(
            args.map(prettyPrint(_, depth)),
            a"", ", ", a"",
            a""
          ) ++
        a") " ++ prettyPrint(body, depth)

      case Ident(name: String) =>
        pos(name.trim)

      case Literal(value: Any) =>
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

      case VarDef(name, rhs) =>
        pos("var " + name + (if (rhs == NoNode) "" else " = ")) ++ prettyPrint(rhs, depth)

      case Assign(lhs, rhs) =>
        prettyPrint(lhs, depth) ++
        pos(" = ") ++
        prettyPrint(rhs, depth)

      case Commented(comment, node) =>
        val sub = prettyPrint(node, depth)
        if (comment.contains("\n"))
          pos(comment.replaceAll("\n", "\n" + indent(depth).value) + "\n") ++
          indent(depth) ++ sub
        else
          pos(comment) ++ sub

      case BinOp(lhs, op, rhs) =>
        prettyPrint(lhs, depth) ++
        pos(" " ++ op ++ " ") ++
        prettyPrint(rhs, depth)

      case PrefixOp(op, operand) =>
        pos(op) ++
        prettyPrint(operand, depth)

      case New(target) =>
        pos("new ") ++ prettyPrint(target, depth)

      case PostfixOp(operand, op) =>
        prettyPrint(operand, depth) ++
        pos(op)

      case Select(target, name) =>
        wrapWithParenIfNeeded(target, pos, depth) ++
        (
          name match {
            case Ident(_) =>
              pos(".") ++ prettyPrint(name, depth)
            case _ =>
              pos("[") ++ prettyPrint(name, depth) ++ a"]"
          }
        )

      case Apply(target, args) =>
        val ts = prettyPrint(target, depth)
        wrapWithParenIfNeeded(target, pos, depth) ++
        reduceOr(
          args.map(prettyPrint(_, depth)),
          pos("("), ", ", pos(")"),
          pos("()")
        )
    }
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



