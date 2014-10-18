package scalaxy.streams

object Optimizations
{
  def messageHeader = "[Scalaxy] "

  def optimizedStreamMessage(streamDescription: String): String =
      messageHeader + "Optimized stream: " + streamDescription

  def matchStrategyTree(u: scala.reflect.api.Universe)
                       (staticClass: String => u.TypeSymbol,
                        inferImplicitValue: u.Type => u.Tree): OptimizationStrategy = 
  {
    import u._

    val optimizationStrategyValue: Tree = try {
      val tpe = staticClass("scalaxy.streams.OptimizationStrategy").asType.toType
      inferImplicitValue(tpe)
    } catch { case _: Throwable =>
      EmptyTree
    }

    optimizationStrategyValue match {
      case EmptyTree =>
        scalaxy.streams.optimization.global

      case strategyTree =>
        scalaxy.streams.optimization.forName(strategyTree.symbol.name.toString)
    }
  }
}
