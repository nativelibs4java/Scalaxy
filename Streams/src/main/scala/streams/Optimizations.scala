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
    scalaxy.streams.optimization.global
  }
}
