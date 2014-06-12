package scalaxy.streams

object Optimizations
{
  def optimizedStreamMessage(streamDescription: String): String =
      "[Scalaxy] Optimized stream: " + streamDescription

  def matchStrategyTree(u: scala.reflect.api.Universe)
                       (tree: u.Tree): scalaxy.optimization = 
  {
    import u._
    tree match {
      case q"scalaxy.optimization.safe" =>
        scalaxy.optimization.safe

      case q"scalaxy.optimization.aggressive" =>
        scalaxy.optimization.aggressive

      case EmptyTree =>
        scalaxy.optimization.default

      case q"scalaxy.optimization.default" =>
        scalaxy.optimization.default
    }
  }

  def optimize(u: scala.reflect.api.Universe)
              (tree: u.Tree,
                typeCheck: u.Tree => u.Tree,
                fresh: String => String,
                info: (u.Position, String) => Unit,
                recurse: Boolean = true,
                strategy: => scalaxy.optimization): u.Tree =
  {
    object Optimize extends StreamTransforms {
      override val global = u
      import global._

      private[this] val typed = typeCheck.asInstanceOf[Tree => Tree]

      val result = new Transformer {
        override def transform(tree: Tree) = tree match {
          case SomeStream(stream) if stream.isWorthOptimizing(strategy) =>
            info(
              tree.pos.asInstanceOf[u.Position],
              optimizedStreamMessage(stream.describe()))
            val result =
              stream.emitStream(
                n => TermName(fresh(n)),
                if (recurse) transform(_) else tree => tree,
                typed)
              .compose(typed)
            // println(result)

            typed(result)

          case _ =>
            super.transform(tree)
        }
      } transform tree.asInstanceOf[Tree]//typed(tree)

      // println(result)
      // println(showRaw(result, printTypes = true))
    }

    typeCheck(Optimize.result.asInstanceOf[u.Tree])
  }
}
