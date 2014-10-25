package scalaxy.streams

private[streams] object Optimizations
{
  def messageHeader = "[Scalaxy] "

  def optimizedStreamMessage(streamDescription: String, strategy: OptimizationStrategy): String =
      messageHeader +
      "Optimized stream " + streamDescription +
      " (strategy: " + strategy.name + ")"


  def logException(pos: Position, ex: Throwable,
                   warning: (Position, String) => Unit) = {
    warning(pos, messageHeader + "An exception ocurred: " + ex)
    if (impl.veryVerbose) {
      ex.printStackTrace()
    }
  }

  def matchStrategyTree(u: scala.reflect.api.Universe)
                       (staticClass: String => u.TypeSymbol,
                        inferImplicitValue: u.Type => u.Tree): OptimizationStrategy = 
  {
    import u._

    val optimizationStrategyValue: Tree = try {
      val tpe = staticClass("scalaxy.streams.OptimizationStrategy").asType.toType
      inferImplicitValue(tpe)
    } catch { case ex: Throwable =>
      EmptyTree
    }

    optimizationStrategyValue match {
      case EmptyTree =>
        scalaxy.streams.strategy.global

      case strategyTree =>
        scalaxy.streams.strategy.forName(strategyTree.symbol.name.toString).get
    }
  }
}
