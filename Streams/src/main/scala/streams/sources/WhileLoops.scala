package scalaxy.streams
import scala.reflect.NameTransformer.{ encode, decode }

trait WhileLoops
{
  val global: scala.reflect.api.Universe
  import global._

  object WhileLoop {
    def unapply(tree: Tree): Option[(Tree, Seq[Tree])] = Option(tree) collect {
      case LabelDef(
        label,
        List(),
        If(
          condition,
          Block(
            statements,
            Apply(
              Ident(label2),
              List()
              )
            ),
          Literal(Constant(()))
          )
        ) if (label == label2) =>
        (condition, statements)
    }

    def apply(condition: Tree, statements: Seq[Tree]): Tree =
      q"while ($condition) { ..$statements ; () }"
  }

}
