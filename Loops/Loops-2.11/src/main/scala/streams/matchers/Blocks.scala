package scalaxy.loops

private[loops] trait Blocks
{
  val global: scala.reflect.api.Universe
  import global._

  object StripBlocks {
    def unapply(tree: Tree): Option[Tree] = Some(tree match {
      case Block(Nil, value) =>
        value

      case _ =>
        tree
    })
  }
}
