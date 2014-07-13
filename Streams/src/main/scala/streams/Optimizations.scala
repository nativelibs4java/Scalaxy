package scalaxy.streams

object Optimizations
{
  def optimizedStreamMessage(streamDescription: String): String =
      "[Scalaxy] Optimized stream: " + streamDescription

  def matchStrategyTree(u: scala.reflect.api.Universe)
                       (tree: u.Tree): OptimizationStrategy = 
  {
    import u._

    // if (tree == EmptyTree)
    //   scalaxy.streams.optimization.default
    // else if (tree.symbol != NoSymbol && tree.symbol.owner == rootMirror.staticModule("scalaxy.streams.optimization")) {
    //   tree.symbol.name.toString match {
    //     case "safe" =>
    //       scalaxy.streams.optimization.safe
    //     case "aggressive" =>
    //       scalaxy.streams.optimization.aggressive
    //     case "default" =>
    //       scalaxy.streams.optimization.default
    //     case _ =>
    //       println("Unknown optimization strategy: " + tree.symbol)
    //       scalaxy.streams.optimization.default
    //   }
    // } else {
    //   println("Unknown optimization strategy: " + tree.symbol)
    //   scalaxy.streams.optimization.default
    // }
    scalaxy.streams.optimization.default
  }

  def optimize(u: scala.reflect.api.Universe)
              (tree: u.Tree,
                typeCheck: u.Tree => u.Tree,
                untypeCheck: u.Tree => u.Tree,
                fresh: String => String,
                info: (u.Position, String) => Unit,
                error: (u.Position, String) => Unit,
                recurse: Boolean = true,
                strategy: => OptimizationStrategy = scalaxy.streams.optimization.default): u.Tree =
  {
    object Optimize extends StreamTransforms {
      override val global = u
      import global._

      private[this] val typed = typeCheck.asInstanceOf[Tree => Tree]
      private[this] val untyped = untypeCheck.asInstanceOf[Tree => Tree]

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
                typed,
                untyped)
              .compose(typed)
            // println(result)

            typed(result)

          case _ if !recurse =>
            error(
              tree.pos.asInstanceOf[u.Position],
              "Failed to detect a top-level optimizable expression in " + tree)
            tree

          case _ =>
            super.transform(tree)
        }
      } transform tree.asInstanceOf[Tree]//typed(tree)

      // if (verbose) {
      //   println(result)
      //   // println(showRaw(result, printTypes = true))
      // }
    }

    typeCheck(Optimize.result.asInstanceOf[u.Tree])
  }
}
