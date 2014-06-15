package scalaxy.streams

import scala.collection.generic.CanBuildFrom

private[streams] trait VectorBuilderSinks extends BuilderSinks {
  val global: scala.reflect.api.Universe
  import global._

  case object VectorBuilderSink extends BuilderSink
  {
    override def describe = Some("Vector")

    override def lambdaCount = 0

    override def usesSizeHint = false

    // TODO build Vector of same size as source collection if it is known.
    override def createBuilder(inputVars: TuploidValue[Tree], typed: Tree => Tree) = {
      val module = rootMirror.staticModule("scala.collection.immutable.Vector")
      typed(q"$module.newBuilder[${inputVars.tpe.dealias}]")
    }
  }
}
