package scalaxy.loops

import scala.collection.generic.CanBuildFrom

private[loops] trait ListBufferSinks extends BuilderSinks {
  val global: scala.reflect.api.Universe
  import global._

  case object ListBufferSink extends BuilderSink
  {
    override def describe = Some("List")

    override def createBuilder(inputVars: TuploidValue[Tree]) = {
      val builderModule = rootMirror.staticModule("scala.collection.mutable.ListBuffer")
      typed(q"$builderModule[${inputVars.tpe}]()")
    }
  }
}
