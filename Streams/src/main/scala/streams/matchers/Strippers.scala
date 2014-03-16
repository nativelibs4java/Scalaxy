package scalaxy.streams

private[streams] trait Strippers
{
  val global: scala.reflect.api.Universe
  import global._

  /** Strip quasi-no-op trees (blocks without statements, type ascriptions...). */
  object Strip {
    def unapply(tree: Tree): Option[Tree] = Some(tree match {
      case Block(Nil, Strip(value)) =>
        value

      case Typed(Strip(value), _) =>
        value

      case _ =>
        tree
    })
  }

  object BlockOrNot {
    def unapply(tree: Tree): Option[(List[Tree], Tree)] = Some(tree match {
      case Block(statements, value) =>
        (statements, value)

      case _ =>
        (Nil, tree)
    })
  }
}
