package scalaxy.streams

import scala.collection.generic.CanBuildFrom

private[streams] trait CanBuildFromSinks extends BuilderSinks {
  val global: scala.reflect.api.Universe
  import global._

  case class CanBuildFromSink(canBuildFrom: Tree) extends BuilderSink
  {
    val TypeRef(_, _, List(_, _, toTpe: Type)) = {
      val sym = rootMirror.staticClass("scala.collection.generic.CanBuildFrom")
      canBuildFrom.tpe.baseType(sym)
    }

    override def describe = Some(toTpe.typeSymbol.name.toString)

    override def createBuilder(inputVars: TuploidValue[Tree], typed: Tree => Tree) = {
      typed(q"$canBuildFrom()")
    }
  }
}
