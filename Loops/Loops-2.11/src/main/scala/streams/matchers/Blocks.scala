package scalaxy.loops

private[loops] trait Blocks
{
  val global: scala.reflect.api.Universe
  import global._

  object StripBlocks {
    def unapply(tree: Tree): Option[Tree] = Some(tree match {
      case Block(Nil, StripBlocks(value)) =>
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
