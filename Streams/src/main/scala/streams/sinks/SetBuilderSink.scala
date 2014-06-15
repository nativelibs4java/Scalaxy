package scalaxy.streams

import scala.collection.generic.CanBuildFrom

private[streams] trait SetBuilderSinks extends BuilderSinks {
  val global: scala.reflect.api.Universe
  import global._

  case object SetBuilderSink extends BuilderSink
  {
    override def describe = Some("Set")

    override def usesSizeHint = false

    override def createBuilder(inputVars: TuploidValue[Tree], typed: Tree => Tree) = {
      val setModule =
        rootMirror.staticModule("scala.collection.immutable.Set")
      typed(q"$setModule.canBuildFrom[${inputVars.tpe}]()")
    }
  }
}
