package scalaxy.streams

import scala.collection.generic.CanBuildFrom

private[streams] trait ListBufferSinks extends BuilderSinks {
  val global: scala.reflect.api.Universe
  import global._

  case object ListBufferSink extends BuilderSink
  {
    override def describe = Some("List")

    override def createBuilder(inputVars: TuploidValue[Tree], typed: Tree => Tree) = {
      val builderModule =
        rootMirror.staticModule("scala.collection.mutable.ListBuffer")
      typed(q"$builderModule[${inputVars.tpe}]()")
    }
  }
}
