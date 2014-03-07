package scalaxy.loops

private[loops] trait CanBuildFromSinks extends BuilderSinks {
  val global: scala.reflect.api.Universe
  import global._

  case class CanBuildFromSink(canBuildFrom: Tree) extends BuilderSink
  {
    override def createBuilder(inputVars: TuploidValue[Tree]) =
      q"$canBuildFrom()"
  }
}
